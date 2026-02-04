# app/chatbot/repository_core/repo.py
from __future__ import annotations

import re
from datetime import date
from typing import Any, Dict, List, Optional, Tuple

from app.chatbot.utils.location import apply_location_filters
from app.core.database import get_db_connection

from .aliases import _expand_aliases_for_canon, _resolve_alias_token, _resolve_stack_token
from .filters import (
    _apply_competition_pct_filters,
    _apply_required_experience_filters,
    _competition_pct_expr,
    _salary_between_predicate,
    _salary_bounds_m_expr,
    _salary_min_predicate,
)
from .keywords import _build_keywords_where_and_params_v2
from .normalize import _SEP_RE, _canon_members_for_query, _normalize_kw_list, _normalize_token
from .schema import _column_exists

class JobStatsRepository:
    def posting_briefs_by_ids(self, job_ids: List[int]) -> List[Dict[str, Any]]:
        if not job_ids:
            return []

        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                sql = """
                    WITH ids AS (
                        SELECT *
                        FROM unnest(%(ids)s::bigint[]) WITH ORDINALITY AS t(job_id, ord)
                    )
                    SELECT
                        jp.job_id,
                        e.name AS employer_name,
                        jp.title
                    FROM ids i
                    JOIN job_posting jp ON jp.job_id = i.job_id
                    JOIN employer e ON e.employer_id = jp.employer_id
                    WHERE jp.deleted_at IS NULL
                    ORDER BY i.ord \
                """
                cur.execute(sql, {"ids": [int(x) for x in job_ids]})
                rows = cur.fetchall() or []
                return [
                    {"job_id": int(r[0]), "employer_name": r[1], "title": r[2]}
                    for r in rows
                ]
        finally:
            conn.close()


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

        canon_to_aliases = _expand_aliases_for_canon(cur, expanded_pool, kind="STACK")

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

    def _resolve_and_expand_industries(
            self,
            cur,
            industries_any: List[str],
            *,
            trigram_threshold: float = 0.42,
            max_items: int = 10,
    ) -> Tuple[List[List[str]], List[str], List[dict]]:
        """
        industries_any(raw) -> (industry_groups_for_OR, used_canon_list, corrections)
        """
        inds = _normalize_kw_list(industries_any or [], max_items=max_items)

        corrections: List[dict] = []
        requested: List[str] = []
        for raw in inds:
            canon, corr = _resolve_alias_token(cur, raw, kind="INDUSTRY", trigram_threshold=trigram_threshold)
            if canon:
                requested.append(canon)
            if corr:
                corrections.append(corr)

        expanded_pool: List[str] = []
        for c in dict.fromkeys(requested):
            for m in _canon_members_for_query(c):
                if m not in expanded_pool:
                    expanded_pool.append(m)

        canon_to_aliases = _expand_aliases_for_canon(cur, expanded_pool, kind="INDUSTRY")

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

        groups = [build_variant_group(c) for c in requested]
        return groups, requested, corrections

    def count_postings(
            self,
            start_date: date,
            end_date: date,
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            keywords_all: Optional[List[str]] = None,
            keywords_any: Optional[List[str]] = None,
            industries_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
            min_salary_m만원: Optional[int] = None,
            max_salary_m만원: Optional[int] = None,
            min_competition_pct: Optional[float] = None,
            max_competition_pct: Optional[float] = None,
            min_required_experience_years: Optional[int] = None,
            max_required_experience_years: Optional[int] = None,
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
                ind_groups, ind_used, ind_corr = self._resolve_and_expand_industries(cur, industries_any or [])
                any_groups2 = any_groups + ind_groups
                corr_all = (stack_corr + ind_corr)[:50]

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

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups2, alias="jp", param_offset=0)
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

                _apply_required_experience_filters(
                    where, params, alias="jp",
                    min_required_experience_years=min_required_experience_years,
                    max_required_experience_years=max_required_experience_years,
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
                    "industries_any": ind_used,
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "keywords_all": [g[0] for g in all_groups] if all_groups else [],
                    "keywords_any": [g[0] for g in any_groups] if any_groups else [],
                    "job_role": job_role,
                    "min_salary_m만원": min_salary_m만원,
                    "max_salary_m만원": max_salary_m만원,
                    "min_competition_pct": min_competition_pct,
                    "max_competition_pct": max_competition_pct,
                    "min_required_experience_years": min_required_experience_years,
                    "max_required_experience_years": max_required_experience_years,
                    "scoped": bool(job_ids_scope),
                    "scope_size": len(job_ids_scope or []),
                    "stack_corrections": stack_corr,
                    "term_corrections": corr_all,
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
            industries_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
            min_competition_pct: Optional[float] = None,
            max_competition_pct: Optional[float] = None,
            min_required_experience_years: Optional[int] = None,
            max_required_experience_years: Optional[int] = None,
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
                ind_groups, ind_used, ind_corr = self._resolve_and_expand_industries(cur, industries_any or [])
                any_groups2 = any_groups + ind_groups
                corr_all = (stack_corr + ind_corr)[:50]

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

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups2, alias="jp", param_offset=0)
                where.extend(kw_where)
                params.update(kw_params)

                _apply_competition_pct_filters(
                    where, params, alias="jp",
                    min_competition_pct=min_competition_pct,
                    max_competition_pct=max_competition_pct,
                    require_capacity=False,
                )

                _apply_required_experience_filters(
                    where, params, alias="jp",
                    min_required_experience_years=min_required_experience_years,
                    max_required_experience_years=max_required_experience_years,
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
                    "industries_any": ind_used,
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "keywords_all": [g[0] for g in all_groups] if all_groups else [],
                    "keywords_any": [g[0] for g in any_groups] if any_groups else [],
                    "job_role": job_role,
                    "min_competition_pct": min_competition_pct,
                    "max_competition_pct": max_competition_pct,
                    "min_required_experience_years": min_required_experience_years,
                    "max_required_experience_years": max_required_experience_years,
                    "scoped": bool(job_ids_scope),
                    "scope_size": len(job_ids_scope or []),
                    "stack_corrections": stack_corr,
                    "term_corrections": corr_all,
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
            industries_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
            min_salary_m만원: Optional[int] = None,
            max_salary_m만원: Optional[int] = None,
            min_competition_pct: Optional[float] = None,
            max_competition_pct: Optional[float] = None,
            min_required_experience_years: Optional[int] = None,
            max_required_experience_years: Optional[int] = None,
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
                ind_groups, ind_used, ind_corr = self._resolve_and_expand_industries(cur, industries_any or [])
                any_groups2 = any_groups + ind_groups
                corr_all = (stack_corr + ind_corr)[:50]

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

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups2, alias="jp", param_offset=0)
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
                    require_capacity=True,
                )

                _apply_required_experience_filters(
                    where, params, alias="jp",
                    min_required_experience_years=min_required_experience_years,
                    max_required_experience_years=max_required_experience_years,
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
                    "industries_any": ind_used,
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "keywords_all": [g[0] for g in all_groups] if all_groups else [],
                    "keywords_any": [g[0] for g in any_groups] if any_groups else [],
                    "job_role": job_role,
                    "min_salary_m만원": min_salary_m만원,
                    "max_salary_m만원": max_salary_m만원,
                    "min_competition_pct": min_competition_pct,
                    "max_competition_pct": max_competition_pct,
                    "min_required_experience_years": min_required_experience_years,
                    "max_required_experience_years": max_required_experience_years,
                    "scoped": bool(job_ids_scope),
                    "scope_size": len(job_ids_scope or []),
                    "stack_corrections": stack_corr,
                    "term_corrections": corr_all,
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
            industries_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
            min_salary_m만원: Optional[int] = None,
            max_salary_m만원: Optional[int] = None,
            min_competition_pct: Optional[float] = None,
            max_competition_pct: Optional[float] = None,
            min_required_experience_years: Optional[int] = None,
            max_required_experience_years: Optional[int] = None,
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
                ind_groups, ind_used, ind_corr = self._resolve_and_expand_industries(cur, industries_any or [])
                any_groups2 = any_groups + ind_groups
                corr_all = (stack_corr + ind_corr)[:50]

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

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups2, alias="jp", param_offset=0)
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

                _apply_required_experience_filters(
                    where, params, alias="jp",
                    min_required_experience_years=min_required_experience_years,
                    max_required_experience_years=max_required_experience_years,
                )

                if random:
                    order_sql = "md5(jp.job_id::text || %(seed)s)"
                    params["seed"] = request_id
                else:
                    order_sql = "jp.created_at DESC"

                lim = max(1, min(20, int(limit)))
                params["lim"] = lim

                has_exp_col = _column_exists("job_posting", "required_experience")
                exp_select = ", jp.required_experience" if has_exp_col else ""

                sql = f"""
                    SELECT
                      jp.job_id,
                      e.name AS employer_name,
                      jp.title,
                      jp.location,
                      jp.salary_text,
                      jp.stack,
                      jp.apply_count,
                      jp.recruitment_capacity
                      {exp_select},
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
                    req_exp = None
                    created_at_idx = 8
                    if has_exp_col:
                        req_exp = r[8]
                        created_at_idx = 9

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

                    created_at = r[created_at_idx]

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
                            "required_experience": (int(req_exp) if req_exp is not None else None),
                            "created_at": (created_at.isoformat() if created_at else None),
                        }
                    )

                return {
                    "items": items,
                    "count": len(items),
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "industries_any": ind_used,
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "keywords_all": [g[0] for g in all_groups] if all_groups else [],
                    "keywords_any": [g[0] for g in any_groups] if any_groups else [],
                    "job_role": job_role,
                    "min_salary_m만원": min_salary_m만원,
                    "max_salary_m만원": max_salary_m만원,
                    "min_competition_pct": min_competition_pct,
                    "max_competition_pct": max_competition_pct,
                    "min_required_experience_years": min_required_experience_years,
                    "max_required_experience_years": max_required_experience_years,
                    "limit": lim,
                    "random": bool(random),
                    "scoped": bool(job_ids_scope),
                    "scope_size": len(job_ids_scope or []),
                    "stack_corrections": stack_corr,
                    "term_corrections": corr_all,
                }
        finally:
            conn.close()

    def salary_extreme_postings(
            self,
            start_date: date,
            end_date: date,
            *,
            order: str = "DESC",
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            keywords_all: Optional[List[str]] = None,
            keywords_any: Optional[List[str]] = None,
            industries_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
            min_salary_m만원: Optional[int] = None,
            max_salary_m만원: Optional[int] = None,
            min_competition_pct: Optional[float] = None,
            max_competition_pct: Optional[float] = None,
            min_required_experience_years: Optional[int] = None,
            max_required_experience_years: Optional[int] = None,
            limit: int = 5,
            job_ids_scope: Optional[List[int]] = None,
    ) -> Dict[str, Any]:
        """연봉(만원) 기준 극값 공고 TOP N.

        - salary_text를 파싱해 상한값(hi)을 정렬 기준으로 사용한다.
        - salary_text가 '면접 후 결정' 등 파싱 불가면 제외한다.
        """
        od = "ASC" if str(order).upper() == "ASC" else "DESC"

        kw_all = keywords_all or []
        kw_any = list(keywords_any or [])
        if job_role and job_role not in kw_any:
            kw_any.append(job_role)

        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                all_groups, any_groups, stack_corr = self._resolve_and_expand_keywords(cur, kw_all, kw_any)
                ind_groups, ind_used, ind_corr = self._resolve_and_expand_industries(cur, industries_any or [])
                any_groups2 = any_groups + ind_groups

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

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups2, alias="jp", param_offset=0)
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

                _apply_required_experience_filters(
                    where, params, alias="jp",
                    min_required_experience_years=min_required_experience_years,
                    max_required_experience_years=max_required_experience_years,
                )

                # 연봉 파싱 가능한 것만 대상
                _lo_m, _hi_m = _salary_bounds_m_expr("jp.salary_text")
                where.append(f"({_hi_m}) IS NOT NULL")

                lim = max(1, min(20, int(limit or 5)))
                params["lim"] = lim

                has_exp_col = _column_exists("job_posting", "required_experience")
                exp_select = ", jp.required_experience" if has_exp_col else ""

                sql = f"""
                    SELECT
                      jp.job_id,
                      e.name AS employer_name,
                      jp.title,
                      jp.location,
                      jp.salary_text,
                      jp.stack,
                      jp.apply_count,
                      jp.recruitment_capacity
                      {exp_select},
                      ({_hi_m}) AS salary_hi_m,
                      jp.created_at
                    FROM job_posting jp
                    JOIN employer e ON e.employer_id = jp.employer_id
                    WHERE {" AND ".join(where)}
                    ORDER BY salary_hi_m {od} NULLS LAST, jp.created_at DESC, jp.job_id DESC
                    LIMIT %(lim)s
                """
                cur.execute(sql, params)
                rows = cur.fetchall() or []

                items = []
                for r in rows:
                    # base indexes: 0..7 always, optional exp at 8, salary_hi, created_at last
                    if has_exp_col:
                        req_exp = r[8]
                        salary_hi_idx = 9
                        created_idx = 10
                    else:
                        req_exp = None
                        salary_hi_idx = 8
                        created_idx = 9

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

                    created_at = r[created_idx]

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
                            "required_experience": (int(req_exp) if req_exp is not None else None),
                            "salary_hi_m만원": (int(r[salary_hi_idx]) if r[salary_hi_idx] is not None else None),
                            "created_at": (created_at.isoformat() if created_at else None),
                        }
                    )

                return {
                    "items": items,
                    "count": len(items),
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "industries_any": ind_used,
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "job_role": job_role,
                    "limit": lim,
                    "order": od,
                    "scoped": bool(job_ids_scope),
                    "scope_size": len(job_ids_scope or []),
                    "stack_corrections": stack_corr,
                    "term_corrections": (ind_corr[:50]),
                }
        finally:
            conn.close()

    def top_stacks(
            self,
            start_date: date,
            end_date: date,
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            industries_any: Optional[List[str]] = None,
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
                ind_groups, ind_used, _ = self._resolve_and_expand_industries(cur, industries_any or [])
                any_groups2 = any_groups + ind_groups

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups2, alias="jp", param_offset=0)
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
                    "industries_any": ind_used,
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "job_role": job_role,
                    "limit": lim,
                    "stack_corrections": stack_corr,
                }
        finally:
            conn.close()

    def bottom_stacks(
            self,
            start_date: date,
            end_date: date,
            *,
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            keywords_all: Optional[List[str]] = None,
            keywords_any: Optional[List[str]] = None,
            industries_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
            min_salary_m만원: Optional[int] = None,
            max_salary_m만원: Optional[int] = None,
            min_competition_pct: Optional[float] = None,
            max_competition_pct: Optional[float] = None,
            min_required_experience_years: Optional[int] = None,
            max_required_experience_years: Optional[int] = None,
            limit: int = 10,
            job_ids_scope: Optional[List[int]] = None,
    ) -> Dict[str, Any]:
        """TOP_STACKS의 반대(가장 적게 등장한 스택)"""
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                kw_all = keywords_all or []
                kw_any = list(keywords_any or [])
                if job_role and job_role not in kw_any:
                    kw_any.append(job_role)

                all_groups, any_groups, stack_corr = self._resolve_and_expand_keywords(cur, kw_all, kw_any)
                ind_groups, ind_used, ind_corr = self._resolve_and_expand_industries(cur, industries_any or [])
                any_groups2 = any_groups + ind_groups

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

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups2, alias="jp", param_offset=0)
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

                _apply_required_experience_filters(
                    where, params, alias="jp",
                    min_required_experience_years=min_required_experience_years,
                    max_required_experience_years=max_required_experience_years,
                )

                lim = int(limit or 10)
                params["lim"] = lim

                sql = f"""
                    WITH tokens AS (
                      SELECT lower(btrim(tok)) AS tok
                      FROM job_posting jp
                      CROSS JOIN LATERAL unnest(coalesce(jp.stack, ARRAY[]::text[])) AS st(item)
                      CROSS JOIN LATERAL regexp_split_to_table(coalesce(st.item, ''), '\\s*[,/|]+\\s*') AS tok
                      WHERE {" AND ".join(where)}
                    )
                    SELECT tok, COUNT(*)::bigint AS cnt
                    FROM tokens
                    WHERE tok <> ''
                    GROUP BY tok
                    ORDER BY cnt ASC, tok ASC
                    LIMIT %(lim)s
                """
                cur.execute(sql, params)
                rows = cur.fetchall() or []
                items = [{"stack": r[0], "count": int(r[1])} for r in rows]

                return {
                    "items": items,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "industries_any": ind_used,
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "job_role": job_role,
                    "limit": lim,
                    "stack_corrections": stack_corr,
                }
        finally:
            conn.close()

    def busiest_week(
            self,
            start_date: date,
            end_date: date,
            *,
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            keywords_all: Optional[List[str]] = None,
            keywords_any: Optional[List[str]] = None,
            industries_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
            min_salary_m만원: Optional[int] = None,
            max_salary_m만원: Optional[int] = None,
            min_competition_pct: Optional[float] = None,
            max_competition_pct: Optional[float] = None,
            min_required_experience_years: Optional[int] = None,
            max_required_experience_years: Optional[int] = None,
            job_ids_scope: Optional[List[int]] = None,
    ) -> Dict[str, Any]:
        """KST 기준 "월-주차"(1~7일=첫째주...)로 묶어 가장 공고가 많은 주간을 반환."""
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                kw_all = keywords_all or []
                kw_any = list(keywords_any or [])
                if job_role and job_role not in kw_any:
                    kw_any.append(job_role)

                all_groups, any_groups, stack_corr = self._resolve_and_expand_keywords(cur, kw_all, kw_any)
                ind_groups, ind_used, ind_corr = self._resolve_and_expand_industries(cur, industries_any or [])
                any_groups2 = any_groups + ind_groups

                where = [
                    "jp.deleted_at IS NULL",
                    "jp.status = 'OPEN'",
                    # ✅ KST 기준 날짜 필터
                    "(jp.created_at AT TIME ZONE 'Asia/Seoul')::date >= %(start_d)s::date",
                    "(jp.created_at AT TIME ZONE 'Asia/Seoul')::date < %(end_d)s::date",
                ]
                params: Dict[str, Any] = {
                    "start_d": str(start_date),
                    "end_d": str(end_date),
                }

                if job_ids_scope:
                    where.append("jp.job_id = ANY(%(scope_ids)s::bigint[])")
                    params["scope_ids"] = [int(x) for x in job_ids_scope]

                apply_location_filters(where, params, regions_any, admin_areas_any, location_col="jp.location")

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups2, alias="jp", param_offset=0)
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

                _apply_required_experience_filters(
                    where, params, alias="jp",
                    min_required_experience_years=min_required_experience_years,
                    max_required_experience_years=max_required_experience_years,
                )

                sql = f"""
                    WITH base AS (
                        SELECT (jp.created_at AT TIME ZONE 'Asia/Seoul')::date AS kst_date
                        FROM job_posting jp
                        WHERE {" AND ".join(where)}
                    ), buck AS (
                        SELECT
                            date_trunc('month', kst_date)::date AS month_start,
                            ((extract(day from kst_date)::int - 1) / 7) + 1 AS week_idx
                        FROM base
                    )
                    SELECT
                        month_start,
                        week_idx,
                        COUNT(*)::bigint AS cnt,
                        (month_start + ((week_idx - 1) * 7) * interval '1 day')::date AS week_start,
                        LEAST(
                            (month_start + (week_idx * 7) * interval '1 day')::date,
                            (month_start + interval '1 month')::date
                        ) AS week_end
                    FROM buck
                    GROUP BY month_start, week_idx
                    ORDER BY cnt DESC, month_start DESC, week_idx DESC
                    LIMIT 1
                """
                cur.execute(sql, params)
                row = cur.fetchone()

                if not row:
                    return {
                        "start_date": str(start_date),
                        "end_date": str(end_date),
                        "busiest_week_label": "-",
                        "busiest_week_count": 0,
                        "busiest_week_start_date": None,
                        "busiest_week_end_date": None,
                        "industries_any": ind_used,
                        "regions_any": regions_any or [],
                        "admin_areas_any": admin_areas_any or [],
                        "job_role": job_role,
                        "stack_corrections": stack_corr,
                    }

                month_start, week_idx, cnt, week_start, week_end = row
                week_idx = int(week_idx)
                month_num = int(getattr(month_start, "month", 0) or 0)
                wk_map = {1: "첫째주", 2: "둘째주", 3: "셋째주", 4: "넷째주", 5: "다섯째주"}
                label = f"{month_num}월 {wk_map.get(week_idx, f'{week_idx}주차')}"

                return {
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "busiest_week_label": label,
                    "busiest_week_count": int(cnt or 0),
                    "busiest_week_start_date": str(week_start),
                    "busiest_week_end_date": str(week_end),
                    "industries_any": ind_used,
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "job_role": job_role,
                    "stack_corrections": stack_corr,
                }
        finally:
            conn.close()

    def busiest_day(
            self,
            start_date: date,
            end_date: date,
            *,
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            keywords_all: Optional[List[str]] = None,
            keywords_any: Optional[List[str]] = None,
            industries_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
            min_salary_m만원: Optional[int] = None,
            max_salary_m만원: Optional[int] = None,
            min_competition_pct: Optional[float] = None,
            max_competition_pct: Optional[float] = None,
            min_required_experience_years: Optional[int] = None,
            max_required_experience_years: Optional[int] = None,
            job_ids_scope: Optional[List[int]] = None,
    ) -> Dict[str, Any]:
        """KST 기준 '날짜'로 묶어 가장 공고가 많은 날을 반환."""
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                kw_all = keywords_all or []
                kw_any = list(keywords_any or [])
                if job_role and job_role not in kw_any:
                    kw_any.append(job_role)

                all_groups, any_groups, stack_corr = self._resolve_and_expand_keywords(cur, kw_all, kw_any)
                ind_groups, ind_used, ind_corr = self._resolve_and_expand_industries(cur, industries_any or [])
                any_groups2 = any_groups + ind_groups

                where = [
                    "jp.deleted_at IS NULL",
                    "jp.status = 'OPEN'",
                    "(jp.created_at AT TIME ZONE 'Asia/Seoul')::date >= %(start_d)s::date",
                    "(jp.created_at AT TIME ZONE 'Asia/Seoul')::date < %(end_d)s::date",
                ]
                params: Dict[str, Any] = {
                    "start_d": str(start_date),
                    "end_d": str(end_date),
                }

                if job_ids_scope:
                    where.append("jp.job_id = ANY(%(scope_ids)s::bigint[])")
                    params["scope_ids"] = [int(x) for x in job_ids_scope]

                apply_location_filters(where, params, regions_any, admin_areas_any, location_col="jp.location")

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups2, alias="jp", param_offset=0)
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

                _apply_required_experience_filters(
                    where, params, alias="jp",
                    min_required_experience_years=min_required_experience_years,
                    max_required_experience_years=max_required_experience_years,
                )

                sql = f"""
                    WITH base AS (
                        SELECT (jp.created_at AT TIME ZONE 'Asia/Seoul')::date AS kst_date
                        FROM job_posting jp
                        WHERE {" AND ".join(where)}
                    )
                    SELECT kst_date, COUNT(*)::bigint AS cnt
                    FROM base
                    GROUP BY kst_date
                    ORDER BY cnt DESC, kst_date DESC
                    LIMIT 1
                """
                cur.execute(sql, params)
                row = cur.fetchone()

                if not row:
                    return {
                        "start_date": str(start_date),
                        "end_date": str(end_date),
                        "busiest_day": None,
                        "busiest_day_count": 0,
                        "industries_any": ind_used,
                        "regions_any": regions_any or [],
                        "admin_areas_any": admin_areas_any or [],
                        "job_role": job_role,
                        "stack_corrections": stack_corr,
                    }

                kst_date, cnt = row
                return {
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "busiest_day": str(kst_date),
                    "busiest_day_count": int(cnt or 0),
                    "industries_any": ind_used,
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "job_role": job_role,
                    "stack_corrections": stack_corr,
                }
        finally:
            conn.close()

    def competition_extreme_postings(
            self,
            start_date: date,
            end_date: date,
            *,
            order: str = "ASC",
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            keywords_all: Optional[List[str]] = None,
            keywords_any: Optional[List[str]] = None,
            industries_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
            min_salary_m만원: Optional[int] = None,
            max_salary_m만원: Optional[int] = None,
            min_competition_pct: Optional[float] = None,
            max_competition_pct: Optional[float] = None,
            min_required_experience_years: Optional[int] = None,
            max_required_experience_years: Optional[int] = None,
            limit: int = 5,
            job_ids_scope: Optional[List[int]] = None,
    ) -> Dict[str, Any]:
        """경쟁률(지원/모집) 극값 공고 TOP N."""
        od = "ASC" if str(order).upper() == "ASC" else "DESC"
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                kw_all = keywords_all or []
                kw_any = list(keywords_any or [])
                if job_role and job_role not in kw_any:
                    kw_any.append(job_role)

                all_groups, any_groups, stack_corr = self._resolve_and_expand_keywords(cur, kw_all, kw_any)
                ind_groups, ind_used, ind_corr = self._resolve_and_expand_industries(cur, industries_any or [])
                any_groups2 = any_groups + ind_groups

                where = [
                    "jp.deleted_at IS NULL",
                    "jp.status = 'OPEN'",
                    "jp.created_at >= %(start_ts)s",
                    "jp.created_at < %(end_ts)s",
                    "jp.recruitment_capacity > 0",
                ]
                params: Dict[str, Any] = {
                    "start_ts": f"{start_date} 00:00:00",
                    "end_ts": f"{end_date} 00:00:00",
                }

                if job_ids_scope:
                    where.append("jp.job_id = ANY(%(scope_ids)s::bigint[])")
                    params["scope_ids"] = [int(x) for x in job_ids_scope]

                apply_location_filters(where, params, regions_any, admin_areas_any, location_col="jp.location")

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups2, alias="jp", param_offset=0)
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
                    require_capacity=True,
                )

                _apply_required_experience_filters(
                    where, params, alias="jp",
                    min_required_experience_years=min_required_experience_years,
                    max_required_experience_years=max_required_experience_years,
                )

                lim = int(limit or 5)
                params["lim"] = lim

                comp_expr = _competition_pct_expr("jp")
                order_sql = f"{comp_expr} {od}, jp.created_at DESC"

                # schema-safe: employer_name / required_experience
                has_exp_col = _column_exists("job_posting", "required_experience")
                exp_select = ", jp.required_experience" if has_exp_col else ""

                sql = f"""
                    SELECT
                        jp.job_id,
                        e.name AS employer_name,
                        jp.title,
                        jp.location,
                        jp.salary_text,
                        {('jp.required_experience,' if has_exp_col else '')}
                        coalesce(jp.apply_count, 0)::bigint AS apply_count,
                        coalesce(jp.recruitment_capacity, 0)::bigint AS recruitment_capacity,
                        {comp_expr} AS competition_pct,
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
                    if has_exp_col:
                        req_exp = r[5]
                        apply_idx = 6
                        cap_idx = 7
                        pct_idx = 8
                        created_idx = 9
                    else:
                        req_exp = None
                        apply_idx = 5
                        cap_idx = 6
                        pct_idx = 7
                        created_idx = 8
                    items.append(
                        {
                            "job_id": int(r[0]),
                            "employer_name": r[1],
                            "title": r[2],
                            "location": r[3],
                            "salary_text": r[4],
                            "required_experience": (int(req_exp) if req_exp is not None else None),
                            "apply_count": int(r[apply_idx] or 0),
                            "recruitment_capacity": int(r[cap_idx] or 0),
                            "competition_pct": float(r[pct_idx]) if r[pct_idx] is not None else None,
                            "created_at": r[created_idx].isoformat() if r[created_idx] else None,
                        }
                    )

                return {
                    "items": items,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "industries_any": ind_used,
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "job_role": job_role,
                    "limit": lim,
                    "order": od,
                    "stack_corrections": stack_corr,
                }
        finally:
            conn.close()

    def stack_apply_rate_extremes(
            self,
            start_date: date,
            end_date: date,
            *,
            order: str = "ASC",
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            keywords_all: Optional[List[str]] = None,
            keywords_any: Optional[List[str]] = None,
            industries_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
            min_salary_m만원: Optional[int] = None,
            max_salary_m만원: Optional[int] = None,
            min_required_experience_years: Optional[int] = None,
            max_required_experience_years: Optional[int] = None,
            limit: int = 5,
            job_ids_scope: Optional[List[int]] = None,
    ) -> Dict[str, Any]:
        """스택별 지원률(총지원/총모집) 극값 TOP N."""
        od = "ASC" if str(order).upper() == "ASC" else "DESC"
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                kw_all = keywords_all or []
                kw_any = list(keywords_any or [])
                if job_role and job_role not in kw_any:
                    kw_any.append(job_role)

                all_groups, any_groups, stack_corr = self._resolve_and_expand_keywords(cur, kw_all, kw_any)
                ind_groups, ind_used, ind_corr = self._resolve_and_expand_industries(cur, industries_any or [])
                any_groups2 = any_groups + ind_groups

                where = [
                    "jp.deleted_at IS NULL",
                    "jp.status = 'OPEN'",
                    "jp.created_at >= %(start_ts)s",
                    "jp.created_at < %(end_ts)s",
                    "jp.recruitment_capacity > 0",
                ]
                params: Dict[str, Any] = {
                    "start_ts": f"{start_date} 00:00:00",
                    "end_ts": f"{end_date} 00:00:00",
                }

                if job_ids_scope:
                    where.append("jp.job_id = ANY(%(scope_ids)s::bigint[])")
                    params["scope_ids"] = [int(x) for x in job_ids_scope]

                apply_location_filters(where, params, regions_any, admin_areas_any, location_col="jp.location")

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups2, alias="jp", param_offset=0)
                where.extend(kw_where)
                params.update(kw_params)

                if min_salary_m만원 is not None and max_salary_m만원 is not None:
                    where.append(_salary_between_predicate(int(min_salary_m만원), int(max_salary_m만원), salary_col="jp.salary_text"))
                    params["min_salary_m"] = int(min_salary_m만원)
                    params["max_salary_m"] = int(max_salary_m만원)
                elif min_salary_m만원 is not None:
                    where.append(_salary_min_predicate(int(min_salary_m만원), salary_col="jp.salary_text"))
                    params["min_salary_m"] = int(min_salary_m만원)

                _apply_required_experience_filters(
                    where, params, alias="jp",
                    min_required_experience_years=min_required_experience_years,
                    max_required_experience_years=max_required_experience_years,
                )

                lim = int(limit or 5)
                params["lim"] = lim

                sql = f"""
                    WITH tokens AS (
                      SELECT
                        lower(btrim(tok)) AS tok,
                        coalesce(jp.apply_count, 0)::bigint AS apply_count,
                        coalesce(jp.recruitment_capacity, 0)::bigint AS recruitment_capacity
                      FROM job_posting jp
                      CROSS JOIN LATERAL unnest(coalesce(jp.stack, ARRAY[]::text[])) AS st(item)
                      CROSS JOIN LATERAL regexp_split_to_table(coalesce(st.item, ''), '\\s*[,/|]+\\s*') AS tok
                      WHERE {" AND ".join(where)}
                    )
                    SELECT
                        tok,
                        SUM(apply_count)::bigint AS apply_sum,
                        SUM(recruitment_capacity)::bigint AS cap_sum,
                        (SUM(apply_count)::numeric / NULLIF(SUM(recruitment_capacity), 0)::numeric) * 100 AS apply_rate_pct
                    FROM tokens
                    WHERE tok <> ''
                    GROUP BY tok
                    HAVING SUM(recruitment_capacity) > 0
                    ORDER BY apply_rate_pct {od}, cap_sum DESC, tok ASC
                    LIMIT %(lim)s
                """
                cur.execute(sql, params)
                rows = cur.fetchall() or []

                items = []
                for r in rows:
                    items.append(
                        {
                            "stack": r[0],
                            "apply_sum": int(r[1] or 0),
                            "cap_sum": int(r[2] or 0),
                            "apply_rate_pct": float(r[3]) if r[3] is not None else None,
                        }
                    )

                return {
                    "items": items,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "industries_any": ind_used,
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "job_role": job_role,
                    "limit": lim,
                    "order": od,
                    "stack_corrections": stack_corr,
                }
        finally:
            conn.close()

    def industry_extremes(
            self,
            start_date: date,
            end_date: date,
            *,
            order: str = "DESC",
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            limit: int = 5,
    ) -> Dict[str, Any]:
        """산업 컬럼 기준 공고수 극값(상위/하위) TOP N.

        ✅ 현재 스키마 기준: employer.industry(=산업분야)가 정식 컬럼인 경우가 많아
        job_posting의 industry_* 컬럼이 없어도 employer 조인으로 집계 가능하도록 처리한다.
        """
        od = "ASC" if str(order).upper() == "ASC" else "DESC"
        lim = int(limit or 5)

        # 1) Prefer employer.industry (current schema)
        if _column_exists("employer", "industry") and _column_exists("job_posting", "employer_id"):
            conn = get_db_connection()
            try:
                with conn.cursor() as cur:
                    where = [
                        "jp.deleted_at IS NULL",
                        "jp.status = 'OPEN'",
                        "jp.created_at >= %(start_ts)s",
                        "jp.created_at < %(end_ts)s",
                        "e.industry IS NOT NULL",
                        "btrim(e.industry) <> ''",
                    ]
                    params: Dict[str, Any] = {
                        "start_ts": f"{start_date} 00:00:00",
                        "end_ts": f"{end_date} 00:00:00",
                        "lim": lim,
                    }

                    apply_location_filters(where, params, regions_any, admin_areas_any, location_col="jp.location")

                    sql = f"""
                        SELECT
                            e.industry AS industry,
                            COUNT(*)::bigint AS cnt
                        FROM job_posting jp
                        JOIN employer e ON e.employer_id = jp.employer_id
                        WHERE {" AND ".join(where)}
                        GROUP BY e.industry
                        ORDER BY cnt {od}, industry ASC
                        LIMIT %(lim)s
                    """
                    cur.execute(sql, params)
                    rows = cur.fetchall() or []
                    items = [{"industry": r[0], "count": int(r[1] or 0)} for r in rows]

                    return {
                        "items": items,
                        "start_date": str(start_date),
                        "end_date": str(end_date),
                        "regions_any": regions_any or [],
                        "admin_areas_any": admin_areas_any or [],
                        "limit": lim,
                        "order": od,
                        "industry_source": "employer.industry",
                    }
            finally:
                conn.close()

        # 2) Fallback: legacy schemas where industry is stored on job_posting
        cand = ["industry", "industry_text", "industry_name", "industry_category", "industry_field"]
        col = None
        for c in cand:
            if _column_exists("job_posting", c):
                col = c
                break

        if not col:
            return {
                "start_date": str(start_date),
                "end_date": str(end_date),
                "items": [],
                "limit": lim,
                "reason": "employer 테이블에 industry 컬럼이 없고, job_posting에도 industry_* 컬럼이 없어 산업별 집계가 불가합니다.",
            }

        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                where = [
                    "jp.deleted_at IS NULL",
                    "jp.status = 'OPEN'",
                    "jp.created_at >= %(start_ts)s",
                    "jp.created_at < %(end_ts)s",
                    f"jp.{col} IS NOT NULL",
                    f"btrim(jp.{col}) <> ''",
                ]
                params: Dict[str, Any] = {
                    "start_ts": f"{start_date} 00:00:00",
                    "end_ts": f"{end_date} 00:00:00",
                    "lim": lim,
                }

                apply_location_filters(where, params, regions_any, admin_areas_any, location_col="jp.location")

                sql = f"""
                    SELECT
                        jp.{col} AS industry,
                        COUNT(*)::bigint AS cnt
                    FROM job_posting jp
                    WHERE {" AND ".join(where)}
                    GROUP BY jp.{col}
                    ORDER BY cnt {od}, industry ASC
                    LIMIT %(lim)s
                """
                cur.execute(sql, params)
                rows = cur.fetchall() or []

                items = [{"industry": r[0], "count": int(r[1] or 0)} for r in rows]

                return {
                    "items": items,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "limit": lim,
                    "order": od,
                    "industry_source": f"job_posting.{col}",
                }
        finally:
            conn.close()



    def most_applicants_posting(
            self,
            start_date: date,
            end_date: date,
            *,
            regions_any: Optional[List[str]] = None,
            admin_areas_any: Optional[List[str]] = None,
            keywords_all: Optional[List[str]] = None,
            keywords_any: Optional[List[str]] = None,
            industries_any: Optional[List[str]] = None,
            job_role: Optional[str] = None,
            min_salary_m만원: Optional[int] = None,
            max_salary_m만원: Optional[int] = None,
            min_competition_pct: Optional[float] = None,
            max_competition_pct: Optional[float] = None,
            min_required_experience_years: Optional[int] = None,
            max_required_experience_years: Optional[int] = None,
            job_ids_scope: Optional[List[int]] = None,
    ) -> Dict[str, Any]:
        """지원자 수(apply_count) 최다 공고 1개."""
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                kw_all = keywords_all or []
                kw_any = list(keywords_any or [])
                if job_role and job_role not in kw_any:
                    kw_any.append(job_role)

                all_groups, any_groups, stack_corr = self._resolve_and_expand_keywords(cur, kw_all, kw_any)
                ind_groups, ind_used, ind_corr = self._resolve_and_expand_industries(cur, industries_any or [])
                any_groups2 = any_groups + ind_groups

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

                kw_where, kw_params = _build_keywords_where_and_params_v2(all_groups, any_groups2, alias="jp", param_offset=0)
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
                )

                _apply_required_experience_filters(
                    where, params, alias="jp",
                    min_required_experience_years=min_required_experience_years,
                    max_required_experience_years=max_required_experience_years,
                )

                has_exp_col = _column_exists("job_posting", "required_experience")

                sql = f"""
                    SELECT
                        jp.job_id,
                        e.name AS employer_name,
                        jp.title,
                        jp.location,
                        jp.salary_text,
                        {('jp.required_experience,' if has_exp_col else '')}
                        coalesce(jp.apply_count, 0)::bigint AS apply_count,
                        jp.created_at
                    FROM job_posting jp
                    JOIN employer e ON e.employer_id = jp.employer_id
                    WHERE {" AND ".join(where)}
                    ORDER BY coalesce(jp.apply_count, 0) DESC, jp.created_at DESC
                    LIMIT 1
                """
                cur.execute(sql, params)
                row = cur.fetchone()

                items: List[Dict[str, Any]] = []
                if row:
                    if has_exp_col:
                        req_exp = row[5]
                        apply_idx = 6
                        created_idx = 7
                    else:
                        req_exp = None
                        apply_idx = 5
                        created_idx = 6

                    items.append(
                        {
                            "job_id": int(row[0]),
                            "employer_name": row[1],
                            "title": row[2],
                            "location": row[3],
                            "salary_text": row[4],
                            "required_experience": (int(req_exp) if req_exp is not None else None),
                            "apply_count": int(row[apply_idx] or 0),
                            "created_at": row[created_idx].isoformat() if row[created_idx] else None,
                        }
                    )

                return {
                    "items": items,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "industries_any": ind_used,
                    "regions_any": regions_any or [],
                    "admin_areas_any": admin_areas_any or [],
                    "keywords_all": [g[0] for g in all_groups] if all_groups else [],
                    "keywords_any": [g[0] for g in any_groups] if any_groups else [],
                    "job_role": job_role,
                    "min_salary_m만원": min_salary_m만원,
                    "max_salary_m만원": max_salary_m만원,
                    "min_competition_pct": min_competition_pct,
                    "max_competition_pct": max_competition_pct,
                    "min_required_experience_years": min_required_experience_years,
                    "max_required_experience_years": max_required_experience_years,
                    "scoped": bool(job_ids_scope),
                    "scope_size": len(job_ids_scope or []),
                    "stack_corrections": stack_corr,
                }
        finally:
            conn.close()

__all__ = ["JobStatsRepository"]
