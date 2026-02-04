from __future__ import annotations

import uuid
from typing import Any, Dict, Optional

from app.chatbot.schemas import ChatbotIntent, ChatbotParsedSpec
from app.chatbot.services.agent_core.types import ChatbotState


def _common_repo_kwargs(parsed: ChatbotParsedSpec, scope_ids: Optional[list[int]] = None) -> Dict[str, Any]:
    return {
        "regions_any": parsed.regions_any,
        "admin_areas_any": parsed.admin_areas_any,
        "keywords_all": parsed.keywords_all,
        "keywords_any": parsed.keywords_any,
        "industries_any": parsed.industries_any,
        "job_role": parsed.job_role,
        "min_salary_m만원": parsed.min_salary_m만원,
        "max_salary_m만원": parsed.max_salary_m만원,
        "min_competition_pct": parsed.min_competition_pct,
        "max_competition_pct": parsed.max_competition_pct,
        "min_required_experience_years": parsed.min_required_experience_years,
        "max_required_experience_years": parsed.max_required_experience_years,
        "job_ids_scope": scope_ids,
    }


async def execute_state(state: ChatbotState, *, repo) -> Dict[str, Any]:
    parsed: ChatbotParsedSpec = state["parsed"]
    rid = state.get("request_id") or str(uuid.uuid4())
    base: Dict[str, Any] = state.get("result") or {}

    scope_ids = state.get("scope_job_ids") or None

    if parsed.intent == ChatbotIntent.DETAIL_URLS:
        ids = state.get("last_item_ids") or []
        items = repo.posting_briefs_by_ids(ids)
        base.update({"items": items, "count": len(items)})
        return {"result": base}


    if parsed.intent == ChatbotIntent.COUNT_POSTINGS:
        r = repo.count_postings(parsed.start_date, parsed.end_date, **_common_repo_kwargs(parsed, scope_ids))
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.COMPETITION:
        r = repo.competition(parsed.start_date, parsed.end_date, **_common_repo_kwargs(parsed, scope_ids))
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.RATE_STATS:
        r = repo.rate_stats(parsed.start_date, parsed.end_date, **_common_repo_kwargs(parsed, scope_ids))
        base.update(r)
        return {"result": base}


    if parsed.intent == ChatbotIntent.TOP_SALARY_POSTINGS:
        r = repo.salary_extreme_postings(
            parsed.start_date,
            parsed.end_date,
            order="DESC",
            limit=int(parsed.limit or 5),
            **_common_repo_kwargs(parsed, scope_ids),
        )
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.BOTTOM_SALARY_POSTINGS:
        r = repo.salary_extreme_postings(
            parsed.start_date,
            parsed.end_date,
            order="ASC",
            limit=int(parsed.limit or 5),
            **_common_repo_kwargs(parsed, scope_ids),
        )
        base.update(r)
        return {"result": base}
    if parsed.intent == ChatbotIntent.LIST_POSTINGS:
        kwargs = _common_repo_kwargs(parsed, scope_ids)
        r = repo.list_postings(
            parsed.start_date,
            parsed.end_date,
            request_id=rid,
            limit=int(parsed.limit or 5),
            random=bool(parsed.random),
            **kwargs,
        )
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.TOP_STACKS:
        lim = int(parsed.limit or 10)
        r = repo.top_stacks(parsed.start_date, parsed.end_date, regions_any=parsed.regions_any, admin_areas_any=parsed.admin_areas_any,
                           industries_any=parsed.industries_any, job_role=parsed.job_role, limit=lim)
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.BOTTOM_STACKS:
        lim = int(parsed.limit or 10)
        r = repo.bottom_stacks(parsed.start_date, parsed.end_date, regions_any=parsed.regions_any, admin_areas_any=parsed.admin_areas_any,
                              industries_any=parsed.industries_any, job_role=parsed.job_role, limit=lim)
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.BUSIEST_WEEK:
        r = repo.busiest_week(parsed.start_date, parsed.end_date, **_common_repo_kwargs(parsed, scope_ids))
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.BUSIEST_DAY:
        r = repo.busiest_day(parsed.start_date, parsed.end_date, **_common_repo_kwargs(parsed, scope_ids))
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.LOW_COMPETITION_POSTINGS:
        r = repo.competition_extreme_postings(parsed.start_date, parsed.end_date, order="ASC", limit=int(parsed.limit or 5),
                                             **_common_repo_kwargs(parsed, scope_ids))
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.HIGH_COMPETITION_POSTINGS:
        r = repo.competition_extreme_postings(parsed.start_date, parsed.end_date, order="DESC", limit=int(parsed.limit or 5),
                                             **_common_repo_kwargs(parsed, scope_ids))
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.LOW_STACK_APPLY_RATE:
        r = repo.stack_apply_rate_extremes(
            parsed.start_date,
            parsed.end_date,
            order="ASC",
            regions_any=parsed.regions_any,
            admin_areas_any=parsed.admin_areas_any,
            industries_any=parsed.industries_any,
            job_role=parsed.job_role,
            limit=int(parsed.limit or 5),
        )
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.HIGH_STACK_APPLY_RATE:
        r = repo.stack_apply_rate_extremes(
            parsed.start_date,
            parsed.end_date,
            order="DESC",
            regions_any=parsed.regions_any,
            admin_areas_any=parsed.admin_areas_any,
            industries_any=parsed.industries_any,
            job_role=parsed.job_role,
            limit=int(parsed.limit or 5),
        )
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.TOP_INDUSTRIES:
        r = repo.industry_extremes(parsed.start_date, parsed.end_date, order="DESC", regions_any=parsed.regions_any,
                                 admin_areas_any=parsed.admin_areas_any, limit=int(parsed.limit or 5))
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.BOTTOM_INDUSTRIES:
        r = repo.industry_extremes(parsed.start_date, parsed.end_date, order="ASC", regions_any=parsed.regions_any,
                                 admin_areas_any=parsed.admin_areas_any, limit=int(parsed.limit or 5))
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.MOST_APPLICANTS_POSTINGS:
        r = repo.most_applicants_posting(parsed.start_date, parsed.end_date, **_common_repo_kwargs(parsed, scope_ids))
        base.update(r)
        return {"result": base}

    return {"result": base}
