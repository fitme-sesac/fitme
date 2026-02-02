# app/employer_chatbot/services/agent.py
from __future__ import annotations

import calendar
import json
import logging
import re
import uuid
from datetime import date, datetime, timedelta
from typing import Any, Dict, List, Optional, TypedDict

from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI
from langgraph.graph import END, StateGraph
from zoneinfo import ZoneInfo

from app.employer_chatbot.repository import employer_stats_repo
from app.employer_chatbot.schemas import EmployerChatbotIntent, EmployerChatbotParsedSpec
from app.employer_chatbot.services.memory import (
    EmployerChatbotConversationState,
    ConversationMode,
    RedisEmployerChatbotMemoryStore,
    TranscriptItem,
)
from app.core.config import settings
from app.core.redis import get_redis

logger = logging.getLogger(__name__)
KST = ZoneInfo("Asia/Seoul")

# -------------------------
# Date parsing helpers
# -------------------------
_MONTH_RE = re.compile(r"(?:(\d{4})\s*년\s*)?(\d{1,2})\s*월(?:\s*(\d{1,2})\s*일)?")
_THIS_MONTH = ("이번달", "이번 달")
_LAST_MONTH = ("지난달", "지난 달", "저번달", "저번 달")
_NEXT_MONTH = ("다음달", "다음 달")
_FROM_DATE_HINT_RE = re.compile(r"(부터|이후|뒤로|이후로|이후부터|지금까지|현재까지|까지)")
_STRICT_AFTER_HINT_RE = re.compile(r"(후에|후로|후|뒤)(?=\s|$|[가-힣])")

_TODAY_WORDS = ("오늘",)
_YESTERDAY_WORDS = ("어제",)
_TOMORROW_WORDS = ("내일",)
_DAY_BEFORE_YESTERDAY_WORDS = ("그저께", "그제")

_THIS_WEEK = ("이번주", "이번 주")
_LAST_WEEK = ("지난주", "지난 주", "저번주", "저번 주")
_NEXT_WEEK = ("다음주", "다음 주")

_KOR_NUM = {
    "한": 1, "두": 2, "세": 3, "네": 4,
    "다섯": 5, "여섯": 6, "일곱": 7, "여덟": 8, "아홉": 9, "열": 10,
}
_RECENT_RE = re.compile(r"(최근|요즘)\s*([0-9]{1,3}|한|두|세|네|다섯|여섯|일곱|여덟|아홉|열)?\s*(일|주|주일|달|개월|년)")
_BEFORE_RE = re.compile(r"([0-9]{1,3}|한|두|세|네|다섯|여섯|일곱|여덟|아홉|열)\s*(일|주|주일|달|개월|년)\s*전")


def _num_token_to_int(tok: str, default: int = 1) -> int:
    if not tok:
        return default
    t = tok.strip()
    if t.isdigit():
        return int(t)
    return int(_KOR_NUM.get(t, default))


def _safe_date(y: int, m: int, d: int) -> date:
    last = calendar.monthrange(y, m)[1]
    return date(y, m, min(d, last))


def _start_of_week(d: date) -> date:
    return d - timedelta(days=d.weekday())


def _add_month(y: int, m: int, delta: int) -> tuple[int, int]:
    m2 = m + delta
    y2 = y + (m2 - 1) // 12
    m2 = (m2 - 1) % 12 + 1
    return y2, m2


def _infer_recent_range(t: str, today: date) -> tuple[date | None, date | None]:
    m = _RECENT_RE.search(t)
    if not m:
        return None, None
    n = _num_token_to_int(m.group(2), default=1)
    unit = m.group(3)

    end = today + timedelta(days=1)
    if unit == "일":
        return today - timedelta(days=n), end
    if unit in ("주", "주일"):
        return today - timedelta(days=7 * n), end
    if unit in ("달", "개월"):
        y, mm = _add_month(today.year, today.month, -n)
        s = _safe_date(y, mm, today.day)
        return s, end
    if unit == "년":
        s = _safe_date(today.year - n, today.month, today.day)
        return s, end
    return None, None


