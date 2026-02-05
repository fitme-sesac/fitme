# app/chatbot/repository.py
from __future__ import annotations

# NOTE:
# - This module is a thin facade that keeps backward-compatible imports.
# - The implementation is split into app.chatbot.repository_core.* for readability and maintenance.

from app.chatbot.repository_core.aliases import (
    _alias_supports_entity_type,
    _alias_supports_kind,
    _alias_table_for,
    _expand_aliases_for_canon,
    _industry_alias_tables_ready,
    _resolve_alias_token,
    _resolve_stack_token,
    _stack_alias_tables_ready,
    _stack_vocab_ready,
)
from app.chatbot.repository_core.filters import (
    _apply_competition_pct_filters,
    _apply_required_experience_filters,
    _competition_pct_expr,
    _salary_amount_m_expr,
    _salary_between_predicate,
    _salary_bounds_m_expr,
    _salary_min_predicate,
)
from app.chatbot.repository_core.keywords import (
    _build_keywords_where_and_params_v2,
    _keyword_group_predicate,
)
from app.chatbot.repository_core.normalize import (
    CANONICAL_GROUP_EXPAND,
    _SEP_RE,
    _SPACE_RE,
    _canon_members_for_query,
    _normalize_kw_list,
    _normalize_token,
)
from app.chatbot.repository_core.repo import JobStatsRepository
from app.chatbot.repository_core.schema import _column_exists, _has_pg_trgm, _relation_exists


job_stats_repo = JobStatsRepository()

__all__ = [
    # public repo handle
    "JobStatsRepository",
    "job_stats_repo",

    # legacy/internal helpers (kept for backward compatibility)
    "_column_exists",
    "_relation_exists",
    "_has_pg_trgm",
    "_SPACE_RE",
    "_SEP_RE",
    "CANONICAL_GROUP_EXPAND",
    "_normalize_token",
    "_normalize_kw_list",
    "_canon_members_for_query",
    "_stack_alias_tables_ready",
    "_stack_vocab_ready",
    "_industry_alias_tables_ready",
    "_alias_supports_entity_type",
    "_alias_supports_kind",
    "_alias_table_for",
    "_resolve_alias_token",
    "_expand_aliases_for_canon",
    "_resolve_stack_token",
    "_keyword_group_predicate",
    "_build_keywords_where_and_params_v2",
    "_salary_amount_m_expr",
    "_salary_bounds_m_expr",
    "_salary_min_predicate",
    "_salary_between_predicate",
    "_competition_pct_expr",
    "_apply_competition_pct_filters",
    "_apply_required_experience_filters",
]
