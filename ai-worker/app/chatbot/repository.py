# app/chatbot/repository.py
from __future__ import annotations

import functools
import re
from datetime import date
from typing import Any, Dict, List, Optional, Tuple

from app.chatbot.utils.location import apply_location_filters
from app.core.database import get_db_connection

# -----------------------------
# Helpers: schema introspection
# -----------------------------

@functools.lru_cache(maxsize=64)
def _column_exists(table_name: str, column_name: str) -> bool:
    conn = get_db_connection()
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = %s
                  AND column_name = %s
                    LIMIT 1
                """,
                (table_name, column_name),
            )
            return cur.fetchone() is not None
    finally:
        conn.close()


@functools.lru_cache(maxsize=64)
def _relation_exists(relname: str, relkind: Optional[str] = None) -> bool:
    """
    relkind: 'r'(table), 'm'(materialized view), 'v'(view), None(any)
    """
    conn = get_db_connection()
    try:
        with conn.cursor() as cur:
            if relkind:
                cur.execute(
                    """
                    SELECT 1
                    FROM pg_class c
                             JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'public'
                      AND c.relname = %s
                      AND c.relkind = %s
                        LIMIT 1
                    """,
                    (relname, relkind),
                )
            else:
                cur.execute(
                    """
                    SELECT 1
                    FROM pg_class c
                             JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'public'
                      AND c.relname = %s
                        LIMIT 1
                    """,
                    (relname,),
                )
            return cur.fetchone() is not None
    finally:
        conn.close()


@functools.lru_cache(maxsize=8)
def _has_pg_trgm() -> bool:
    """
    pg_trgm 설치 여부.
    - stack_vocab fuzzy lookup을 위해 similarity(), % 연산자(opclass)가 필요.
    """
    conn = get_db_connection()
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT 1 FROM pg_extension WHERE extname='pg_trgm' LIMIT 1")
            return cur.fetchone() is not None
    finally:
        conn.close()


# -----------------------------
# Keyword normalization
# -----------------------------

_SPACE_RE = re.compile(r"\s+")
_SEP_RE = re.compile(r"\s*[,/|]\s*")

def _normalize_token(s: str, max_len: int = 50) -> str:
    s = (s or "").strip().lower()
    s = _SPACE_RE.sub(" ", s)
    if len(s) > max_len:
        s = s[:max_len]
    return s

def _normalize_kw_list(xs: List[str], max_items: int = 30, max_len: int = 50) -> List[str]:
    out: List[str] = []
    for x in xs or []:
        s = _normalize_token(x, max_len=max_len)
        if not s:
            continue
        if s not in out:
            out.append(s)
        if len(out) >= max_items:
            break
    return out


# -----------------------------
# Stack vocabulary resolution
# (stack_alias + stack_vocab)
# -----------------------------

def _stack_alias_tables_ready() -> bool:
    # stack_alias는 테이블, stack_vocab은 MV(권장). 없으면 기능을 degrade.
    return _relation_exists("stack_alias", relkind="r") or _relation_exists("stack_alias", relkind=None)

def _stack_vocab_ready() -> bool:
    return _relation_exists("stack_vocab", relkind="m") or _relation_exists("stack_vocab", relkind=None)

def _resolve_stack_token(
        cur,
        raw: str,
        *,
        trigram_threshold: float = 0.45,
) -> Tuple[str, Optional[dict]]:
    """
    raw -> canonical (lowercase)
    correction dict: {"from": raw, "to": canonical, "via": "...", "sim": ...}
    """
    q = _normalize_token(raw)
    if not q:
        return q, None

    # 1) exact alias mapping: alias -> canonical
    if _stack_alias_tables_ready():
        cur.execute("SELECT canonical FROM stack_alias WHERE alias = %s LIMIT 1", (q,))
        row = cur.fetchone()
        if row and row[0]:
            canon = _normalize_token(str(row[0]))
            if canon and canon != q:
                return canon, {"from": raw, "to": canon, "via": "alias_exact"}
            return canon, None

    # 2) if user already typed canonical-ish, accept as-is
    # (단, alias table이 있어도 canonical 자체를 alias로 넣지 않았을 수 있음)
    # -> 아래에서 fuzzy까지 보고, 없으면 q 그대로 사용

    # 3) fuzzy lookup using stack_vocab (pg_trgm required)
    if _has_pg_trgm() and _stack_vocab_ready():
        # token % q  : trigram "similar" operator (uses gin_trgm_ops index)
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
                # tok itself may be an alias -> canonicalize again
                if _stack_alias_tables_ready():
                    cur.execute("SELECT canonical FROM stack_alias WHERE alias = %s LIMIT 1", (tok,))
                    r2 = cur.fetchone()
                    if r2 and r2[0]:
                        canon = _normalize_token(str(r2[0]))
                        if canon and canon != q:
                            return canon, {"from": raw, "to": canon, "via": "vocab_trgm+alias", "sim": sim}
                        return canon, None

                if tok != q:
                    return tok, {"from": raw, "to": tok, "via": "vocab_trgm", "sim": sim}
                return tok, None

    # fallback: keep normalized user token
    return q, None


def _expand_aliases_for_canon(cur, canon_tokens: List[str], max_alias_per_canon: int = 50) -> Dict[str, List[str]]:
    """
    canon -> [alias1, alias2, ...] (including canon itself)
    """
    out: Dict[str, List[str]] = {c: [c] for c in canon_tokens if c}

    if not canon_tokens or not _stack_alias_tables_ready():
        return out

    # aliases where canonical in canon_tokens
    cur.execute(
        """
        SELECT canonical, alias
        FROM stack_alias
        WHERE canonical = ANY(%s::text[])
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
            # cap
            out[c] = lst[:max_alias_per_canon]

    return out