def _infer_n_before_range(t: str, today: date) -> tuple[date | None, date | None]:
    m = _BEFORE_RE.search(t)
    if not m:
        return None, None

    n = _num_token_to_int(m.group(1), default=1)
    unit = m.group(2)
    tail = t[m.end():]

    if unit == "일":
        base = today - timedelta(days=n)
    elif unit in ("주", "주일"):
        base = today - timedelta(days=7 * n)
    elif unit in ("달", "개월"):
        y, mm = _add_month(today.year, today.month, -n)
        base = _safe_date(y, mm, today.day)
    elif unit == "년":
        base = _safe_date(today.year - n, today.month, today.day)
    else:
        return None, None

    if _FROM_DATE_HINT_RE.search(tail):
        return base, today + timedelta(days=1)
    return base, base + timedelta(days=1)


def _first_day(y: int, m: int) -> date:
    return date(y, m, 1)


def _today_kst() -> date:
    return datetime.now(tz=KST).date()


def _infer_range_from_text(msg: str) -> tuple[date | None, date | None]:
    if not msg:
        return None, None
    t = msg.strip()
    today = _today_kst()

    if any(k in t for k in _TODAY_WORDS):
        return today, today + timedelta(days=1)
    if any(k in t for k in _YESTERDAY_WORDS):
        d = today - timedelta(days=1)
        return d, d + timedelta(days=1)
    if any(k in t for k in _DAY_BEFORE_YESTERDAY_WORDS):
        d = today - timedelta(days=2)
        return d, d + timedelta(days=1)
    if any(k in t for k in _TOMORROW_WORDS):
        d = today + timedelta(days=1)
        return d, d + timedelta(days=1)

    s, e = _infer_recent_range(t, today)
    if s and e:
        return s, e

    s, e = _infer_n_before_range(t, today)
    if s and e:
        return s, e

    if any(k in t for k in _THIS_WEEK):
        s = _start_of_week(today)
        e = today + timedelta(days=1)
        return s, e
    if any(k in t for k in _LAST_WEEK):
        this_start = _start_of_week(today)
        s = this_start - timedelta(days=7)
        e = this_start
        return s, e
    if any(k in t for k in _NEXT_WEEK):
        this_start = _start_of_week(today)
        s = this_start + timedelta(days=7)
        e = s + timedelta(days=7)
        return s, e

    if any(k in t for k in _THIS_MONTH):
        y, m = today.year, today.month
        s = _first_day(y, m)
        y2, m2 = _add_month(y, m, 1)
        e = _first_day(y2, m2)
        return s, e

    if any(k in t for k in _LAST_MONTH):
        y, m = _add_month(today.year, today.month, -1)
        s = _first_day(y, m)
        y2, m2 = _add_month(y, m, 1)
        e = _first_day(y2, m2)
        return s, e

    if any(k in t for k in _NEXT_MONTH):
        y, m = _add_month(today.year, today.month, 1)
        s = _first_day(y, m)
        y2, m2 = _add_month(y, m, 1)
        e = _first_day(y2, m2)
        return s, e

    m = _MONTH_RE.search(t)
    if not m:
        return None, None

    year = int(m.group(1) or today.year)
    month = int(m.group(2))
    day = m.group(3)
    tail = t[m.end():]

    if day:
        s = date(year, month, int(day))

        if _STRICT_AFTER_HINT_RE.search(tail):
            s = s + timedelta(days=1)
            e = today + timedelta(days=1)
            return s, e

        if _FROM_DATE_HINT_RE.search(tail):
            e = today + timedelta(days=1)
            return s, e

        e = s + timedelta(days=1)
        return s, e

    s = date(year, month, 1)

    if _FROM_DATE_HINT_RE.search(tail):
        e = today + timedelta(days=1)
        return s, e

    y2, m2 = _add_month(year, month, 1)
    e = date(y2, m2, 1)
    return s, e


def _default_range(_: EmployerChatbotIntent) -> tuple[date, date]:
    today = _today_kst()
    lookback = int(getattr(settings, "CHATBOT_DEFAULT_LOOKBACK_DAYS", 30))
    return today - timedelta(days=lookback), today + timedelta(days=1)


