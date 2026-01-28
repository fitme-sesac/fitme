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
# Canonical group expansion (NO new tables)
# -----------------------------

CANONICAL_GROUP_EXPAND: Dict[str, List[str]] = {
    "javascript": ["javascript", "nodejs", "vuejs", "nextjs"],
}


def _canon_members_for_query(canon: str) -> List[str]:
    c = _normalize_token(canon)
    if not c:
        return []
    members = CANONICAL_GROUP_EXPAND.get(c)
    if not members:
        return [c]
    out: List[str] = []
    for m in members:
        nm = _normalize_token(m)
        if nm and nm not in out:
            out.append(nm)
    if c not in out:
        out.insert(0, c)
    return out


# -----------------------------
# Stack vocabulary resolution
# (stack_alias + stack_vocab)
# -----------------------------

def _stack_alias_tables_ready() -> bool:
    return _relation_exists("stack_alias", relkind="r") or _relation_exists("stack_alias", relkind=None)


def _stack_vocab_ready() -> bool:
    return _relation_exists("stack_vocab", relkind="m") or _relation_exists("stack_vocab", relkind=None)


def _resolve_stack_token(cur, raw: str, *, trigram_threshold: float = 0.42) -> Tuple[str, Optional[dict]]:
    """
    raw token -> canonical token
    priority:
      1) exact alias (stack_alias)
      2) trigram vocab (stack_vocab) then alias
    """
    q = _normalize_token(raw)
    if not q:
        return q, None

    # 1) exact alias mapping
    if _stack_alias_tables_ready():
        cur.execute(
            "SELECT canonical FROM stack_alias WHERE is_active AND alias = %s LIMIT 1",
            (q,),
        )
        row = cur.fetchone()
        if row and row[0]:
            canon = _normalize_token(str(row[0]))
            if canon and canon != q:
                return canon, {"from": raw, "to": canon, "via": "alias_exact"}
            return canon, None

    # 2) fuzzy lookup on vocab
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
                # map vocab token -> canonical via alias, if possible
                if _stack_alias_tables_ready():
                    cur.execute(
                        "SELECT canonical FROM stack_alias WHERE is_active AND alias = %s LIMIT 1",
                        (tok,),
                    )
                    r2 = cur.fetchone()
                    if r2 and r2[0]:
                        canon = _normalize_token(str(r2[0]))
                        if canon and canon != q:
                            return canon, {"from": raw, "to": canon, "via": "vocab_trgm+alias", "sim": sim}
                        return canon, None

                if tok != q:
                    return tok, {"from": raw, "to": tok, "via": "vocab_trgm", "sim": sim}
                return tok, None

    return q, None


