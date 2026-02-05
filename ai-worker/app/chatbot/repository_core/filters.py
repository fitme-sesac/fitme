# app/chatbot/repository_core/filters.py
from __future__ import annotations

from typing import Any, Dict, List, Optional, Tuple

from .schema import _column_exists

# -----------------------------
# Salary / competition / experience helpers
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

def _apply_required_experience_filters(
        where: List[str],
        params: Dict[str, Any],
        *,
        alias: str = "jp",
        min_required_experience_years: Optional[int] = None,
        max_required_experience_years: Optional[int] = None,
) -> None:
    if not _column_exists("job_posting", "required_experience"):
        return

    if min_required_experience_years is None and max_required_experience_years is None:
        return

    where.append(f"{alias}.required_experience IS NOT NULL")

    if min_required_experience_years is not None and max_required_experience_years is not None:
        where.append(f"{alias}.required_experience BETWEEN %(exp_min)s AND %(exp_max)s")
        params["exp_min"] = int(min_required_experience_years)
        params["exp_max"] = int(max_required_experience_years)
    elif min_required_experience_years is not None:
        where.append(f"{alias}.required_experience >= %(exp_min)s")
        params["exp_min"] = int(min_required_experience_years)
    elif max_required_experience_years is not None:
        where.append(f"{alias}.required_experience <= %(exp_max)s")
        params["exp_max"] = int(max_required_experience_years)

__all__ = [
    "_salary_amount_m_expr",
    "_salary_bounds_m_expr",
    "_salary_min_predicate",
    "_salary_between_predicate",
    "_competition_pct_expr",
    "_apply_competition_pct_filters",
    "_apply_required_experience_filters",
]
