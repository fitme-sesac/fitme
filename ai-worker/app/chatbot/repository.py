# app/chatbot/repository.py
from __future__ import annotations

import functools
from datetime import date
from typing import Any, Dict, List, Optional, Tuple

from app.chatbot.utils.location import apply_location_filters
from app.core.database import get_db_connection


@functools.lru_cache(maxsize=32)
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


def _normalize_kw_list(xs: List[str], max_items: int = 30, max_len: int = 50) -> List[str]:
    out: List[str] = []
    for x in xs or []:
        s = (x or "").strip()
        if not s:
            continue
        if len(s) > max_len:
            s = s[:max_len]
        if s not in out:
            out.append(s)
        if len(out) >= max_items:
            break
    return out


def _keyword_group_predicate(alias: Optional[str] = None) -> List[str]:
    """
    One keyword matches if it appears in ANY of these fields (OR group).
    """
    prefix = f"{alias}." if alias else ""
    preds = [
        f"{prefix}title ILIKE %(kw)s",
        f"{prefix}description ILIKE %(kw)s",
    ]
    # stack 컬럼이 있으면 포함(옵션)
    if _column_exists("job_posting", "stack"):
        preds.insert(0, f"COALESCE({prefix}stack,'') ILIKE %(kw)s")
    return preds


def _build_keywords_where_and_params(
        keywords_all: List[str],
        keywords_any: List[str],
        alias: Optional[str] = None,
        param_offset: int = 0,
) -> Tuple[List[str], Dict[str, Any]]:
    where_parts: List[str] = []
    params: Dict[str, Any] = {}
    preds = _keyword_group_predicate(alias=alias)

    idx = param_offset

    # AND group: each keyword produces an (OR fields) group, combined with AND
    for k in keywords_all:
        key = f"kw{idx}"
        params[key] = f"%{k}%"
        group = "(" + " OR ".join(p.replace("%(kw)s", f"%({key})s") for p in preds) + ")"
        where_parts.append(group)
        idx += 1

    # OR group: all keywords produce (OR fields) groups, combined with OR
    if keywords_any:
        or_groups: List[str] = []
        for k in keywords_any:
            key = f"kw{idx}"
            params[key] = f"%{k}%"
            group = "(" + " OR ".join(p.replace("%(kw)s", f"%({key})s") for p in preds) + ")"
            or_groups.append(group)
            idx += 1
        where_parts.append("(" + " OR ".join(or_groups) + ")")

    return where_parts, params


def _salary_min_predicate(min_salary_m만원: int, salary_col: str = "salary_text") -> str:
    """
    salary_text is free-form string => heuristic numeric extraction.
    Accept both "만원 단위" and "원 단위" representations.

    - digits >= min_salary_m  (이미 '만원' 단위 숫자만 들어간 경우)
    - digits >= min_salary_m * 10000 (원 단위 숫자만 들어간 경우)
    """
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


