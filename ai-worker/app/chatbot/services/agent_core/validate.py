from __future__ import annotations

import logging
import re
from typing import Any, Dict, List

from app.chatbot.schemas import ChatbotIntent, ChatbotParsedSpec
from app.chatbot.services.agent_core.date_range import clamp_limit, clamp_range, default_range, infer_range_from_text
from app.chatbot.services.agent_core.filters import (
    infer_min_salary_m만원_from_text,
    infer_rate_filter_pct_from_text,
    infer_required_experience_years_from_text,
    infer_salary_range_m만원_from_text,
)
from app.chatbot.services.agent_core.intent import infer_intent_by_rule, is_followup
from app.chatbot.services.agent_core.types import ChatbotState, coerce_last
from app.chatbot.services.agent_core.url_intent import is_detail_url_request
from app.chatbot.utils.industry import infer_industries_from_text, is_industry_query
from app.chatbot.utils.keywords import normalize_keyword_list
from app.chatbot.utils.location import infer_admin_areas_from_text, infer_regions_from_text
from app.chatbot.utils.stack_detect import extract_stack_candidates
from app.core.config import settings

logger = logging.getLogger(__name__)

_RATE_TARGET_RE = re.compile(r"(지원률|경쟁률)")


async def validate_state(state: ChatbotState) -> Dict[str, Any]:
    parsed = state.get("parsed") or ChatbotParsedSpec(intent=ChatbotIntent.HELP, confidence=0.0)
    msg = (state.get("message") or "").strip()

    # ✅ 상세 페이지 URL 요청은 별도 intent로 처리 (기존 규칙/날짜보정/DB조회 방지)
    if is_detail_url_request(msg):
        parsed.intent = ChatbotIntent.DETAIL_URLS
        parsed.confidence = max(parsed.confidence, 0.95)
        return {
            "parsed": parsed,
            "scope_job_ids": [],
            "result": {"keyword_corrections": [], "stack_candidates": []},
        }


    last = coerce_last(state.get("last_parsed_dict") or {})
    is_follow = is_followup(msg) and (last is not None)

    # 1) follow-up에서 HELP/저신뢰면 intent 구제 (룰 → last.intent)
    if is_follow and (parsed.intent == ChatbotIntent.HELP or parsed.confidence < 0.45):
        fb = infer_intent_by_rule(msg)
        if fb is not None:
            parsed.intent = fb
            parsed.confidence = max(parsed.confidence, 0.7)
        else:
            try:
                parsed.intent = last.intent
                parsed.confidence = max(parsed.confidence, 0.7)
            except Exception:
                pass

    # ✅ BUSIEST_WEEK -> BUSIEST_DAY follow-up 강제
    # - "그중(에서)... 제일 많이 올라온 날" 류 문장
    if is_follow:
        last_int = state.get("last_intent") or ""
        if (last_int == ChatbotIntent.BUSIEST_WEEK.value) and (
            ("그중" in msg or "그 중" in msg or "그중에서" in msg or "그 중에서" in msg or "거기서" in msg or "거기에서" in msg or "이어서" in msg)
            and ("날" in msg or "날짜" in msg or "언제" in msg)
            and any(k in msg for k in ("제일", "가장", "최다", "많"))
        ):
            parsed.intent = ChatbotIntent.BUSIEST_DAY
            parsed.confidence = max(parsed.confidence, 0.85)

            ws = state.get("busiest_week_start_date")
            we = state.get("busiest_week_end_date")
            if ws and we:
                parsed.start_date = ws
                parsed.end_date = we



    # ✅ 스택 최다/최저 질의는 규칙 기반으로 intent 보정 (LLM 오류 방어)
    # - '제일 적게 공고가 올라온 스택' 같은 문장이 LLM에서 TOP_STACKS로 오분류되는 경우를 방지
    if ("스택" in msg) and ("지원률" not in msg) and ("경쟁률" not in msg):
        ml = msg.lower()
        if any(k in msg for k in ("적게", "적은", "낮은", "최저", "하위")) or any(k in ml for k in ("least", "bottom")):
            parsed.intent = ChatbotIntent.BOTTOM_STACKS
            parsed.confidence = max(parsed.confidence, 0.85)
        elif any(k in msg for k in ("많이", "많은", "높은", "최다", "상위", "가장", "제일")) or ("top" in ml):
            parsed.intent = ChatbotIntent.TOP_STACKS
            parsed.confidence = max(parsed.confidence, 0.85)

    # ✅ 경쟁률 극값 공고 질의는 규칙 기반으로 intent 보정 (LLM 오류 방어)
    # - "경쟁률이 제일 높은/낮은 공고" 같은 문장이 RATE_STATS로 오분류되는 경우를 방지
    # - "경쟁률 120% 이하 공고 몇개" 같은 '개수 질문'은 COUNT_POSTINGS로 남겨둔다.
    if ("경쟁률" in msg) and any(k in msg for k in ("공고", "채용")):
        is_count_query = any(k in msg for k in ("몇개", "몇 개", "몇건", "몇 건", "건수", "공고 수", "공고수"))
        if not is_count_query:
            if any(k in msg for k in ("낮", "최저", "하위")):
                parsed.intent = ChatbotIntent.LOW_COMPETITION_POSTINGS
                parsed.confidence = max(parsed.confidence, 0.85)
            elif any(k in msg for k in ("높", "최고", "상위")):
                parsed.intent = ChatbotIntent.HIGH_COMPETITION_POSTINGS
                parsed.confidence = max(parsed.confidence, 0.85)

    # ✅ "지원자 수 최다 공고" 질의는 '경쟁률/지원률' 단어가 포함돼도 RATE_STATS로 강제하지 않음
    # 예: "ai 직업군 중 경쟁률 200% 이상에서 지원자 수 제일 많은 공고는?"
    ml2 = msg.lower()
    if (
        ("공고" in msg or "채용" in msg)
        and (("지원자" in msg) or ("지원자수" in msg) or (("지원" in msg) and ("지원률" not in msg)))
        and any(k in msg for k in ("제일", "가장", "최다"))
        and any(k in ml2 for k in ("ai", "machine learning", "ml", "머신러닝"))
    ):
        parsed.intent = ChatbotIntent.MOST_APPLICANTS_POSTINGS
        parsed.confidence = max(parsed.confidence, 0.9)
        if parsed.job_role is None:
            parsed.job_role = "AI"

    # 2) 지원률/경쟁률 문장이면 RATE_STATS로 정규화
    # - 단, "지원률/경쟁률 극값" intent는 여기서 덮어쓰지 않는다.
    if _RATE_TARGET_RE.search(msg) and parsed.intent not in (
        ChatbotIntent.LOW_COMPETITION_POSTINGS,
        ChatbotIntent.HIGH_COMPETITION_POSTINGS,
        ChatbotIntent.LOW_STACK_APPLY_RATE,
        ChatbotIntent.HIGH_STACK_APPLY_RATE,
        ChatbotIntent.MOST_APPLICANTS_POSTINGS,
        ChatbotIntent.LIST_POSTINGS,
    ):
        if any(k in msg for k in ("몇개", "몇 개", "몇건", "몇 건", "건수", "공고 수", "공고수")):
            parsed.intent = ChatbotIntent.COUNT_POSTINGS
            parsed.confidence = max(parsed.confidence, 0.8)
        else:
            parsed.intent = ChatbotIntent.RATE_STATS
            parsed.confidence = max(parsed.confidence, 0.8)

    # 3) confidence gate (follow-up은 완화)
    thr = 0.45 if not is_follow else 0.25
    if parsed.confidence < thr:
        parsed.intent = ChatbotIntent.HELP

    if parsed.intent == ChatbotIntent.HELP:
        return {
            "parsed": parsed,
            "result": {"keyword_corrections": [], "stack_candidates": []},
            "scope_job_ids": [],
        }

    # 4) follow-up이면 last_parsed에서 기본 상속
    if is_follow and last is not None:
        parsed.start_date = parsed.start_date or last.start_date
        parsed.end_date = parsed.end_date or last.end_date

        if not parsed.regions_any:
            parsed.regions_any = list(last.regions_any)
        if not parsed.admin_areas_any:
            if re.search(r"([가-힣]{2,10})(구|시|군)\b", msg):
                parsed.admin_areas_any = infer_admin_areas_from_text(msg, parsed.regions_any)

        if parsed.job_role is None:
            parsed.job_role = last.job_role

        # ✅ 산업 필터 상속
        if not parsed.industries_any:
            parsed.industries_any = list(getattr(last, "industries_any", []) or [])

        if parsed.min_salary_m만원 is None:
            parsed.min_salary_m만원 = last.min_salary_m만원
        if parsed.max_salary_m만원 is None:
            parsed.max_salary_m만원 = last.max_salary_m만원

        if parsed.min_competition_pct is None:
            parsed.min_competition_pct = getattr(last, "min_competition_pct", None)
        if parsed.max_competition_pct is None:
            parsed.max_competition_pct = getattr(last, "max_competition_pct", None)

        # ✅ 경력 필터 상속
        if parsed.min_required_experience_years is None:
            parsed.min_required_experience_years = getattr(last, "min_required_experience_years", None)
        if parsed.max_required_experience_years is None:
            parsed.max_required_experience_years = getattr(last, "max_required_experience_years", None)

        if not parsed.keywords_all:
            parsed.keywords_all = list(last.keywords_all)
        if not parsed.keywords_any:
            parsed.keywords_any = list(last.keywords_any)

    # 5) 날짜: 메시지에 월/일이 있으면 follow-up에서도 override
    s2, e2 = infer_range_from_text(msg)
    if s2 and e2:
        parsed.start_date, parsed.end_date = s2, e2

    # 6) 그래도 없으면 default_range
    if parsed.start_date is None or parsed.end_date is None:
        s, e = default_range(parsed.intent)
        parsed.start_date = parsed.start_date or s
        parsed.end_date = parsed.end_date or e
    parsed.start_date, parsed.end_date = clamp_range(parsed.start_date, parsed.end_date)

    # 7) follow-up에서 스택 후보 추출 (1번만)
    stack_cands: List[str] = []
    if is_follow:
        try:
            stack_cands = extract_stack_candidates(msg) or []
        except Exception:
            logger.exception("extract_stack_candidates failed")
            stack_cands = []

        if stack_cands:
            or_hint = any(x in msg.lower() for x in ("또는", "혹은", " or ", "/", "|"))
            if or_hint:
                for s in stack_cands:
                    if s not in parsed.keywords_any:
                        parsed.keywords_any.append(s)
            else:
                for s in stack_cands:
                    if s not in parsed.keywords_all:
                        parsed.keywords_all.append(s)

    # 8) ✅ 산업/업종 힌트가 있으면 industries_any 자동 추론(LLM보다 우선)
    if not parsed.industries_any and is_industry_query(msg):
        parsed.industries_any = infer_industries_from_text(msg)

    # ✅ AI 직업군 질문은 industries_any 힌트가 없어도 "ai"를 산업 후보로 보정
    if parsed.intent == ChatbotIntent.MOST_APPLICANTS_POSTINGS and not parsed.industries_any:
        tl = msg.lower()
        if any(k in tl for k in ("ai", "machine learning", "ml", "머신러닝")):
            parsed.industries_any = ["ai"]

    # 9) ✅ 연봉 구간 우선 파싱
    sr = infer_salary_range_m만원_from_text(msg)
    if sr is not None:
        parsed.min_salary_m만원, parsed.max_salary_m만원 = sr
    else:
        smin = infer_min_salary_m만원_from_text(msg)
        if smin is not None:
            parsed.min_salary_m만원 = smin

    if parsed.min_salary_m만원 is not None and parsed.max_salary_m만원 is not None:
        if parsed.min_salary_m만원 > parsed.max_salary_m만원:
            parsed.min_salary_m만원, parsed.max_salary_m만원 = parsed.max_salary_m만원, parsed.min_salary_m만원

    # 10) 지역/행정구역 룰 추론
    if not parsed.regions_any:
        parsed.regions_any = infer_regions_from_text(msg)

    if (
        not parsed.admin_areas_any
        and parsed.regions_any
        and re.search(r"([가-힣]{2,10})(구|시|군)\b", msg)
    ):
        parsed.admin_areas_any = infer_admin_areas_from_text(msg, parsed.regions_any)

    # 11) 키워드 split/dedupe
    all_norm, corr_all = normalize_keyword_list(parsed.keywords_all, max_items=30)
    any_norm, corr_any = normalize_keyword_list(parsed.keywords_any, max_items=30)
    parsed.keywords_all = all_norm
    parsed.keywords_any = any_norm

    # industries split/dedupe
    ind_norm, corr_ind = normalize_keyword_list(parsed.industries_any, max_items=10)
    parsed.industries_any = ind_norm

    # 12) limit
    default_limit = int(getattr(settings, "CHATBOT_DEFAULT_RESULT_LIMIT", 5))
    parsed.limit = clamp_limit(parsed.limit, default=default_limit)

    # ✅ MOST_APPLICANTS_POSTINGS는 항상 1개만
    if parsed.intent == ChatbotIntent.MOST_APPLICANTS_POSTINGS:
        parsed.limit = 1


    # ✅ TOP/BOTTOM 스택은 '제일/가장'만 말했을 때도 기본 5개를 보여주도록 보정
    # - 사용자가 '1개/한 개/하나만' 등을 명시하지 않았다면 limit=1은 기본값(default_limit)으로 교정
    if parsed.intent in (ChatbotIntent.TOP_STACKS, ChatbotIntent.BOTTOM_STACKS):
        has_explicit_limit = bool(re.search(r"\d+\s*(개|회|건|명)", msg)) or any(
            k in msg for k in ("한 개", "한개", "하나", "하나만", "1개", "1 개")
        )
        if (not has_explicit_limit) and (parsed.limit == 1):
            parsed.limit = default_limit

    # 13) LIST_POSTINGS random default
    if parsed.intent != ChatbotIntent.LIST_POSTINGS:
        parsed.random = None
    elif parsed.random is None:
        ml = msg.lower()
        parsed.random = ("랜덤" in msg) or ("무작위" in msg) or ("random" in ml)

    # 14) ✅ 지원률/경쟁률(%) 필터 파싱(LLM보다 우선)
    rf = infer_rate_filter_pct_from_text(msg)
    if rf["min_competition_pct"] is not None:
        parsed.min_competition_pct = rf["min_competition_pct"]
    if rf["max_competition_pct"] is not None:
        parsed.max_competition_pct = rf["max_competition_pct"]

    if parsed.min_competition_pct is not None and parsed.max_competition_pct is not None:
        if parsed.min_competition_pct > parsed.max_competition_pct:
            parsed.min_competition_pct, parsed.max_competition_pct = (
                parsed.max_competition_pct,
                parsed.min_competition_pct,
            )

    # ✅ required_experience(년) 파싱(LLM보다 우선)
    er = infer_required_experience_years_from_text(msg)
    if er["min"] is not None:
        parsed.min_required_experience_years = er["min"]
    if er["max"] is not None:
        parsed.max_required_experience_years = er["max"]

    if (
        parsed.min_required_experience_years is not None
        and parsed.max_required_experience_years is not None
        and parsed.min_required_experience_years > parsed.max_required_experience_years
    ):
        parsed.min_required_experience_years, parsed.max_required_experience_years = (
            parsed.max_required_experience_years,
            parsed.min_required_experience_years,
        )

    # 15) ✅ 후속질의에서 "그 공고들" 스코프(직전 LIST 결과)
    scope_ids: List[int] = []
    if is_follow:
        last_ids = state.get("last_item_ids") or []
        if last_ids and any(
            x in msg
            for x in (
                "그 공고",
                "그공고",
                "그 공고들",
                "그공고들",
                "그중",
                "그 중",
                "그중에서",
                "그 중에서",
                "거기서",
                "거기에서",
                "이어서",
                "방금",
                "이전",
                "아까",
            )
        ):
            scope_ids = [int(x) for x in last_ids]

    return {
        "parsed": parsed,
        "scope_job_ids": scope_ids,
        "result": {
            "keyword_corrections": (corr_all + corr_any + corr_ind)[:20],
            "stack_candidates": (stack_cands[:20] if is_follow else []),
        },
    }
