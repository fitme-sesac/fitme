# app/chatbot/repository_core/aliases.py
from __future__ import annotations

from typing import Dict, List, Optional, Tuple

from .normalize import _normalize_token
from .schema import _column_exists, _has_pg_trgm, _relation_exists

# Alias table config (STACK / INDUSTRY)
# ✅ 최신: stack_alias(entity_type, alias) 통합 테이블
# - entity_type: 'STACK' | 'INDUSTRY'
# - PK: (entity_type, alias)
#
# ✅ 하위호환:
# - stack_alias.kind 컬럼이 있으면 kind 사용
# - 산업을 industry_alias 테이블로 분리한 구버전이면 INDUSTRY는 industry_alias fallback
# -----------------------------

def _stack_alias_tables_ready() -> bool:
    return _relation_exists("stack_alias", relkind="r") or _relation_exists("stack_alias", relkind=None)


def _stack_vocab_ready() -> bool:
    return _relation_exists("stack_vocab", relkind="m") or _relation_exists("stack_vocab", relkind=None)


def _industry_alias_tables_ready() -> bool:
    return _relation_exists("industry_alias", relkind="r") or _relation_exists("industry_alias", relkind=None)


def _alias_supports_entity_type() -> bool:
    return _stack_alias_tables_ready() and _column_exists("stack_alias", "entity_type")


def _alias_supports_kind() -> bool:
    return _stack_alias_tables_ready() and _column_exists("stack_alias", "kind")


def _alias_table_for(kind: str) -> tuple[Optional[str], Optional[tuple[str, str]]]:
    """
    returns (table_name, filter)
      - filter: (column_name, value) or None

    priority:
      1) stack_alias.entity_type
      2) stack_alias.kind
      3) legacy: STACK => stack_alias, INDUSTRY => industry_alias(if exists)
    """
    kind_u = (kind or "").upper()

    if _alias_supports_entity_type():
        return "stack_alias", ("entity_type", kind_u)

    if _alias_supports_kind():
        return "stack_alias", ("kind", kind_u)

    if kind_u == "STACK":
        return ("stack_alias" if _stack_alias_tables_ready() else None), None

    if kind_u == "INDUSTRY":
        return ("industry_alias" if _industry_alias_tables_ready() else None), None

    return None, None


def _resolve_alias_token(
        cur,
        raw: str,
        *,
        kind: str,
        trigram_threshold: float = 0.42,
) -> Tuple[str, Optional[dict]]:
    """
    raw token -> canonical token (alias-based)
    priority:
      1) exact alias
      2) trigram alias (pg_trgm)
    """
    q = _normalize_token(raw)
    if not q:
        return q, None

    table, flt = _alias_table_for(kind)
    if not table:
        return q, None

    # ---------- 1) exact alias (fast path: alias = q) ----------
    if flt:
        col, val = flt
        cur.execute(
            f"SELECT canonical FROM {table} WHERE is_active AND {col} = %s AND alias = %s LIMIT 1",
            (val, q),
        )
    else:
        cur.execute(
            f"SELECT canonical FROM {table} WHERE is_active AND alias = %s LIMIT 1",
            (q,),
        )
    row = cur.fetchone()
    if row and row[0]:
        canon = _normalize_token(str(row[0]))
        if canon and canon != q:
            return canon, {"from": raw, "to": canon, "via": f"{kind.lower()}_alias_exact"}
        return canon, None

    # ---------- 1-b) exact alias (fallback: lower(alias)=q) ----------
    # (ASCII 대소문자/입력 변형 방어. 테이블이 크지 않으면 충분히 감당 가능)
    if q.isascii():
        if flt:
            col, val = flt
            cur.execute(
                f"SELECT canonical FROM {table} WHERE is_active AND {col} = %s AND lower(alias) = %s LIMIT 1",
                (val, q),
            )
        else:
            cur.execute(
                f"SELECT canonical FROM {table} WHERE is_active AND lower(alias) = %s LIMIT 1",
                (q,),
            )
        row = cur.fetchone()
        if row and row[0]:
            canon = _normalize_token(str(row[0]))
            if canon and canon != q:
                return canon, {"from": raw, "to": canon, "via": f"{kind.lower()}_alias_exact"}
            return canon, None

    # ---------- 2) trigram alias ----------
    if _has_pg_trgm():
        if flt:
            col, val = flt
            cur.execute(
                f"""
                SELECT alias, canonical, similarity(alias, %s) AS sim
                FROM {table}
                WHERE is_active
                  AND {col} = %s
                  AND alias %% %s
                ORDER BY sim DESC
                LIMIT 1
                """,
                (q, val, q),
            )
        else:
            cur.execute(
                f"""
                SELECT alias, canonical, similarity(alias, %s) AS sim
                FROM {table}
                WHERE is_active
                  AND alias %% %s
                ORDER BY sim DESC
                LIMIT 1
                """,
                (q, q),
            )
        r = cur.fetchone()
        if r and r[1]:
            sim = float(r[2] or 0.0)
            if sim >= trigram_threshold:
                canon = _normalize_token(str(r[1]))
                if canon and canon != q:
                    return canon, {
                        "from": raw,
                        "to": canon,
                        "via": f"{kind.lower()}_alias_trgm",
                        "sim": sim,
                    }
                return canon, None

    return q, None