# -----------------------------
# WHERE builder (supports variants)
# -----------------------------

def _keyword_group_predicate(alias: Optional[str] = None) -> List[str]:
    """
    One keyword matches if it appears in ANY of these fields (OR group).
    preds contain a placeholder %(kw)s which will be replaced with param keys.
    """
    prefix = f"{alias}." if alias else ""

    preds = [
        f"{prefix}title ILIKE %(kw)s",
        f"{prefix}description ILIKE %(kw)s",
    ]

    # stack is text[]: tokenize each element by separators and match any token
    if _column_exists("job_posting", "stack"):
        preds.insert(
            0,
            f"""
            EXISTS (
              SELECT 1
              FROM unnest(coalesce({prefix}stack, ARRAY[]::text[])) AS st(item)
              CROSS JOIN LATERAL regexp_split_to_table(coalesce(st.item, ''), '\\s*[,/|]\\s*') AS tok
              WHERE tok ILIKE %(kw)s
            )
            """.strip()
        )
    return preds


def _build_keywords_where_and_params_v2(
        keywords_all_groups: List[List[str]],
        keywords_any_groups: List[List[str]],
        alias: Optional[str] = None,
        param_offset: int = 0,
) -> Tuple[List[str], Dict[str, Any]]:
    """
    keywords_all_groups: AND across groups. Each group is OR across its variants.
    keywords_any_groups: OR across groups. Each group is OR across its variants.
    """
    where_parts: List[str] = []
    params: Dict[str, Any] = {}
    preds = _keyword_group_predicate(alias=alias)

    idx = param_offset

    def _group_sql_for_variants(variants: List[str]) -> str:
        nonlocal idx
        variant_clauses: List[str] = []
        for v in variants:
            key = f"kw{idx}"
            params[key] = f"%{v}%"
            field_or = "(" + " OR ".join(p.replace("%(kw)s", f"%({key})s") for p in preds) + ")"
            variant_clauses.append(field_or)
            idx += 1
        return "(" + " OR ".join(variant_clauses) + ")"

    # AND group
    for variants in keywords_all_groups:
        variants = [v for v in variants if v]
        if not variants:
            continue
        where_parts.append(_group_sql_for_variants(variants))

    # OR group
    if keywords_any_groups:
        or_groups: List[str] = []
        for variants in keywords_any_groups:
            variants = [v for v in variants if v]
            if not variants:
                continue
            or_groups.append(_group_sql_for_variants(variants))
        if or_groups:
            where_parts.append("(" + " OR ".join(or_groups) + ")")

    return where_parts, params


