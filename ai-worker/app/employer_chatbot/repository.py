# app/employer_chatbot/repository.py
from __future__ import annotations

import functools
import re
from datetime import date
from typing import Any, Dict, List, Optional

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
def _table_exists(table_name: str) -> bool:
    conn = get_db_connection()
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = %s
                LIMIT 1
                """,
                (table_name,),
            )
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
# Repository
# -----------------------------

class EmployerStatsRepository:
    """기업용 지원자/공고 통계 Repository"""

    def count_applications(
            self,
            employer_id: int,
            start_date: date,
            end_date: date,
            job_posting_ids: Optional[List[int]] = None,
            job_title_keywords: Optional[List[str]] = None,
            applicant_skills_any: Optional[List[str]] = None,
            applicant_skills_all: Optional[List[str]] = None,
            application_status_any: Optional[List[str]] = None,
            min_experience_years: Optional[int] = None,
            max_experience_years: Optional[int] = None,
            regions_any: Optional[List[str]] = None,
    ) -> Dict[str, Any]:
        """지원자 수 카운트"""
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                where = [
                    "jp.employer_id = %(employer_id)s",
                    "jp.deleted_at IS NULL",
                    "ja.created_at >= %(start_ts)s",
                    "ja.created_at < %(end_ts)s",
                ]
                params: Dict[str, Any] = {
                    "employer_id": employer_id,
                    "start_ts": f"{start_date} 00:00:00",
                    "end_ts": f"{end_date} 00:00:00",
                }

                # 특정 공고 필터
                if job_posting_ids:
                    where.append("jp.job_id = ANY(%(job_ids)s::bigint[])")
                    params["job_ids"] = [int(x) for x in job_posting_ids]

                # 공고 제목 키워드 필터
                if job_title_keywords:
                    kw_clauses = []
                    for i, kw in enumerate(job_title_keywords[:10]):
                        key = f"title_kw{i}"
                        kw_clauses.append(f"jp.title ILIKE %({key})s")
                        params[key] = f"%{kw}%"
                    if kw_clauses:
                        where.append("(" + " OR ".join(kw_clauses) + ")")

                # 지원 상태 필터
                if application_status_any:
                    where.append("ja.status = ANY(%(status_list)s::text[])")
                    params["status_list"] = [s.upper() for s in application_status_any]

                sql = f"""
                    SELECT COUNT(DISTINCT ja.application_id)::bigint AS cnt
                    FROM job_application ja
                    JOIN job_posting jp ON jp.job_id = ja.job_id
                    WHERE {" AND ".join(where)}
                """
                cur.execute(sql, params)
                row = cur.fetchone()
                cnt = int(row[0] if row and row[0] is not None else 0)

                return {
                    "count": cnt,
                    "employer_id": employer_id,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "job_posting_ids": job_posting_ids or [],
                    "job_title_keywords": job_title_keywords or [],
                    "application_status_any": application_status_any or [],
                }
        finally:
            conn.close()

    def list_applications(
            self,
            employer_id: int,
            start_date: date,
            end_date: date,
            request_id: str,
            job_posting_ids: Optional[List[int]] = None,
            job_title_keywords: Optional[List[str]] = None,
            applicant_skills_any: Optional[List[str]] = None,
            application_status_any: Optional[List[str]] = None,
            min_experience_years: Optional[int] = None,
            max_experience_years: Optional[int] = None,
            limit: int = 10,
            random: bool = False,
    ) -> Dict[str, Any]:
        """지원자 목록 조회"""
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                where = [
                    "jp.employer_id = %(employer_id)s",
                    "jp.deleted_at IS NULL",
                    "ja.created_at >= %(start_ts)s",
                    "ja.created_at < %(end_ts)s",
                ]
                params: Dict[str, Any] = {
                    "employer_id": employer_id,
                    "start_ts": f"{start_date} 00:00:00",
                    "end_ts": f"{end_date} 00:00:00",
                }

                if job_posting_ids:
                    where.append("jp.job_id = ANY(%(job_ids)s::bigint[])")
                    params["job_ids"] = [int(x) for x in job_posting_ids]

                if job_title_keywords:
                    kw_clauses = []
                    for i, kw in enumerate(job_title_keywords[:10]):
                        key = f"title_kw{i}"
                        kw_clauses.append(f"jp.title ILIKE %({key})s")
                        params[key] = f"%{kw}%"
                    if kw_clauses:
                        where.append("(" + " OR ".join(kw_clauses) + ")")

                if application_status_any:
                    where.append("ja.status = ANY(%(status_list)s::text[])")
                    params["status_list"] = [s.upper() for s in application_status_any]

                if random:
                    order_sql = "md5(ja.application_id::text || %(seed)s)"
                    params["seed"] = request_id
                else:
                    order_sql = "ja.created_at DESC"

                lim = max(1, min(50, int(limit)))
                params["lim"] = lim

                sql = f"""
                    SELECT
                        ja.application_id,
                        ja.user_id,
                        u.name AS applicant_name,
                        u.email AS applicant_email,
                        jp.job_id,
                        jp.title AS job_title,
                        ja.status,
                        ja.created_at
                    FROM job_application ja
                    JOIN job_posting jp ON jp.job_id = ja.job_id
                    LEFT JOIN "user" u ON u.user_id = ja.user_id
                    WHERE {" AND ".join(where)}
                    ORDER BY {order_sql}
                    LIMIT %(lim)s
                """
                cur.execute(sql, params)
                rows = cur.fetchall() or []

                items = []
                for r in rows:
                    items.append({
                        "application_id": int(r[0]),
                        "user_id": int(r[1]) if r[1] else None,
                        "applicant_name": r[2] or "-",
                        "applicant_email": r[3] or "-",
                        "job_id": int(r[4]),
                        "job_title": r[5],
                        "status": r[6],
                        "created_at": r[7].isoformat() if r[7] else None,
                    })

                return {
                    "items": items,
                    "count": len(items),
                    "employer_id": employer_id,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "job_posting_ids": job_posting_ids or [],
                    "job_title_keywords": job_title_keywords or [],
                    "application_status_any": application_status_any or [],
                    "limit": lim,
                    "random": bool(random),
                }
        finally:
            conn.close()

    def application_stats(
            self,
            employer_id: int,
            start_date: date,
            end_date: date,
            job_posting_ids: Optional[List[int]] = None,
            job_title_keywords: Optional[List[str]] = None,
    ) -> Dict[str, Any]:
        """지원 통계 (총 지원수, 상태별 분포 등)"""
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                where = [
                    "jp.employer_id = %(employer_id)s",
                    "jp.deleted_at IS NULL",
                    "ja.created_at >= %(start_ts)s",
                    "ja.created_at < %(end_ts)s",
                ]
                params: Dict[str, Any] = {
                    "employer_id": employer_id,
                    "start_ts": f"{start_date} 00:00:00",
                    "end_ts": f"{end_date} 00:00:00",
                }

                if job_posting_ids:
                    where.append("jp.job_id = ANY(%(job_ids)s::bigint[])")
                    params["job_ids"] = [int(x) for x in job_posting_ids]

                if job_title_keywords:
                    kw_clauses = []
                    for i, kw in enumerate(job_title_keywords[:10]):
                        key = f"title_kw{i}"
                        kw_clauses.append(f"jp.title ILIKE %({key})s")
                        params[key] = f"%{kw}%"
                    if kw_clauses:
                        where.append("(" + " OR ".join(kw_clauses) + ")")

                # 총 지원 수
                sql_total = f"""
                    SELECT COUNT(DISTINCT ja.application_id)::bigint AS total
                    FROM job_application ja
                    JOIN job_posting jp ON jp.job_id = ja.job_id
                    WHERE {" AND ".join(where)}
                """
                cur.execute(sql_total, params)
                total = int((cur.fetchone() or [0])[0] or 0)

                # 상태별 분포
                sql_status = f"""
                    SELECT ja.status, COUNT(*)::bigint AS cnt
                    FROM job_application ja
                    JOIN job_posting jp ON jp.job_id = ja.job_id
                    WHERE {" AND ".join(where)}
                    GROUP BY ja.status
                    ORDER BY cnt DESC
                """
                cur.execute(sql_status, params)
                status_rows = cur.fetchall() or []
                status_distribution = {r[0]: int(r[1]) for r in status_rows}

                # 공고별 지원 수 (상위 10개)
                sql_by_posting = f"""
                    SELECT jp.job_id, jp.title, COUNT(ja.application_id)::bigint AS cnt
                    FROM job_application ja
                    JOIN job_posting jp ON jp.job_id = ja.job_id
                    WHERE {" AND ".join(where)}
                    GROUP BY jp.job_id, jp.title
                    ORDER BY cnt DESC
                    LIMIT 10
                """
                cur.execute(sql_by_posting, params)
                posting_rows = cur.fetchall() or []
                by_posting = [
                    {"job_id": int(r[0]), "title": r[1], "count": int(r[2])}
                    for r in posting_rows
                ]

                return {
                    "total_applications": total,
                    "status_distribution": status_distribution,
                    "by_posting": by_posting,
                    "employer_id": employer_id,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "job_posting_ids": job_posting_ids or [],
                    "job_title_keywords": job_title_keywords or [],
                }
        finally:
            conn.close()

    def posting_performance(
            self,
            employer_id: int,
            start_date: date,
            end_date: date,
            job_posting_ids: Optional[List[int]] = None,
            limit: int = 10,
    ) -> Dict[str, Any]:
        """공고 성과 분석 (공고별 지원수, 조회수 등)"""
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                where = [
                    "jp.employer_id = %(employer_id)s",
                    "jp.deleted_at IS NULL",
                    "jp.created_at >= %(start_ts)s",
                    "jp.created_at < %(end_ts)s",
                ]
                params: Dict[str, Any] = {
                    "employer_id": employer_id,
                    "start_ts": f"{start_date} 00:00:00",
                    "end_ts": f"{end_date} 00:00:00",
                }

                if job_posting_ids:
                    where.append("jp.job_id = ANY(%(job_ids)s::bigint[])")
                    params["job_ids"] = [int(x) for x in job_posting_ids]

                lim = max(1, min(50, int(limit)))
                params["lim"] = lim

                sql = f"""
                    SELECT
                        jp.job_id,
                        jp.title,
                        jp.status,
                        jp.apply_count,
                        jp.recruitment_capacity,
                        jp.created_at,
                        COALESCE(
                            (SELECT COUNT(*) FROM job_application ja WHERE ja.job_id = jp.job_id),
                            0
                        )::bigint AS application_count
                    FROM job_posting jp
                    WHERE {" AND ".join(where)}
                    ORDER BY jp.apply_count DESC NULLS LAST, jp.created_at DESC
                    LIMIT %(lim)s
                """
                cur.execute(sql, params)
                rows = cur.fetchall() or []

                items = []
                for r in rows:
                    apply_count = int(r[3] or 0)
                    capacity = int(r[4] or 0)
                    competition_pct = (apply_count / capacity * 100.0) if capacity > 0 else None

                    items.append({
                        "job_id": int(r[0]),
                        "title": r[1],
                        "status": r[2],
                        "apply_count": apply_count,
                        "recruitment_capacity": capacity if r[4] is not None else None,
                        "competition_pct": competition_pct,
                        "application_count": int(r[6]),
                        "created_at": r[5].isoformat() if r[5] else None,
                    })

                return {
                    "items": items,
                    "count": len(items),
                    "employer_id": employer_id,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "job_posting_ids": job_posting_ids or [],
                    "limit": lim,
                }
        finally:
            conn.close()

    def top_skills(
            self,
            employer_id: int,
            start_date: date,
            end_date: date,
            job_posting_ids: Optional[List[int]] = None,
            limit: int = 10,
    ) -> Dict[str, Any]:
        """지원자 보유 스킬 상위 (이력서 기반)"""
        # 이력서 테이블 구조에 따라 조정 필요
        # 여기서는 기본 구조 제공
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                # resume 테이블에 skills 컬럼이 있다고 가정
                if not _column_exists("resume", "skills"):
                    return {
                        "items": [],
                        "employer_id": employer_id,
                        "start_date": str(start_date),
                        "end_date": str(end_date),
                        "reason": "resume.skills column not found",
                    }

                where = [
                    "jp.employer_id = %(employer_id)s",
                    "jp.deleted_at IS NULL",
                    "ja.created_at >= %(start_ts)s",
                    "ja.created_at < %(end_ts)s",
                    "r.skills IS NOT NULL",
                ]
                params: Dict[str, Any] = {
                    "employer_id": employer_id,
                    "start_ts": f"{start_date} 00:00:00",
                    "end_ts": f"{end_date} 00:00:00",
                }

                if job_posting_ids:
                    where.append("jp.job_id = ANY(%(job_ids)s::bigint[])")
                    params["job_ids"] = [int(x) for x in job_posting_ids]

                lim = max(1, min(50, int(limit)))
                params["lim"] = lim

                sql = f"""
                    WITH applicant_skills AS (
                        SELECT
                            lower(btrim(skill)) AS skill
                        FROM job_application ja
                        JOIN job_posting jp ON jp.job_id = ja.job_id
                        JOIN resume r ON r.user_id = ja.user_id
                        CROSS JOIN LATERAL unnest(coalesce(r.skills, ARRAY[]::text[])) AS skill
                        WHERE {" AND ".join(where)}
                    )
                    SELECT skill, COUNT(*)::bigint AS cnt
                    FROM applicant_skills
                    WHERE skill <> ''
                    GROUP BY skill
                    ORDER BY cnt DESC, skill ASC
                    LIMIT %(lim)s
                """
                cur.execute(sql, params)
                rows = cur.fetchall() or []
                items = [{"skill": r[0], "count": int(r[1])} for r in rows]

                return {
                    "items": items,
                    "employer_id": employer_id,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "job_posting_ids": job_posting_ids or [],
                    "limit": lim,
                }
        finally:
            conn.close()

    def applicant_trend(
            self,
            employer_id: int,
            start_date: date,
            end_date: date,
            time_unit: str = "day",
            job_posting_ids: Optional[List[int]] = None,
    ) -> Dict[str, Any]:
        """지원자 추이 분석 (일별/주별/월별)"""
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                where = [
                    "jp.employer_id = %(employer_id)s",
                    "jp.deleted_at IS NULL",
                    "ja.created_at >= %(start_ts)s",
                    "ja.created_at < %(end_ts)s",
                ]
                params: Dict[str, Any] = {
                    "employer_id": employer_id,
                    "start_ts": f"{start_date} 00:00:00",
                    "end_ts": f"{end_date} 00:00:00",
                }

                if job_posting_ids:
                    where.append("jp.job_id = ANY(%(job_ids)s::bigint[])")
                    params["job_ids"] = [int(x) for x in job_posting_ids]

                # 시간 단위별 그룹핑
                if time_unit == "week":
                    date_expr = "date_trunc('week', ja.created_at)::date"
                elif time_unit == "month":
                    date_expr = "date_trunc('month', ja.created_at)::date"
                else:  # day
                    date_expr = "ja.created_at::date"

                sql = f"""
                    SELECT {date_expr} AS period, COUNT(*)::bigint AS cnt
                    FROM job_application ja
                    JOIN job_posting jp ON jp.job_id = ja.job_id
                    WHERE {" AND ".join(where)}
                    GROUP BY period
                    ORDER BY period ASC
                """
                cur.execute(sql, params)
                rows = cur.fetchall() or []

                trend = [
                    {"period": r[0].isoformat() if r[0] else "-", "count": int(r[1])}
                    for r in rows
                ]

                return {
                    "trend": trend,
                    "time_unit": time_unit,
                    "employer_id": employer_id,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "job_posting_ids": job_posting_ids or [],
                }
        finally:
            conn.close()

    def applicant_profile(
            self,
            employer_id: int,
            start_date: date,
            end_date: date,
            job_posting_ids: Optional[List[int]] = None,
    ) -> Dict[str, Any]:
        """지원자 프로필 분석 (경력 분포 등)"""
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                where = [
                    "jp.employer_id = %(employer_id)s",
                    "jp.deleted_at IS NULL",
                    "ja.created_at >= %(start_ts)s",
                    "ja.created_at < %(end_ts)s",
                ]
                params: Dict[str, Any] = {
                    "employer_id": employer_id,
                    "start_ts": f"{start_date} 00:00:00",
                    "end_ts": f"{end_date} 00:00:00",
                }

                if job_posting_ids:
                    where.append("jp.job_id = ANY(%(job_ids)s::bigint[])")
                    params["job_ids"] = [int(x) for x in job_posting_ids]

                # 총 지원자 수
                sql_total = f"""
                    SELECT COUNT(DISTINCT ja.user_id)::bigint AS total
                    FROM job_application ja
                    JOIN job_posting jp ON jp.job_id = ja.job_id
                    WHERE {" AND ".join(where)}
                """
                cur.execute(sql_total, params)
                total = int((cur.fetchone() or [0])[0] or 0)

                # 경력 분포 (resume 테이블 연동 시)
                exp_distribution = {}
                avg_experience = None

                # resume 테이블에 career_years 컬럼이 있는 경우
                if _column_exists("resume", "career_years"):
                    sql_exp = f"""
                        SELECT
                            CASE 
                                WHEN r.career_years IS NULL THEN '미입력'
                                WHEN r.career_years = 0 THEN '신입'
                                WHEN r.career_years <= 3 THEN '1-3년'
                                WHEN r.career_years <= 5 THEN '4-5년'
                                WHEN r.career_years <= 10 THEN '6-10년'
                                ELSE '10년 이상'
                            END AS exp_range,
                            COUNT(*)::bigint AS cnt
                        FROM job_application ja
                        JOIN job_posting jp ON jp.job_id = ja.job_id
                        LEFT JOIN resume r ON r.user_id = ja.user_id
                        WHERE {" AND ".join(where)}
                        GROUP BY exp_range
                        ORDER BY cnt DESC
                    """
                    cur.execute(sql_exp, params)
                    exp_rows = cur.fetchall() or []
                    exp_distribution = {r[0]: int(r[1]) for r in exp_rows}

                    # 평균 경력
                    sql_avg = f"""
                        SELECT AVG(r.career_years)
                        FROM job_application ja
                        JOIN job_posting jp ON jp.job_id = ja.job_id
                        LEFT JOIN resume r ON r.user_id = ja.user_id
                        WHERE {" AND ".join(where)} AND r.career_years IS NOT NULL
                    """
                    cur.execute(sql_avg, params)
                    avg_row = cur.fetchone()
                    if avg_row and avg_row[0]:
                        avg_experience = float(avg_row[0])

                return {
                    "total": total,
                    "avg_experience": avg_experience,
                    "experience_distribution": exp_distribution,
                    "employer_id": employer_id,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "job_posting_ids": job_posting_ids or [],
                }
        finally:
            conn.close()

    def pending_applications(
            self,
            employer_id: int,
            start_date: date,
            end_date: date,
            limit: int = 10,
    ) -> Dict[str, Any]:
        """검토 대기 중인 지원자 목록"""
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                where = [
                    "jp.employer_id = %(employer_id)s",
                    "jp.deleted_at IS NULL",
                    "ja.created_at >= %(start_ts)s",
                    "ja.created_at < %(end_ts)s",
                    "ja.status = 'PENDING'",
                ]
                params: Dict[str, Any] = {
                    "employer_id": employer_id,
                    "start_ts": f"{start_date} 00:00:00",
                    "end_ts": f"{end_date} 00:00:00",
                }

                lim = max(1, min(50, int(limit)))
                params["lim"] = lim

                # 총 대기 수
                sql_count = f"""
                    SELECT COUNT(*)::bigint AS cnt
                    FROM job_application ja
                    JOIN job_posting jp ON jp.job_id = ja.job_id
                    WHERE {" AND ".join(where)}
                """
                cur.execute(sql_count, params)
                total_pending = int((cur.fetchone() or [0])[0] or 0)

                # 목록 (오래된 순)
                sql = f"""
                    SELECT
                        ja.application_id,
                        ja.user_id,
                        u.name AS applicant_name,
                        jp.job_id,
                        jp.title AS job_title,
                        ja.created_at,
                        EXTRACT(DAY FROM NOW() - ja.created_at)::int AS days_waiting
                    FROM job_application ja
                    JOIN job_posting jp ON jp.job_id = ja.job_id
                    LEFT JOIN "user" u ON u.user_id = ja.user_id
                    WHERE {" AND ".join(where)}
                    ORDER BY ja.created_at ASC
                    LIMIT %(lim)s
                """
                cur.execute(sql, params)
                rows = cur.fetchall() or []

                items = []
                for r in rows:
                    items.append({
                        "application_id": int(r[0]),
                        "user_id": int(r[1]) if r[1] else None,
                        "applicant_name": r[2] or "-",
                        "job_id": int(r[3]),
                        "job_title": r[4],
                        "created_at": r[5].isoformat() if r[5] else None,
                        "days_waiting": int(r[6]) if r[6] else 0,
                    })

                return {
                    "count": total_pending,
                    "items": items,
                    "employer_id": employer_id,
                    "start_date": str(start_date),
                    "end_date": str(end_date),
                    "limit": lim,
                }
        finally:
            conn.close()


employer_stats_repo = EmployerStatsRepository()