def _clamp_range(start: date, end: date) -> tuple[date, date]:
    if end <= start:
        end = start + timedelta(days=1)
    max_days = int(getattr(settings, "CHATBOT_MAX_RANGE_DAYS", 365))
    if (end - start).days > max_days:
        end = start + timedelta(days=max_days)
    return start, end


def _clamp_limit(n: Optional[int], default: int = 10) -> int:
    try:
        x = int(n) if n is not None else default
    except Exception:
        x = default
    return max(1, min(50, x))


def _infer_intent_by_rule(msg: str) -> Optional[EmployerChatbotIntent]:
    if not msg:
        return None
    ml = msg.lower()

    if any(k in msg for k in ("목록", "리스트", "보여줘", "알려줘")):
        if any(k in msg for k in ("지원자", "지원", "응시자")):
            return EmployerChatbotIntent.LIST_APPLICATIONS

    if any(k in msg for k in ("몇명", "몇 명", "몇건", "몇 건", "지원자 수", "지원수")):
        return EmployerChatbotIntent.COUNT_APPLICATIONS

    if any(k in msg for k in ("통계", "분석", "현황", "상태별")):
        return EmployerChatbotIntent.APPLICATION_STATS

    if any(k in msg for k in ("공고 성과", "성과", "퍼포먼스", "공고별")):
        return EmployerChatbotIntent.POSTING_PERFORMANCE

    if any(k in msg for k in ("스킬", "기술", "역량")) and any(k in msg for k in ("많이", "상위", "탑", "top")):
        return EmployerChatbotIntent.TOP_SKILLS

    return None


def _coerce_last(last_parsed_dict: Dict[str, Any]) -> Optional[EmployerChatbotParsedSpec]:
    try:
        return EmployerChatbotParsedSpec.model_validate(last_parsed_dict or {})
    except Exception:
        return None


_FOLLOWUP_MARKERS = ("그 중", "그중", "그 중에", "그중에", "그러면", "그럼", "거기서", "방금", "이전", "그 지원자", "해당")


def _is_followup(msg: str) -> bool:
    return bool(msg) and any(m in msg for m in _FOLLOWUP_MARKERS)


class EmployerChatbotState(TypedDict, total=False):
    request_id: str
    conversation_id: str
    employer_id: int
    conversation_mode: str
    turn: int

    message: str
    parsed: EmployerChatbotParsedSpec
    result: Dict[str, Any]
    answer: str

    last_parsed_dict: Dict[str, Any]
    last_intent: str
    last_item_ids: List[int]

    scope_application_ids: List[int]