def _salary_min_predicate(min_salary_m만원: int, salary_col: str = "salary_text") -> str:
    digits = f"NULLIF(regexp_replace({salary_col}, '[^0-9]', '', 'g'), '')::bigint"
    return f"""
    (
      {salary_col} IS NOT NULL AND
      (
        {digits} >= %(min_salary_m)s
        OR
        {digits} >= (%(min_salary_m)s * 10000)
      )
    )
    """


# -----------------------------
# Repository
# -----------------------------

class JobStatsRepository:
    def _resolve_and_expand_keywords(
            self,
            cur,
            keywords_all: List[str],
            keywords_any: List[str],
            *,
            trigram_threshold: float = 0.45,
            max_items: int = 30,
    ) -> Tuple[List[List[str]], List[List[str]], List[dict]]:
        """
        Returns:
          - keywords_all_groups: [[variants...], ...]
          - keywords_any_groups: [[variants...], ...]
          - corrections: list[dict]
        """
        all_n = _normalize_kw_list(keywords_all or [], max_items=max_items)
        any_n = _normalize_kw_list(keywords_any or [], max_items=max_items)

        corrections: List[dict] = []

        # resolve each token -> canonical
        all_canon: List[str] = []
        any_canon: List[str] = []

        for raw in all_n:
            canon, corr = _resolve_stack_token(cur, raw, trigram_threshold=trigram_threshold)
            if canon:
                all_canon.append(canon)
            if corr:
                corrections.append(corr)

        for raw in any_n:
            canon, corr = _resolve_stack_token(cur, raw, trigram_threshold=trigram_threshold)
            if canon:
                any_canon.append(canon)
            if corr:
                corrections.append(corr)

        # expand canonical -> aliases
        canon_set = list(dict.fromkeys([*all_canon, *any_canon]))
        canon_to_aliases = _expand_aliases_for_canon(cur, canon_set)

        # build groups (variants per keyword)
        all_groups: List[List[str]] = []
        for c in all_canon:
            all_groups.append(canon_to_aliases.get(c, [c]))

        any_groups: List[List[str]] = []
        for c in any_canon:
            any_groups.append(canon_to_aliases.get(c, [c]))

        return all_groups, any_groups, corrections

    def count_postings(
            self,
            start_date: date,
            end_date: date,
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            keywords_all: Optional[List[str]] = None,
            keywords_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
            min_salary_m만원: Optional[int] = None,
    ) -> Dict[str, Any]:
        # job_role은 "힌트"로 OR에 추가
        kw_all = keywords_all or []
        kw_any = (keywords_any or [])
        if job_role:
            kw_any = [job_role] + kw_any

        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                # resolve + expand (alias/typo)
                all_groups, any_groups, stack_corr = self._resolve_and_expand_keywords(cur, kw_all, kw_any)

                where = [
                    "deleted_at IS NULL",
                    "status = 'OPEN'",
                    "created_at >= %(start_ts)s",
                    "created_at < %(end_ts)s",
                ]
                params: Dict[str, Any] = {
                    "start_ts": f"{start_date} 00:00:00",
                    "end_ts": f"{end_date} 00:00:00",
                }

                apply_location_filters(where, params, regions_any, admin_areas_any, location_col="location")

                kw_where, kw_params = _build_keywords_where_and_params_v2(
                    all_groups, any_groups, alias=None, param_offset=0
                )
                where.extend(kw_where)
                params.update(kw_params)

                if min_salary_m만원 is not None:
                    where.append(_salary_min_predicate(int(min_salary_m만원), salary_col="salary_text"))
                    params["min_salary_m"] = int(min_salary_m만원)

                sql = f"""
                    SELECT COUNT(*)::bigint AS cnt
                    FROM job_posting
                    WHERE {" AND ".join(where)}
                """
                cur.execute(sql, params)
                row = cur.fetchone()
                cnt = int(row[0] if row and row[0] is not None else 0)

                return {
                    "count": cnt,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    # canonical/expanded 결과를 “노출용”으로 남겨두면 answer 단계에서 문구 만들기 쉬움
                    "keywords_all": [g[0] for g in all_groups] if all_groups else [],
                    "keywords_any": [g[0] for g in any_groups] if any_groups else [],
                    "job_role": job_role,
                    "min_salary_m만원": min_salary_m만원,
                    "stack_corrections": stack_corr,  # ✅ 추가
                }
        finally:
            conn.close()

    def competition(
            self,
            start_date: date,
            end_date: date,
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            keywords_all: Optional[List[str]] = None,
            keywords_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
    ) -> Dict[str, Any]:
        kw_all = keywords_all or []
        kw_any = (keywords_any or [])
        if job_role:
            kw_any = [job_role] + kw_any

        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                all_groups, any_groups, stack_corr = self._resolve_and_expand_keywords(cur, kw_all, kw_any)

                where = [
                    "deleted_at IS NULL",
                    "status = 'OPEN'",
                    "created_at >= %(start_ts)s",
                    "created_at < %(end_ts)s",
                ]
                params: Dict[str, Any] = {
                    "start_ts": f"{start_date} 00:00:00",
                    "end_ts": f"{end_date} 00:00:00",
                }

                apply_location_filters(where, params, regions_any, admin_areas_any, location_col="location")

                kw_where, kw_params = _build_keywords_where_and_params_v2(
                    all_groups, any_groups, alias=None, param_offset=0
                )
                where.extend(kw_where)
                params.update(kw_params)

                sql = f"""
                    SELECT
                        COUNT(*)::bigint AS postings,
                        COALESCE(SUM(apply_count), 0)::bigint AS applications,
                        CASE WHEN COUNT(*) = 0 THEN 0
                             ELSE (COALESCE(SUM(apply_count), 0)::float / COUNT(*))
                        END AS avg_apply_per_posting
                    FROM job_posting
                    WHERE {" AND ".join(where)}
                """
                cur.execute(sql, params)
                row = cur.fetchone() or (0, 0, 0)

                return {
                    "postings": int(row[0] or 0),
                    "applications": int(row[1] or 0),
                    "avg_apply_per_posting": float(row[2] or 0),
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "keywords_all": [g[0] for g in all_groups] if all_groups else [],
                    "keywords_any": [g[0] for g in any_groups] if any_groups else [],
                    "job_role": job_role,
                    "stack_corrections": stack_corr,  # ✅ 추가
                }
        finally:
            conn.close()

    def list_postings(
            self,
            start_date: date,
            end_date: date,
            request_id: str,
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            keywords_all: Optional[List[str]] = None,
            keywords_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
            min_salary_m만원: Optional[int] = None,
            limit: int = 5,
            random: bool = False,
            job_ids_scope: Optional[List[int]] = None,
    ) -> Dict[str, Any]:
        kw_all = keywords_all or []
        kw_any = (keywords_any or [])
        if job_role:
            kw_any = [job_role] + kw_any

        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                all_groups, any_groups, stack_corr = self._resolve_and_expand_keywords(cur, kw_all, kw_any)

                where = [
                    "jp.deleted_at IS NULL",
                    "jp.status = 'OPEN'",
                    "jp.created_at >= %(start_ts)s",
                    "jp.created_at < %(end_ts)s",
                ]
                params: Dict[str, Any] = {
                    "start_ts": f"{start_date} 00:00:00",
                    "end_ts": f"{end_date} 00:00:00",
                }

                if job_ids_scope:
                    where.append("jp.job_id = ANY(%(scope_ids)s::bigint[])")
                    params["scope_ids"] = [int(x) for x in job_ids_scope]

                apply_location_filters(where, params, regions_any, admin_areas_any, location_col="jp.location")

                kw_where, kw_params = _build_keywords_where_and_params_v2(
                    all_groups, any_groups, alias="jp", param_offset=0
                )
                where.extend(kw_where)
                params.update(kw_params)

                if min_salary_m만원 is not None:
                    where.append(_salary_min_predicate(int(min_salary_m만원), salary_col="jp.salary_text"))
                    params["min_salary_m"] = int(min_salary_m만원)

                if random:
                    order_sql = "md5(jp.job_id::text || %(seed)s)"
                    params["seed"] = request_id
                else:
                    order_sql = "jp.created_at DESC"

                lim = max(1, min(20, int(limit)))
                params["lim"] = lim

                sql = f"""
                    SELECT
                      jp.job_id,
                      e.name AS employer_name,
                      jp.title,
                      jp.location,
                      jp.salary_text,
                      jp.stack,
                      jp.apply_count,
                      jp.created_at
                    FROM job_posting jp
                    JOIN employer e ON e.employer_id = jp.employer_id
                    WHERE {" AND ".join(where)}
                    ORDER BY {order_sql}
                    LIMIT %(lim)s
                """
                cur.execute(sql, params)
                rows = cur.fetchall() or []

                items = []
                for r in rows:
                    # jp.stack is text[] -> psycopg2 typically returns list[str]
                    st = r[5]
                    if isinstance(st, list):
                        # 요소 내부에 "Java, Spring" 같은게 들어올 수 있으니 보기 좋게 평탄화
                        flat: List[str] = []
                        for item in st:
                            s = (item or "").strip()
                            if not s:
                                continue
                            flat.extend([p for p in _SEP_RE.split(s) if p])
                        stack_str = ", ".join(dict.fromkeys([x.strip() for x in flat if x.strip()]))
                    else:
                        stack_str = st

                    items.append(
                        {
                            "job_id": int(r[0]),
                            "employer_name": r[1],
                            "title": r[2],
                            "location": r[3],
                            "salary_text": r[4],
                            "stack": stack_str,
                            "apply_count": int(r[6] or 0),
                            "created_at": (r[7].isoformat() if r[7] else None),
                        }
                    )

                return {
                    "items": items,
                    "count": len(items),
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "keywords_all": [g[0] for g in all_groups] if all_groups else [],
                    "keywords_any": [g[0] for g in any_groups] if any_groups else [],
                    "job_role": job_role,
                    "min_salary_m만원": min_salary_m만원,
                    "limit": lim,
                    "random": bool(random),
                    "scoped": bool(job_ids_scope),
                    "scope_size": len(job_ids_scope or []),
                    "stack_corrections": stack_corr,  # ✅ 추가
                }
        finally:
            conn.close()

    def top_stacks(
            self,
            start_date: date,
            end_date: date,
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
            limit: int = 10,
    ) -> Dict[str, Any]:
        if not _column_exists("job_posting", "stack"):
            return {
                "items": [],
                "start_date": str(start_date),
                "end_date": str(end_date),
                "reason": "job_posting.stack column missing",
            }

        where = [
            "jp.deleted_at IS NULL",
            "jp.status = 'OPEN'",
            "jp.created_at >= %(start_ts)s",
            "jp.created_at < %(end_ts)s",
            "jp.stack IS NOT NULL",
        ]
        params: Dict[str, Any] = {
            "start_ts": f"{start_date} 00:00:00",
            "end_ts": f"{end_date} 00:00:00",
        }

        apply_location_filters(where, params, regions_any, admin_areas_any, location_col="jp.location")

        # job_role도 "힌트"로 keyword-any처럼 적용(원문 그대로)
        # -> resolve/expand를 타게 해서 한/영 섞여도 최대한 잡힘
        kw_all: List[str] = []
        kw_any: List[str] = [job_role] if job_role else []

        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                all_groups, any_groups, stack_corr = self._resolve_and_expand_keywords(cur, kw_all, kw_any)

                kw_where, kw_params = _build_keywords_where_and_params_v2(
                    all_groups, any_groups, alias="jp", param_offset=0
                )
                where.extend(kw_where)
                params.update(kw_params)

                lim = max(1, min(50, int(limit)))
                params["lim"] = lim

                # stack(text[]) -> unnest -> split -> count
                sql = f"""
                    WITH tokens AS (
                      SELECT
                        lower(trim(tok)) AS tok
                      FROM job_posting jp
                      CROSS JOIN LATERAL unnest(coalesce(jp.stack, ARRAY[]::text[])) AS st(item)
                      CROSS JOIN LATERAL regexp_split_to_table(coalesce(st.item, ''), '\\s*[,/|]\\s*') AS tok
                      WHERE {" AND ".join(where)}
                    )
                    SELECT tok, COUNT(*)::bigint AS cnt
                    FROM tokens
                    WHERE tok <> ''
                    GROUP BY tok
                    ORDER BY cnt DESC, tok ASC
                    LIMIT %(lim)s
                """
                cur.execute(sql, params)
                rows = cur.fetchall() or []
                items = [{"stack": r[0], "count": int(r[1])} for r in rows]

                return {
                    "items": items,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "job_role": job_role,
                    "limit": lim,
                    "stack_corrections": stack_corr,  # ✅ 추가
                }
        finally:
            conn.close()


job_stats_repo = JobStatsRepository()