def _expand_aliases_for_canon(cur, canon_tokens: List[str], max_alias_per_canon: int = 50) -> Dict[str, List[str]]:
    """
    canonical -> [canonical, alias1, alias2, ...]
    """
    canon_tokens = [_normalize_token(c) for c in (canon_tokens or []) if _normalize_token(c)]
    out: Dict[str, List[str]] = {c: [c] for c in canon_tokens}

    if not canon_tokens or not _stack_alias_tables_ready():
        return out

    cur.execute(
        """
        SELECT canonical, alias
        FROM stack_alias
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
# WHERE builder (supports variants)
# -----------------------------

def _keyword_group_predicate(alias: Optional[str] = None) -> List[str]:
    prefix = f"{alias}." if alias else ""
    preds = [
        f"{prefix}title ILIKE %(kw)s",
        f"{prefix}description ILIKE %(kw)s",
    ]

    if _column_exists("job_posting", "stack"):
        preds.insert(
            0,
            f"""
            EXISTS (
              SELECT 1
              FROM unnest(coalesce({prefix}stack, ARRAY[]::text[])) AS st(item)
              CROSS JOIN LATERAL regexp_split_to_table(coalesce(st.item, ''), '\\s*[,/|]+\\s*') AS tok
              WHERE lower(btrim(tok)) ILIKE lower(%(kw)s)
            )
            """.strip(),
        )
    return preds


def _build_keywords_where_and_params_v2(
        keywords_all_groups: List[List[str]],
        keywords_any_groups: List[List[str]],
        alias: Optional[str] = None,
        param_offset: int = 0,
) -> Tuple[List[str], Dict[str, Any]]:
    """
    groups:
      - keywords_all_groups: AND of (OR variants)
      - keywords_any_groups: OR of (OR variants)
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


# -----------------------------
# Salary predicates (VARCHAR salary_text only)
# -----------------------------

def _salary_amount_m_expr(part_sql: str) -> str:
    eok = f"NULLIF((regexp_match({part_sql}, '(\\d+)\\s*억'))[1], '')::bigint"
    thou_after_eok = f"NULLIF((regexp_match({part_sql}, '억\\s*(\\d+)\\s*천'))[1], '')::bigint"
    thou_only = f"NULLIF((regexp_match({part_sql}, '(\\d+)\\s*천'))[1], '')::bigint"
    digits = f"NULLIF(regexp_replace({part_sql}, '[^0-9]', '', 'g'), '')::bigint"

    return f"""
    CASE
      WHEN {part_sql} ~ '억' THEN COALESCE({eok},0)*10000 + COALESCE({thou_after_eok},0)*1000
      WHEN {part_sql} ~ '천' THEN COALESCE({thou_only},0)*1000
      WHEN {digits} IS NULL THEN NULL
      WHEN {digits} >= 1000000 THEN ({digits} / 10000)
      ELSE {digits}
    END
    """


def _salary_bounds_m_expr(salary_col: str) -> tuple[str, str]:
    col = f"COALESCE(({salary_col})::text, '')"
    parts = f"(regexp_split_to_array({col}, '\\s*[~∼〜\\-–]\\s*'))"
    p1 = f"COALESCE({parts}[1], '')"
    p2 = f"COALESCE({parts}[2], '')"

    lo_raw = _salary_amount_m_expr(p1)
    hi_raw = f"COALESCE({_salary_amount_m_expr(p2)}, {lo_raw})"

    lo = f"LEAST(({lo_raw}), ({hi_raw}))"
    hi = f"GREATEST(({lo_raw}), ({hi_raw}))"
    return lo, hi


def _salary_min_predicate(min_salary_m만원: int, salary_col: str = "salary_text") -> str:
    lo, hi = _salary_bounds_m_expr(salary_col)
    return f"(({lo}) IS NOT NULL AND ({hi}) >= %(min_salary_m)s)"


def _salary_between_predicate(min_salary_m만원: int, max_salary_m만원: int, salary_col: str = "salary_text") -> str:
    lo, hi = _salary_bounds_m_expr(salary_col)
    return f"(({lo}) IS NOT NULL AND ({lo}) <= %(max_salary_m)s AND ({hi}) >= %(min_salary_m)s)"


# -----------------------------
# Competition(%) expression + filters
# -----------------------------

def _competition_pct_expr(alias: str = "jp") -> str:
    return f"(({alias}.apply_count::float / NULLIF({alias}.recruitment_capacity, 0)) * 100.0)"


def _apply_competition_pct_filters(
        where: List[str],
        params: Dict[str, Any],
        *,
        alias: str = "jp",
        min_competition_pct: Optional[float] = None,
        max_competition_pct: Optional[float] = None,
        require_capacity: bool = False,
) -> None:
    # 임계값 필터가 있거나, RATE_STATS 같이 % 계산이 필수면 capacity 조건 강제
    if require_capacity or (min_competition_pct is not None) or (max_competition_pct is not None):
        where.append(f"{alias}.recruitment_capacity IS NOT NULL")
        where.append(f"{alias}.recruitment_capacity > 0")

    expr = _competition_pct_expr(alias)

    if min_competition_pct is not None:
        where.append(f"{expr} >= %(min_comp_pct)s")
        params["min_comp_pct"] = float(min_competition_pct)

    if max_competition_pct is not None:
        where.append(f"{expr} <= %(max_comp_pct)s")
        params["max_comp_pct"] = float(max_competition_pct)


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
            trigram_threshold: float = 0.42,
            max_items: int = 30,
    ) -> Tuple[List[List[str]], List[List[str]], List[dict]]:
        all_n = _normalize_kw_list(keywords_all or [], max_items=max_items)
        any_n = _normalize_kw_list(keywords_any or [], max_items=max_items)

        corrections: List[dict] = []
        requested_all: List[str] = []
        requested_any: List[str] = []

        for raw in all_n:
            canon, corr = _resolve_stack_token(cur, raw, trigram_threshold=trigram_threshold)
            if canon:
                requested_all.append(canon)
            if corr:
                corrections.append(corr)

        for raw in any_n:
            canon, corr = _resolve_stack_token(cur, raw, trigram_threshold=trigram_threshold)
            if canon:
                requested_any.append(canon)
            if corr:
                corrections.append(corr)

        expanded_pool: List[str] = []
        for c in dict.fromkeys([*requested_all, *requested_any]):
            for m in _canon_members_for_query(c):
                if m not in expanded_pool:
                    expanded_pool.append(m)

        canon_to_aliases = _expand_aliases_for_canon(cur, expanded_pool)

        def build_variant_group(requested_canon: str) -> List[str]:
            members = _canon_members_for_query(requested_canon)

            variants: List[str] = []
            seen = set()

            rc = _normalize_token(requested_canon)
            if rc and rc not in seen:
                variants.append(rc)
                seen.add(rc)

            for m in members:
                for a in canon_to_aliases.get(m, [m]):
                    aa = _normalize_token(a)
                    if aa and aa not in seen:
                        variants.append(aa)
                        seen.add(aa)

            return variants

        all_groups = [build_variant_group(c) for c in requested_all]
        any_groups = [build_variant_group(c) for c in requested_any]

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
            max_salary_m만원: Optional[int] = None,
            min_competition_pct: Optional[float] = None,
            max_competition_pct: Optional[float] = None,
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

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups, alias="jp", param_offset=0)
                where.extend(kw_where)
                params.update(kw_params)

                if min_salary_m만원 is not None and max_salary_m만원 is not None:
                    where.append(_salary_between_predicate(int(min_salary_m만원), int(max_salary_m만원), salary_col="jp.salary_text"))
                    params["min_salary_m"] = int(min_salary_m만원)
                    params["max_salary_m"] = int(max_salary_m만원)
                elif min_salary_m만원 is not None:
                    where.append(_salary_min_predicate(int(min_salary_m만원), salary_col="jp.salary_text"))
                    params["min_salary_m"] = int(min_salary_m만원)

                _apply_competition_pct_filters(
                    where, params, alias="jp",
                    min_competition_pct=min_competition_pct,
                    max_competition_pct=max_competition_pct,
                    require_capacity=False,
                )

                sql = f"""
                    SELECT COUNT(*)::bigint AS cnt
                    FROM job_posting jp
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
                    "keywords_all": [g[0] for g in all_groups] if all_groups else [],
                    "keywords_any": [g[0] for g in any_groups] if any_groups else [],
                    "job_role": job_role,
                    "min_salary_m만원": min_salary_m만원,
                    "max_salary_m만원": max_salary_m만원,
                    "min_competition_pct": min_competition_pct,
                    "max_competition_pct": max_competition_pct,
                    "scoped": bool(job_ids_scope),
                    "scope_size": len(job_ids_scope or []),
                    "stack_corrections": stack_corr,
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
            min_competition_pct: Optional[float] = None,
            max_competition_pct: Optional[float] = None,
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

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups, alias="jp", param_offset=0)
                where.extend(kw_where)
                params.update(kw_params)

                _apply_competition_pct_filters(
                    where, params, alias="jp",
                    min_competition_pct=min_competition_pct,
                    max_competition_pct=max_competition_pct,
                    require_capacity=False,
                )

                sql = f"""
                    SELECT
                        COUNT(*)::bigint AS postings,
                        COALESCE(SUM(jp.apply_count), 0)::bigint AS applications,
                        CASE WHEN COUNT(*) = 0 THEN 0
                             ELSE (COALESCE(SUM(jp.apply_count), 0)::float / COUNT(*))
                        END AS avg_apply_per_posting
                    FROM job_posting jp
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
                    "min_competition_pct": min_competition_pct,
                    "max_competition_pct": max_competition_pct,
                    "scoped": bool(job_ids_scope),
                    "scope_size": len(job_ids_scope or []),
                    "stack_corrections": stack_corr,
                }
        finally:
            conn.close()

    def rate_stats(
            self,
            start_date: date,
            end_date: date,
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            keywords_all: Optional[List[str]] = None,
            keywords_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
            min_salary_m만원: Optional[int] = None,
            max_salary_m만원: Optional[int] = None,
            min_competition_pct: Optional[float] = None,
            max_competition_pct: Optional[float] = None,
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

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups, alias="jp", param_offset=0)
                where.extend(kw_where)
                params.update(kw_params)

                if min_salary_m만원 is not None and max_salary_m만원 is not None:
                    where.append(_salary_between_predicate(int(min_salary_m만원), int(max_salary_m만원), salary_col="jp.salary_text"))
                    params["min_salary_m"] = int(min_salary_m만원)
                    params["max_salary_m"] = int(max_salary_m만원)
                elif min_salary_m만원 is not None:
                    where.append(_salary_min_predicate(int(min_salary_m만원), salary_col="jp.salary_text"))
                    params["min_salary_m"] = int(min_salary_m만원)

                # RATE_STATS는 % 계산이 핵심이므로 capacity 조건을 강제
                _apply_competition_pct_filters(
                    where, params, alias="jp",
                    min_competition_pct=min_competition_pct,
                    max_competition_pct=max_competition_pct,
                    require_capacity=True,
                )

                comp_expr = _competition_pct_expr("jp")

                sql = f"""
                    SELECT
                        COUNT(*)::bigint AS postings,
                        COALESCE(SUM(jp.apply_count), 0)::bigint AS applications,
                        COALESCE(SUM(jp.recruitment_capacity), 0)::bigint AS total_capacity,
                        CASE WHEN COUNT(*) = 0 THEN 0
                             ELSE (COALESCE(SUM(jp.apply_count), 0)::float / COUNT(*))
                        END AS avg_apply_per_posting,
                        CASE WHEN COALESCE(SUM(jp.recruitment_capacity),0) = 0 THEN 0
                             ELSE (COALESCE(SUM(jp.apply_count),0)::float
                                   / COALESCE(SUM(jp.recruitment_capacity),0)::float) * 100.0
                        END AS apply_rate_weighted_pct,
                        CASE WHEN COUNT(*) = 0 THEN 0
                             ELSE AVG({comp_expr})
                        END AS competition_avg_pct
                    FROM job_posting jp
                    WHERE {" AND ".join(where)}
                """
                cur.execute(sql, params)
                r = cur.fetchone() or (0, 0, 0, 0, 0, 0)

                return {
                    "postings": int(r[0] or 0),
                    "applications": int(r[1] or 0),
                    "total_capacity": int(r[2] or 0),
                    "avg_apply_per_posting": float(r[3] or 0.0),
                    "apply_rate_weighted_pct": float(r[4] or 0.0),
                    "competition_avg_pct": float(r[5] or 0.0),
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "keywords_all": [g[0] for g in all_groups] if all_groups else [],
                    "keywords_any": [g[0] for g in any_groups] if any_groups else [],
                    "job_role": job_role,
                    "min_salary_m만원": min_salary_m만원,
                    "max_salary_m만원": max_salary_m만원,
                    "min_competition_pct": min_competition_pct,
                    "max_competition_pct": max_competition_pct,
                    "scoped": bool(job_ids_scope),
                    "scope_size": len(job_ids_scope or []),
                    "stack_corrections": stack_corr,
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
            max_salary_m만원: Optional[int] = None,
            min_competition_pct: Optional[float] = None,
            max_competition_pct: Optional[float] = None,
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

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups, alias="jp", param_offset=0)
                where.extend(kw_where)
                params.update(kw_params)

                if min_salary_m만원 is not None and max_salary_m만원 is not None:
                    where.append(_salary_between_predicate(int(min_salary_m만원), int(max_salary_m만원), salary_col="jp.salary_text"))
                    params["min_salary_m"] = int(min_salary_m만원)
                    params["max_salary_m"] = int(max_salary_m만원)
                elif min_salary_m만원 is not None:
                    where.append(_salary_min_predicate(int(min_salary_m만원), salary_col="jp.salary_text"))
                    params["min_salary_m"] = int(min_salary_m만원)

                _apply_competition_pct_filters(
                    where, params, alias="jp",
                    min_competition_pct=min_competition_pct,
                    max_competition_pct=max_competition_pct,
                    require_capacity=False,
                )

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
                      jp.recruitment_capacity,
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
                    st = r[5]
                    if isinstance(st, list):
                        flat: List[str] = []
                        for item in st:
                            s = (item or "").strip()
                            if not s:
                                continue
                            flat.extend([p for p in _SEP_RE.split(s) if p])
                        stack_str = ", ".join(dict.fromkeys([x.strip() for x in flat if x.strip()]))
                    else:
                        stack_str = st

                    apply_count = int(r[6] or 0)
                    cap = int(r[7] or 0)
                    comp_pct = (apply_count / cap * 100.0) if cap > 0 else None

                    items.append(
                        {
                            "job_id": int(r[0]),
                            "employer_name": r[1],
                            "title": r[2],
                            "location": r[3],
                            "salary_text": r[4],
                            "stack": stack_str,
                            "apply_count": apply_count,
                            "recruitment_capacity": (cap if r[7] is not None else None),
                            "competition_pct": comp_pct,
                            "created_at": (r[8].isoformat() if r[8] else None),
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
                    "max_salary_m만원": max_salary_m만원,
                    "min_competition_pct": min_competition_pct,
                    "max_competition_pct": max_competition_pct,
                    "limit": lim,
                    "random": bool(random),
                    "scoped": bool(job_ids_scope),
                    "scope_size": len(job_ids_scope or []),
                    "stack_corrections": stack_corr,
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

        kw_all: List[str] = []
        kw_any: List[str] = [job_role] if job_role else []

        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                all_groups, any_groups, stack_corr = self._resolve_and_expand_keywords(cur, kw_all, kw_any)

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups, alias="jp", param_offset=0)
                where.extend(kw_where)
                params.update(kw_params)

                lim = max(1, min(50, int(limit)))
                params["lim"] = lim

                sql = f"""
                    WITH tokens AS (
                      SELECT
                        lower(btrim(tok)) AS tok
                      FROM job_posting jp
                      CROSS JOIN LATERAL unnest(coalesce(jp.stack, ARRAY[]::text[])) AS st(item)
                      CROSS JOIN LATERAL regexp_split_to_table(coalesce(st.item, ''), '\\s*[,/|]+\\s*') AS tok
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
                    "stack_corrections": stack_corr,
                }
        finally:
            conn.close()


job_stats_repo = JobStatsRepository()