def _expand_aliases_for_canon(
        cur,
        canon_tokens: List[str],
        *,
        kind: str,
        max_alias_per_canon: int = 50,
) -> Dict[str, List[str]]:
    """
    canonical -> [canonical + aliases]
    """
    canon_tokens = [_normalize_token(c) for c in (canon_tokens or []) if _normalize_token(c)]
    out: Dict[str, List[str]] = {c: [c] for c in canon_tokens}

    table, flt = _alias_table_for(kind)
    if not canon_tokens or not table:
        return out

    # canonical 매칭은 데이터가 대부분 lower로 들어간다는 전제(너 SQL이 그렇게 넣음)
    # 그래도 방어적으로 lower(canonical)로 비교
    if flt:
        col, val = flt
        cur.execute(
            f"""
            SELECT canonical, alias
            FROM {table}
            WHERE is_active
              AND {col} = %s
              AND lower(canonical) = ANY(%s::text[])
            """,
            (val, canon_tokens),
        )
    else:
        cur.execute(
            f"""
            SELECT canonical, alias
            FROM {table}
            WHERE is_active
              AND lower(canonical) = ANY(%s::text[])
            """,
            (canon_tokens,),
        )

    rows = cur.fetchall() or []
    for canonical, alias in rows:
        c = _normalize_token(str(canonical))
        a = _normalize_token(str(alias))
        if not c or not a:
            continue
        lst = out.setdefault(c, [c])
        if a not in lst:
            lst.append(a)
        if len(lst) > max_alias_per_canon:
            out[c] = lst[:max_alias_per_canon]

    return out


# -----------------------------
# Stack vocabulary resolution (alias + stack_vocab)
# -----------------------------

def _resolve_stack_token(cur, raw: str, *, trigram_threshold: float = 0.42) -> Tuple[str, Optional[dict]]:
    """
    raw token -> canonical token
    priority:
      1) exact/trgm alias (stack_alias, entity_type='STACK')
      2) vocab_trgm (stack_vocab) then alias
    """
    q = _normalize_token(raw)
    if not q:
        return q, None

    # 1) alias (STACK)
    canon, corr = _resolve_alias_token(cur, raw, kind="STACK", trigram_threshold=trigram_threshold)
    if corr and corr.get("via") == "stack_alias_exact":
        return canon, corr
    if canon and canon != q:
        if corr:
            return canon, corr

    # 2) vocab_trgm
    if _has_pg_trgm() and _stack_vocab_ready():
        cur.execute(
            """
            SELECT token, similarity(token, %s) AS sim
            FROM stack_vocab
            WHERE token %% %s
            ORDER BY sim DESC, cnt DESC
                LIMIT 1
            """,
            (q, q),
        )
        row = cur.fetchone()
        if row and row[0]:
            tok = _normalize_token(str(row[0]))
            sim = float(row[1] or 0.0)

            if tok and sim >= trigram_threshold:
                # vocab token -> alias canonical (if exists)
                canon2, _ = _resolve_alias_token(cur, tok, kind="STACK", trigram_threshold=trigram_threshold)
                if canon2:
                    if canon2 != q:
                        return canon2, {"from": raw, "to": canon2, "via": "vocab_trgm+alias", "sim": sim}
                    return canon2, None

                if tok != q:
                    return tok, {"from": raw, "to": tok, "via": "vocab_trgm", "sim": sim}
                return tok, None

    return q, None

__all__ = [
    "_stack_alias_tables_ready",
    "_stack_vocab_ready",
    "_industry_alias_tables_ready",
    "_alias_supports_entity_type",
    "_alias_supports_kind",
    "_alias_table_for",
    "_resolve_alias_token",
    "_expand_aliases_for_canon",
    "_resolve_stack_token",
]