class EmployerStatsChatbot:
    def __init__(self):
        chatbot_model = getattr(settings, "CHATBOT_OPENAI_MODEL_NAME", None) or settings.OPENAI_MODEL_NAME
        chatbot_temp = float(getattr(settings, "CHATBOT_OPENAI_TEMPERATURE", 0.0))

        self.llm = ChatOpenAI(
            model=chatbot_model,
            temperature=chatbot_temp,
            openai_api_key=settings.OPENAI_API_KEY,
        )

        self.parser = PydanticOutputParser(pydantic_object=EmployerChatbotParsedSpec)
        self.graph = self._build_graph()

        ttl = int(getattr(settings, "CHATBOT_CONVERSATION_TTL_SECONDS", 604800))
        self.memory = RedisEmployerChatbotMemoryStore(get_redis(), ttl_seconds=ttl)

        self.max_full_turns = int(getattr(settings, "CHATBOT_MAX_FULL_TURNS", 10))
        self.max_transcript_items = int(getattr(settings, "CHATBOT_MAX_TRANSCRIPT_ITEMS", 200))

    def _build_graph(self):
        sg = StateGraph(EmployerChatbotState)
        sg.add_node("parse", self._parse)
        sg.add_node("validate", self._validate)
        sg.add_node("execute", self._execute)
        sg.add_node("answer", self._answer)

        sg.set_entry_point("parse")
        sg.add_edge("parse", "validate")
        sg.add_edge("validate", "execute")
        sg.add_edge("execute", "answer")
        sg.add_edge("answer", END)
        return sg.compile()

    async def _parse(self, state: EmployerChatbotState) -> Dict[str, Any]:
        msg = state.get("message", "")
        today_d = _today_kst()
        today = today_d.isoformat()
        year = today_d.year

        system = """[Role]
너는 기업용 채용 관리 챗봇의 '질문 파서(parser)'다.
기업 담당자가 자사 채용공고의 지원자 현황, 통계, 성과를 조회할 수 있도록 돕는다.
출력은 서버가 DB 조회 파라미터로만 사용한다.

[Rules]
- 추측 금지.
- intent는 가능하면 반드시 COUNT_APPLICATIONS/LIST_APPLICATIONS/APPLICATION_STATS/POSTING_PERFORMANCE/TOP_SKILLS 중 하나로 선택하라.
- intent 자체가 전혀 판단되지 않을 때만 HELP.
- 날짜는 YYYY-MM-DD. end_date는 exclusive(미포함).
- 날짜/필터가 불확실하면 비워두고(Null/empty), validate 단계에서 기본값/상속이 처리된다.

[Supported intents]
1) COUNT_APPLICATIONS - 지원자 수 카운트
2) LIST_APPLICATIONS - 지원자 목록 조회
3) APPLICATION_STATS - 지원 통계 (총 지원수, 상태별 분포, 공고별 분포)
4) POSTING_PERFORMANCE - 공고 성과 분석 (공고별 지원수, 경쟁률)
5) TOP_SKILLS - 지원자들이 보유한 스킬 상위
6) HELP

[Fields]
- job_posting_ids: 특정 공고 ID 필터 (정수 배열)
- job_title_keywords: 공고 제목 키워드 필터 (예: ['백엔드', '프론트엔드'])
- applicant_skills_any: 지원자 스킬 OR 필터
- applicant_skills_all: 지원자 스킬 AND 필터
- application_status_any: 지원 상태 필터 (PENDING, REVIEWED, SHORTLISTED, REJECTED, HIRED)
- min_experience_years / max_experience_years: 경력 필터
- min_expected_salary_m만원 / max_expected_salary_m만원: 희망연봉 필터
- regions_any: 지역 필터
- limit: 결과 개수 제한 (1..50)
- random: 랜덤 여부

[Multi-turn]
- '그 중에/그러면/거기서' 같은 후속 질문은 이전 컨텍스트를 참고.
"""

        examples = f"""[Today]
{today}

[Examples]
- '오늘 지원자 몇명이야?' => COUNT_APPLICATIONS, start_date=today, end_date=today+1
- '이번달 지원자 현황 보여줘' => APPLICATION_STATS, start_date={year}-{today_d.month:02d}-01
- '백엔드 공고에 지원한 사람들 목록' => LIST_APPLICATIONS, job_title_keywords=['백엔드']
- '지원 상태가 PENDING인 지원자 몇명?' => COUNT_APPLICATIONS, application_status_any=['PENDING']
- '우리 공고 성과 분석해줘' => POSTING_PERFORMANCE
- '지원자들이 가장 많이 보유한 스킬은?' => TOP_SKILLS
- '서류 통과한 지원자 목록' => LIST_APPLICATIONS, application_status_any=['SHORTLISTED']
"""

        context_dict = state.get("last_parsed_dict") or {}
        context_json = json.dumps(context_dict, ensure_ascii=False) if context_dict else ""

        prompt = ChatPromptTemplate.from_messages(
            [
                ("system", system + "\n\n{format_instructions}"),
                (
                    "user",
                    examples
                    + "\n\n[Conversation context]\n{context}\n"
                    + "\n[User question]\n{message}",
                ),
            ]
        ).partial(format_instructions=self.parser.get_format_instructions())

        chain = prompt | self.llm | self.parser
        payload = {"message": msg, "context": context_json}

        try:
            parsed: EmployerChatbotParsedSpec = await chain.ainvoke(payload)
            return {"parsed": parsed}
        except Exception:
            logger.exception("employer chatbot parse failed (1st)")
            raw = await (prompt | self.llm).ainvoke(payload)
            logger.error("LLM raw output: %s", getattr(raw, "content", raw))
            try:
                parsed: EmployerChatbotParsedSpec = await chain.ainvoke(payload)
                return {"parsed": parsed}
            except Exception:
                logger.exception("employer chatbot parse failed (2nd)")
                return {"parsed": EmployerChatbotParsedSpec(intent=EmployerChatbotIntent.HELP, confidence=0.0)}

    async def _validate(self, state: EmployerChatbotState) -> Dict[str, Any]:
        parsed = state.get("parsed") or EmployerChatbotParsedSpec(intent=EmployerChatbotIntent.HELP, confidence=0.0)
        msg = state.get("message", "") or ""

        last = _coerce_last(state.get("last_parsed_dict") or {})
        is_follow = _is_followup(msg) and (last is not None)

        # 1) follow-up에서 HELP/저신뢰면 intent 구제
        if is_follow and (parsed.intent == EmployerChatbotIntent.HELP or parsed.confidence < 0.45):
            fb = _infer_intent_by_rule(msg)
            if fb is not None:
                parsed.intent = fb
                parsed.confidence = max(parsed.confidence, 0.7)
            else:
                try:
                    parsed.intent = last.intent
                    parsed.confidence = max(parsed.confidence, 0.7)
                except Exception:
                    pass

        # 2) confidence gate
        thr = 0.45 if not is_follow else 0.25
        if parsed.confidence < thr:
            parsed.intent = EmployerChatbotIntent.HELP

        if parsed.intent == EmployerChatbotIntent.HELP:
            return {"parsed": parsed, "result": {}, "scope_application_ids": []}

        # 3) follow-up 상속
        if is_follow and last is not None:
            parsed.start_date = parsed.start_date or last.start_date
            parsed.end_date = parsed.end_date or last.end_date

            if not parsed.job_posting_ids:
                parsed.job_posting_ids = list(getattr(last, "job_posting_ids", []) or [])
            if not parsed.job_title_keywords:
                parsed.job_title_keywords = list(getattr(last, "job_title_keywords", []) or [])
            if not parsed.application_status_any:
                parsed.application_status_any = list(getattr(last, "application_status_any", []) or [])

        # 4) 날짜 추론
        s2, e2 = _infer_range_from_text(msg)
        if s2 and e2:
            parsed.start_date, parsed.end_date = s2, e2

        if parsed.start_date is None or parsed.end_date is None:
            s, e = _default_range(parsed.intent)
            parsed.start_date = parsed.start_date or s
            parsed.end_date = parsed.end_date or e
        parsed.start_date, parsed.end_date = _clamp_range(parsed.start_date, parsed.end_date)

        # 5) limit
        default_limit = int(getattr(settings, "CHATBOT_DEFAULT_RESULT_LIMIT", 10))
        parsed.limit = _clamp_limit(parsed.limit, default=default_limit)

        # 6) random
        if parsed.intent != EmployerChatbotIntent.LIST_APPLICATIONS:
            parsed.random = None
        elif parsed.random is None:
            ml = msg.lower()
            parsed.random = ("랜덤" in msg) or ("무작위" in msg) or ("random" in ml)

        return {
            "parsed": parsed,
            "scope_application_ids": [],
            "result": {},
        }

    async def _execute(self, state: EmployerChatbotState) -> Dict[str, Any]:
        parsed: EmployerChatbotParsedSpec = state["parsed"]
        rid = state.get("request_id") or str(uuid.uuid4())
        employer_id = state.get("employer_id") or 0
        base = state.get("result") or {}

        if parsed.intent == EmployerChatbotIntent.COUNT_APPLICATIONS:
            r = employer_stats_repo.count_applications(
                employer_id=employer_id,
                start_date=parsed.start_date,
                end_date=parsed.end_date,
                job_posting_ids=parsed.job_posting_ids or None,
                job_title_keywords=parsed.job_title_keywords or None,
                application_status_any=parsed.application_status_any or None,
            )
            base.update(r)
            return {"result": base}

        if parsed.intent == EmployerChatbotIntent.LIST_APPLICATIONS:
            r = employer_stats_repo.list_applications(
                employer_id=employer_id,
                start_date=parsed.start_date,
                end_date=parsed.end_date,
                request_id=rid,
                job_posting_ids=parsed.job_posting_ids or None,
                job_title_keywords=parsed.job_title_keywords or None,
                application_status_any=parsed.application_status_any or None,
                limit=int(parsed.limit or 10),
                random=bool(parsed.random),
            )
            base.update(r)
            return {"result": base}

        if parsed.intent == EmployerChatbotIntent.APPLICATION_STATS:
            r = employer_stats_repo.application_stats(
                employer_id=employer_id,
                start_date=parsed.start_date,
                end_date=parsed.end_date,
                job_posting_ids=parsed.job_posting_ids or None,
                job_title_keywords=parsed.job_title_keywords or None,
            )
            base.update(r)
            return {"result": base}

        if parsed.intent == EmployerChatbotIntent.POSTING_PERFORMANCE:
            r = employer_stats_repo.posting_performance(
                employer_id=employer_id,
                start_date=parsed.start_date,
                end_date=parsed.end_date,
                job_posting_ids=parsed.job_posting_ids or None,
                limit=int(parsed.limit or 10),
            )
            base.update(r)
            return {"result": base}

        if parsed.intent == EmployerChatbotIntent.TOP_SKILLS:
            r = employer_stats_repo.top_skills(
                employer_id=employer_id,
                start_date=parsed.start_date,
                end_date=parsed.end_date,
                job_posting_ids=parsed.job_posting_ids or None,
                limit=int(parsed.limit or 10),
            )
            base.update(r)
            return {"result": base}

        return {"result": base}

    async def _answer(self, state: EmployerChatbotState) -> Dict[str, Any]:
        rid = state.get("request_id") or str(uuid.uuid4())
        parsed: EmployerChatbotParsedSpec = state.get("parsed") or EmployerChatbotParsedSpec(intent=EmployerChatbotIntent.HELP, confidence=0.0)
        result = state.get("result") or {}

        if parsed.intent == EmployerChatbotIntent.HELP:
            answer = (
                "지원 예시:\n"
                "- 오늘 지원자 몇명이야?\n"
                "- 이번달 지원 현황 보여줘\n"
                "- 백엔드 공고 지원자 목록\n"
                "- 서류 통과한 지원자는?\n"
                "- 우리 공고 성과 분석해줘\n"
                "- 지원자들이 많이 보유한 스킬은?\n"
                "- PENDING 상태 지원자 몇명?\n"
            )
            return {"answer": answer, "request_id": rid}

        start = result.get("start_date")
        end = result.get("end_date")

        def filters_summary() -> str:
            parts = []
            if result.get("job_posting_ids"):
                parts.append(f"공고ID: {result['job_posting_ids']}")
            if result.get("job_title_keywords"):
                parts.append(f"공고키워드: {', '.join(result['job_title_keywords'])}")
            if result.get("application_status_any"):
                parts.append(f"상태: {', '.join(result['application_status_any'])}")
            return (" | ".join(parts)) if parts else "필터: 없음"

        if parsed.intent == EmployerChatbotIntent.COUNT_APPLICATIONS:
            cnt = int(result.get("count", 0))
            answer = f"{start} ~ {end} 지원자 수: {cnt}명\n{filters_summary()}"

        elif parsed.intent == EmployerChatbotIntent.LIST_APPLICATIONS:
            items = result.get("items") or []
            lim = int(result.get("limit", 10))
            rnd = bool(result.get("random"))
            lines = [
                f"{start} ~ {end} 지원자 {len(items)}명 (limit={lim}, {'랜덤' if rnd else '최신'}):",
                filters_summary(),
            ]
            for i, it in enumerate(items, 1):
                lines.append(
                    f"{i}. {it.get('applicant_name')} ({it.get('applicant_email')}) "
                    f"| 공고: {it.get('job_title')} "
                    f"| 상태: {it.get('status')} "
                    f"| 지원일: {it.get('created_at') or '-'}"
                )
            answer = "\n".join(lines)

        elif parsed.intent == EmployerChatbotIntent.APPLICATION_STATS:
            total = int(result.get("total_applications", 0))
            status_dist = result.get("status_distribution") or {}
            by_posting = result.get("by_posting") or []

            lines = [
                f"{start} ~ {end} 지원 통계:",
                f"- 총 지원자 수: {total}명",
                "- 상태별 분포:",
            ]
            for status, cnt in status_dist.items():
                lines.append(f"  · {status}: {cnt}명")

            if by_posting:
                lines.append("- 공고별 지원자 수 (상위 10개):")
                for p in by_posting[:10]:
                    lines.append(f"  · [{p['job_id']}] {p['title']}: {p['count']}명")

            lines.append(filters_summary())
            answer = "\n".join(lines)

        elif parsed.intent == EmployerChatbotIntent.POSTING_PERFORMANCE:
            items = result.get("items") or []
            lines = [
                f"{start} ~ {end} 공고 성과 분석 (상위 {len(items)}개):",
                filters_summary(),
            ]
            for i, it in enumerate(items, 1):
                comp_str = f"{it['competition_pct']:.1f}%" if it.get('competition_pct') is not None else "-"
                lines.append(
                    f"{i}. [{it['job_id']}] {it['title']} | 상태: {it['status']} "
                    f"| 지원: {it['apply_count']}명 | 모집: {it['recruitment_capacity'] or '-'}명 "
                    f"| 경쟁률: {comp_str}"
                )
            answer = "\n".join(lines)

        elif parsed.intent == EmployerChatbotIntent.TOP_SKILLS:
            items = result.get("items") or []
            lim = int(result.get("limit", 10))
            lines = [f"{start} ~ {end} 지원자 보유 스킬 TOP {lim}:", filters_summary()]
            for i, it in enumerate(items, 1):
                lines.append(f"{i}. {it['skill']} ({it['count']}명)")
            answer = "\n".join(lines)

        else:
            answer = "지원하지 않는 질문입니다."

        return {"answer": answer, "request_id": rid}

    async def ask(
            self,
            message: str,
            request_id: Optional[str] = None,
            conversation_id: Optional[str] = None,
            employer_id: Optional[int] = None,
    ) -> Dict[str, Any]:
        rid = request_id or str(uuid.uuid4())
        cid = conversation_id or str(uuid.uuid4())

        ctx = await self.memory.load(cid)
        if ctx is None:
            ctx = EmployerChatbotConversationState(conversation_id=cid, employer_id=employer_id)

        if employer_id is not None:
            ctx.employer_id = employer_id

        ctx.turn += 1
        if ctx.mode == ConversationMode.FULL and ctx.turn > self.max_full_turns:
            ctx.mode = ConversationMode.SUMMARY
            ctx.turn = 1
            ctx.last_item_ids = []

        out = await self.graph.ainvoke(
            {
                "message": message,
                "request_id": rid,
                "conversation_id": cid,
                "employer_id": ctx.employer_id or 0,
                "conversation_mode": ctx.mode.value,
                "turn": ctx.turn,
                "last_parsed_dict": ctx.last_parsed or {},
                "last_intent": ctx.last_intent or "",
                "last_item_ids": ctx.last_item_ids if ctx.mode == ConversationMode.FULL else [],
            }
        )

        parsed = out.get("parsed") or None
        result = out.get("result") or {}
        answer = out.get("answer", "")

        if parsed is not None and getattr(parsed, "intent", None) != EmployerChatbotIntent.HELP:
            ctx.last_parsed = parsed.model_dump(mode="json")
            ctx.last_intent = parsed.intent.value

            if ctx.mode == ConversationMode.FULL and parsed.intent == EmployerChatbotIntent.LIST_APPLICATIONS:
                items = result.get("items") or []
                ctx.last_item_ids = [int(it["application_id"]) for it in items if it.get("application_id") is not None]

        now = datetime.now(tz=KST).isoformat()
        ctx.transcript.append(TranscriptItem(role="user", content=message, at=now))
        ctx.transcript.append(TranscriptItem(role="assistant", content=answer[:4000], at=now))
        if len(ctx.transcript) > self.max_transcript_items:
            ctx.transcript = ctx.transcript[-self.max_transcript_items:]

        ctx.updated_at = now
        await self.memory.save(ctx)

        return {
            "request_id": out.get("request_id", rid),
            "conversation_id": cid,
            "turn": ctx.turn,
            "mode": ctx.mode.value,
            "answer": answer,
            "parsed": parsed,
            "result": result,
        }


employer_chatbot_agent = EmployerStatsChatbot()
