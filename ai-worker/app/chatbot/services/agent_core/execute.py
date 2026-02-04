from __future__ import annotations

import uuid
from urllib.parse import urlencode

from typing import Any, Dict, Optional

from app.chatbot.schemas import ChatbotIntent, ChatbotParsedSpec
from app.chatbot.services.agent_core.types import ChatbotState
from app.chatbot.utils.stack_detect import extract_stack_candidates



def _common_repo_kwargs(parsed: ChatbotParsedSpec, scope_ids: Optional[list[int]] = None) -> Dict[str, Any]:
    return {
        "regions_any": parsed.regions_any,
        "admin_areas_any": parsed.admin_areas_any,
        "keywords_all": parsed.keywords_all,
        "keywords_any": parsed.keywords_any,
        "industries_any": parsed.industries_any,
        "job_role": parsed.job_role,
        "positions_any": parsed.positions_any or [],
        "min_salary_m만원": parsed.min_salary_m만원,
        "max_salary_m만원": parsed.max_salary_m만원,
        "min_competition_pct": parsed.min_competition_pct,
        "max_competition_pct": parsed.max_competition_pct,
        "min_required_experience_years": parsed.min_required_experience_years,
        "max_required_experience_years": parsed.max_required_experience_years,
        "job_ids_scope": scope_ids,
    }



def _stack_token_to_ui_label(tok: str) -> str:
    if not tok:
        return tok
    t = tok.strip().lower()
    MAP = {
        "python": "Python",
        "java": "Java",
        "javascript": "JavaScript",
        "typescript": "TypeScript",
        "react": "React",
        "vue": "Vue",
        "spring": "Spring",
        "spring boot": "Spring",
        "node.js": "Node.js",
        "django": "Django",
        "aws": "AWS",
        "docker": "Docker",
        "kubernetes": "Kubernetes",
    }
    return MAP.get(t, tok)


def _industry_token_to_ui_label(tok: str) -> str:
    if not tok:
        return tok
    t = tok.strip().lower()
    if t in ("ai", "ml", "machine learning", "artificial intelligence") or ("인공지능" in tok):
        return "인공지능"
    return tok


def _build_jobs_filter_url(parsed: ChatbotParsedSpec, msg: str) -> tuple[str, list[str], list[str]]:
    """Build /jobs?... URL for the frontend.

    NOTE:
    - 날짜(start/end), 경쟁률/지원률(min_competition_pct) 등은 Jobs 페이지에 필터 UI가 없으므로 URL에서 제외.
    - 연봉은 validate 단계에서 1000만원 단위로 버킷 보정됨.
    """
    applied: list[str] = []
    excluded: list[str] = []

    # stacks: parse from message (robust)
    stack_tokens = extract_stack_candidates(msg or "")
    stack_labels = [_stack_token_to_ui_label(x) for x in stack_tokens if x]
    # positions: use parsed.positions_any (already sanitized)
    positions = list(parsed.positions_any or [])
    industries = [_industry_token_to_ui_label(x) for x in (parsed.industries_any or []) if x]

    # location: prefer region (e.g. 서울) then admin area
    location = ""
    if parsed.regions_any:
        location = parsed.regions_any[0]
    elif parsed.admin_areas_any:
        location = parsed.admin_areas_any[0]

    params: dict[str, str] = {}

    if stack_labels:
        params["stack"] = ",".join(stack_labels)
        applied.append("stack")
    if positions:
        params["position"] = ",".join(positions)
        applied.append("position")
    if industries:
        params["industry"] = ",".join(industries)
        applied.append("industry")
    if location:
        params["location"] = location
        applied.append("location")

    # experience
    if parsed.min_required_experience_years is not None:
        params["minExperience"] = str(int(parsed.min_required_experience_years))
        applied.append("minExperience")
    if parsed.max_required_experience_years is not None:
        params["maxExperience"] = str(int(parsed.max_required_experience_years))
        applied.append("maxExperience")

    # salary (만원)
    if parsed.min_salary_m만원 is not None:
        params["salaryMin"] = str(int(parsed.min_salary_m만원))
        applied.append("salaryMin")
    if parsed.max_salary_m만원 is not None:
        params["salaryMax"] = str(int(parsed.max_salary_m만원))
        applied.append("salaryMax")

    # excluded filters
    if parsed.min_competition_pct is not None or parsed.max_competition_pct is not None:
        excluded.append("경쟁률/지원률")
    if parsed.start_date is not None or parsed.end_date is not None:
        # "1월" 같은 날짜 조건이 있어도 Jobs 페이지엔 날짜 필터 UI가 없음
        excluded.append("날짜")

    qs = urlencode(params, safe=",")
    url = "/jobs" + (("?" + qs) if qs else "")
    return url, applied, excluded


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


    if parsed.intent == ChatbotIntent.NAVIGATE_FILTERED_PAGE:
        msg = state.get("message") or ""
        url, applied, excluded = _build_jobs_filter_url(parsed, msg)
        base.update({
            "action": "NAVIGATE",
            "redirect_url": url,
            "applied_filters": applied,
            "excluded_filters": excluded,
            "start_date": str(parsed.start_date) if parsed.start_date else None,
            "end_date": str(parsed.end_date) if parsed.end_date else None,
        })
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
            positions_any=parsed.positions_any or [],
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
            positions_any=parsed.positions_any or [],
            limit=int(parsed.limit or 5),
        )
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.TOP_INDUSTRIES:
        r = repo.industry_extremes(parsed.start_date, parsed.end_date, order="DESC", regions_any=parsed.regions_any,
                                 admin_areas_any=parsed.admin_areas_any, positions_any=parsed.positions_any or [], limit=int(parsed.limit or 5))
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.BOTTOM_INDUSTRIES:
        r = repo.industry_extremes(parsed.start_date, parsed.end_date, order="ASC", regions_any=parsed.regions_any,
                                 admin_areas_any=parsed.admin_areas_any, positions_any=parsed.positions_any or [], limit=int(parsed.limit or 5))
        base.update(r)
        return {"result": base}

    if parsed.intent == ChatbotIntent.MOST_APPLICANTS_POSTINGS:
        r = repo.most_applicants_posting(parsed.start_date, parsed.end_date, **_common_repo_kwargs(parsed, scope_ids))
        base.update(r)
        return {"result": base}

    return {"result": base}