class JobStatsRepository:
    def count_postings(
            self,
            start_date: date,
            end_date: date,
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            keywords_all: Optional[List[str]] = None,
            keywords_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,  # free-form string (no hardcoded enum)
            min_salary_m만원: Optional[int] = None,
    ) -> Dict[str, Any]:
        keywords_all_n = _normalize_kw_list(keywords_all or [])
        keywords_any_n = _normalize_kw_list(keywords_any or [])

        # job_role을 "사전 정의" 없이 힌트로만 사용(원문 그대로 OR 키워드로 추가)
        if job_role:
            keywords_any_n = _normalize_kw_list([job_role] + keywords_any_n)

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

        kw_where, kw_params = _build_keywords_where_and_params(
            keywords_all_n, keywords_any_n, alias=None, param_offset=0
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

        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                cur.execute(sql, params)
                row = cur.fetchone()
                cnt = int(row[0] if row and row[0] is not None else 0)
                return {
                    "count": cnt,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "keywords_all": keywords_all_n,
                    "keywords_any": keywords_any_n,
                    "job_role": job_role,
                    "min_salary_m만원": min_salary_m만원,
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
        keywords_all_n = _normalize_kw_list(keywords_all or [])
        keywords_any_n = _normalize_kw_list(keywords_any or [])

        if job_role:
            keywords_any_n = _normalize_kw_list([job_role] + keywords_any_n)

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

        kw_where, kw_params = _build_keywords_where_and_params(
            keywords_all_n, keywords_any_n, alias=None, param_offset=0
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

        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
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
                    "keywords_all": keywords_all_n,
                    "keywords_any": keywords_any_n,
                    "job_role": job_role,
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
            job_ids_scope: Optional[List[int]] = None,  # optional: restrict within prior list
    ) -> Dict[str, Any]:
        keywords_all_n = _normalize_kw_list(keywords_all or [])
        keywords_any_n = _normalize_kw_list(keywords_any or [])

        if job_role:
            keywords_any_n = _normalize_kw_list([job_role] + keywords_any_n)

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

        # optional scope (e.g., "그 중에 서울만")
        if job_ids_scope:
            where.append("jp.job_id = ANY(%(scope_ids)s::bigint[])")
            params["scope_ids"] = [int(x) for x in job_ids_scope]

        apply_location_filters(where, params, regions_any, admin_areas_any, location_col="jp.location")

        kw_where, kw_params = _build_keywords_where_and_params(
            keywords_all_n, keywords_any_n, alias="jp", param_offset=0
        )
        where.extend(kw_where)
        params.update(kw_params)

        if min_salary_m만원 is not None:
            where.append(_salary_min_predicate(int(min_salary_m만원), salary_col="jp.salary_text"))
            params["min_salary_m"] = int(min_salary_m만원)

        # deterministic random by request_id (stable within same request)
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

        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                cur.execute(sql, params)
                rows = cur.fetchall() or []
                items = []
                for r in rows:
                    items.append(
                        {
                            "job_id": int(r[0]),
                            "employer_name": r[1],
                            "title": r[2],
                            "location": r[3],
                            "salary_text": r[4],
                            "stack": r[5],
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
                    "keywords_all": keywords_all_n,
                    "keywords_any": keywords_any_n,
                    "job_role": job_role,
                    "min_salary_m만원": min_salary_m만원,
                    "limit": lim,
                    "random": bool(random),
                    "scoped": bool(job_ids_scope),
                    "scope_size": len(job_ids_scope or []),
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
            "deleted_at IS NULL",
            "status = 'OPEN'",
            "created_at >= %(start_ts)s",
            "created_at < %(end_ts)s",
            "stack IS NOT NULL",
            "length(trim(stack)) > 0",
        ]
        params: Dict[str, Any] = {
            "start_ts": f"{start_date} 00:00:00",
            "end_ts": f"{end_date} 00:00:00",
        }

        apply_location_filters(where, params, regions_any, admin_areas_any, location_col="location")

        # job_role을 사전 정의 없이 "posting set" 힌트로만 적용 (원문 그대로 OR 키워드로)
        if job_role:
            kw_where, kw_params = _build_keywords_where_and_params(
                keywords_all=[],
                keywords_any=_normalize_kw_list([job_role]),
                alias=None,
                param_offset=0,
            )
            where.extend(kw_where)
            params.update(kw_params)

        lim = max(1, min(50, int(limit)))
        params["lim"] = lim

        # stack split: comma / slash / pipe
        sql = f"""
            WITH tokens AS (
              SELECT
                lower(trim(tok)) AS tok
              FROM job_posting,
                   regexp_split_to_table(stack, '\\s*[,/|]\\s*') AS tok
              WHERE {" AND ".join(where)}
            )
            SELECT tok, COUNT(*)::bigint AS cnt
            FROM tokens
            WHERE tok <> ''
            GROUP BY tok
            ORDER BY cnt DESC, tok ASC
            LIMIT %(lim)s
        """

        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
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
                }
        finally:
            conn.close()


job_stats_repo = JobStatsRepository()
