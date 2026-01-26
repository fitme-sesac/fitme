# app/chatbot/services/agent.py
from __future__ import annotations

import re
import logging
import uuid
from datetime import date, datetime,timedelta
from typing import Any, Dict, Optional, TypedDict, List

from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI
from langgraph.graph import END, StateGraph
from zoneinfo import ZoneInfo

from app.chatbot.repository import job_stats_repo
from app.chatbot.schemas import ChatbotIntent, ChatbotParsedSpec
from app.chatbot.services.memory import (
    RedisChatbotMemoryStore,
    ChatbotConversationState,
    ConversationMode,
    TranscriptItem,
)
from app.chatbot.utils.keywords import normalize_keyword_list
from app.chatbot.utils.location import infer_admin_areas_from_text, infer_regions_from_text
from app.chatbot.utils.stack_detect import extract_stack_candidates  # ✅ 후속질문에서 스택 후보 추출
from app.core.config import settings
from app.core.redis import get_redis

_MONTH_RE = re.compile(r"(?:(\d{4})\s*년\s*)?(\d{1,2})\s*월(?:\s*(\d{1,2})\s*일)?")
_THIS_MONTH = ("이번달", "이번 달")
_LAST_MONTH = ("지난달", "지난 달", "저번달", "저번 달")
_NEXT_MONTH = ("다음달", "다음 달")

def _first_day(y: int, m: int) -> date:
    return date(y, m, 1)

def _add_month(y: int, m: int, delta: int) -> tuple[int, int]:
    m2 = m + delta
    y2 = y + (m2 - 1) // 12
    m2 = (m2 - 1) % 12 + 1
    return y2, m2

def _infer_range_from_text(msg: str) -> tuple[date | None, date | None]:
    if not msg:
        return None, None
    t = msg.strip()
    today = _today_kst()

    # relative months
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

    # explicit "YYYY년 M월 (D일)"
    m = _MONTH_RE.search(t)
    if not m:
        return None, None

    year = int(m.group(1) or today.year)  # ✅ 연도 없으면 올해로 가정
    month = int(m.group(2))
    day = m.group(3)

    if day:
        s = date(year, month, int(day))
        e = s + timedelta(days=1)
        return s, e

    s = date(year, month, 1)
    y2, m2 = _add_month(year, month, 1)
    e = date(y2, m2, 1)
    return s, e

logger = logging.getLogger(__name__)
KST = ZoneInfo("Asia/Seoul")


class ChatbotState(TypedDict, total=False):
    request_id: str
    conversation_id: str
    conversation_mode: str
    turn: int

    message: str
    parsed: ChatbotParsedSpec
    result: Dict[str, Any]
    answer: str

    last_parsed_dict: Dict[str, Any]
    last_intent: str
    last_item_ids: List[int]
    scope_job_ids: List[int]


_FOLLOWUP_MARKERS = ("그 중", "그중", "그 중에", "그중에", "그러면", "그럼", "거기서", "방금", "이전")


def _is_followup(msg: str) -> bool:
    return bool(msg) and any(m in msg for m in _FOLLOWUP_MARKERS)


def _today_kst() -> date:
    return datetime.now(tz=KST).date()


def _default_range(_: ChatbotIntent) -> tuple[date, date]:
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


def _clamp_limit(n: Optional[int], default: int = 5) -> int:
    try:
        x = int(n) if n is not None else default
    except Exception:
        x = default
    return max(1, min(20, x))


def _infer_intent_by_rule(msg: str) -> Optional[ChatbotIntent]:
    """
    LLM이 HELP로 도망가거나 confidence가 낮을 때, 최소한의 룰로 intent를 구제한다.
    """
    if not msg:
        return None
    ml = msg.lower()

    if any(k in msg for k in ("보여줘", "목록", "리스트", "최신", "랜덤")):
        return ChatbotIntent.LIST_POSTINGS

    if any(k in msg for k in ("몇개", "몇 개", "몇건", "몇 건", "건수", "공고 수", "공고수")):
        return ChatbotIntent.COUNT_POSTINGS

    if ("스택" in msg) and any(k in msg for k in ("많이", "상위", "제일", "top")):
        return ChatbotIntent.TOP_STACKS

    if "경쟁률" in msg:
        return ChatbotIntent.COMPETITION

    return None


def _coerce_last(last_parsed_dict: Dict[str, Any]) -> Optional[ChatbotParsedSpec]:
    try:
        return ChatbotParsedSpec.model_validate(last_parsed_dict or {})
    except Exception:
        return None


def _display_stack(token: str) -> str:
    """
    canonical(대개 lowercase)을 사용자에게 보여줄 형태로 변환.
    전부 맵핑하기 어렵기 때문에, 최소 특수케이스 + 일반 title-case.
    """
    t = (token or "").strip()
    tl = t.lower()

    if tl == "java":
        return "JAVA"
    if tl == "python":
        return "Python"
    if tl == "javascript":
        return "JavaScript"
    if tl == "typescript":
        return "TypeScript"

    # 다단어 토큰: spring boot -> Spring Boot
    if " " in tl:
        return " ".join(w[:1].upper() + w[1:] if w else w for w in tl.split(" "))

    # 기본: 첫 글자만 대문자
    return tl[:1].upper() + tl[1:] if tl else t


def _pick_stack_typo_note(stack_corrections: List[dict]) -> Optional[str]:
    """
    stack_alias(동의어) 매핑은 조용히 처리하고,
    stack_vocab(trgm) 기반 "오타 추정"일 때만 안내 문구를 만든다.
    """
    for c in (stack_corrections or []):
        via = str(c.get("via") or "")
        if not via.startswith("vocab_trgm"):
            continue
        frm = c.get("from")
        to = c.get("to")
        if not frm or not to:
            continue
        sim = c.get("sim")
        sim_txt = ""
        if isinstance(sim, (float, int)):
            sim_txt = f" (유사도 {float(sim):.2f})"
        return (
            f"입력한 기술스택 '{frm}'은(는) '{_display_stack(str(to))}'로 해석했습니다{sim_txt}.\n"
            f"만약 다른 기술스택을 찾는 거라면 기술스택을 다시 입력해주세요."
        )
    return None


class JobStatsChatbot:
    def __init__(self):
        chatbot_model = getattr(settings, "CHATBOT_OPENAI_MODEL_NAME", None) or settings.OPENAI_MODEL_NAME
        chatbot_temp = float(getattr(settings, "CHATBOT_OPENAI_TEMPERATURE", 0.0))

        self.llm = ChatOpenAI(
            model=chatbot_model,
            temperature=chatbot_temp,
            openai_api_key=settings.OPENAI_API_KEY,
        )

        self.parser = PydanticOutputParser(pydantic_object=ChatbotParsedSpec)
        self.graph = self._build_graph()

        ttl = int(getattr(settings, "CHATBOT_CONVERSATION_TTL_SECONDS", 604800))
        self.memory = RedisChatbotMemoryStore(get_redis(), ttl_seconds=ttl)

        self.max_full_turns = int(getattr(settings, "CHATBOT_MAX_FULL_TURNS", 10))
        self.max_transcript_items = int(getattr(settings, "CHATBOT_MAX_TRANSCRIPT_ITEMS", 200))

    def _build_graph(self):
        sg = StateGraph(ChatbotState)
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

    async def _parse(self, state: ChatbotState) -> Dict[str, Any]:
        msg = state.get("message", "")
        today_d = _today_kst()
        today = today_d.isoformat()
        year = today_d.year

        system = """[Role]
너는 취업 공고 통계용 챗봇의 '질문 파서(parser)'다.
출력은 서버가 DB 조회 파라미터로만 사용한다.

[Rules]
- 추측 금지.
- intent는 가능하면 반드시 COUNT/LIST/COMPETITION/TOP_STACKS 중 하나로 선택하라.
- intent 자체가 전혀 판단되지 않을 때만 HELP.
- 날짜는 YYYY-MM-DD. end_date는 exclusive(미포함).
- 날짜/필터가 불확실하면 비워두고(Null/empty), validate 단계에서 기본값/상속이 처리된다.
- keywords_all: AND, keywords_any: OR
- regions_any: 광역 코드(OR):
  SEOUL,BUSAN,DAEGU,INCHEON,GWANGJU,DAEJEON,ULSAN,SEJONG,
  GYEONGGI,GANGWON,CHUNGBUK,CHUNGNAM,JEONBUK,JEONNAM,
  GYEONGBUK,GYEONGNAM,JEJU
- admin_areas_any: 시/군/구(OR) 예: 강남구, 수원시, 성남시, 기장군
- min_salary_m만원: 예) '연봉 4천 이상' => 4000
- job_role: 자유 문자열(예: Backend, Frontend, Data Engineer 등). 확장 가능.
- limit: 1..20
- random: LIST_POSTINGS에서 랜덤 여부

[Canonicalization]
- 기술스택/직군/툴 이름은 가능한 표준 표기로 정규화하라.
  예: "스프링부트", "spring boot" -> "Spring Boot"
      "js" -> "JavaScript"
  확신이 없으면 사용자가 쓴 원문을 그대로 사용.

[Multi-turn]
- 사용자가 '그 중에/그러면/거기서'처럼 후속 질문을 하면,
  [Conversation context]의 이전 parsed spec을 참고해 누락 필드를 채울 수 있다.
  그래도 불확실하면 HELP.

[Supported intents]
1) COUNT_POSTINGS
2) COMPETITION
3) LIST_POSTINGS (최신/랜덤)
4) TOP_STACKS
5) HELP
"""

        examples = f"""[Today]
{today}

[Examples]
- '오늘 공고 몇개 올라왔어?' => COUNT_POSTINGS, start_date=today, end_date=today+1
- '올해 8월 공고 몇개' => COUNT_POSTINGS, start_date={year}-08-01, end_date={year}-09-01
- '백엔드 자바/스프링부트 공고 5개 최신' => LIST_POSTINGS, job_role='Backend', keywords_all=['Java','Spring Boot'], limit=5, random=false
- '서울 강남구 연봉 4천 이상 공고 5개 랜덤' => LIST_POSTINGS, regions_any=['SEOUL'], admin_areas_any=['강남구'], min_salary_m만원=4000, limit=5, random=true
"""

        context = state.get("last_parsed_dict") or None
        context_block = ""
        if context:
            context_block = "[Conversation context]\nPrevious parsed spec:\n" + str(context) + "\n"

        prompt = ChatPromptTemplate.from_messages(
            [
                ("system", system + "\n\n{format_instructions}"),
                ("user", examples + "\n\n" + context_block + "\n[User question]\n{message}"),
            ]
        ).partial(format_instructions=self.parser.get_format_instructions())

        chain = prompt | self.llm | self.parser

        try:
            parsed: ChatbotParsedSpec = await chain.ainvoke({"message": msg})
            return {"parsed": parsed}
        except Exception:
            logger.exception("chatbot parse failed (1st)")
            try:
                parsed: ChatbotParsedSpec = await chain.ainvoke({"message": msg})
                return {"parsed": parsed}
            except Exception:
                logger.exception("chatbot parse failed (2nd)")
                return {"parsed": ChatbotParsedSpec(intent=ChatbotIntent.HELP, confidence=0.0)}

    async def _validate(self, state: ChatbotState) -> Dict[str, Any]:
        parsed = state.get("parsed") or ChatbotParsedSpec(intent=ChatbotIntent.HELP, confidence=0.0)
        msg = state.get("message", "") or ""

        last = _coerce_last(state.get("last_parsed_dict") or {})
        is_follow = _is_followup(msg) and (last is not None)

        # 1) follow-up에서 HELP/저신뢰면 intent 구제 (룰 → last.intent)
        if is_follow and (parsed.intent == ChatbotIntent.HELP or parsed.confidence < 0.45):
            fb = _infer_intent_by_rule(msg)
            if fb is not None:
                parsed.intent = fb
                parsed.confidence = max(parsed.confidence, 0.7)
            else:
                # last parsed intent 승계(가능하면)
                try:
                    parsed.intent = last.intent
                    parsed.confidence = max(parsed.confidence, 0.7)
                except Exception:
                    pass

        # 2) confidence gate (follow-up은 완화)
        thr = 0.45 if not is_follow else 0.25
        if parsed.confidence < thr:
            parsed.intent = ChatbotIntent.HELP

        if parsed.intent == ChatbotIntent.HELP:
            return {"parsed": parsed, "result": {"keyword_corrections": [], "stack_candidates": []}}

        # 3) follow-up이면 last_parsed에서 기본 상속 (먼저 상속)
        if is_follow and last is not None:
            parsed.start_date = parsed.start_date or last.start_date
            parsed.end_date = parsed.end_date or last.end_date

            if not parsed.regions_any:
                parsed.regions_any = list(last.regions_any)
            if not parsed.admin_areas_any:
                parsed.admin_areas_any = list(last.admin_areas_any)

            if parsed.job_role is None:
                parsed.job_role = last.job_role
            if parsed.min_salary_m만원 is None:
                parsed.min_salary_m만원 = last.min_salary_m만원

            if not parsed.keywords_all:
                parsed.keywords_all = list(last.keywords_all)
            if not parsed.keywords_any:
                parsed.keywords_any = list(last.keywords_any)

        # 4) 날짜: 메시지에 월/일이 있으면 follow-up에서도 무조건 override
        s2, e2 = _infer_range_from_text(msg)
        if s2 and e2:
            parsed.start_date, parsed.end_date = s2, e2

        # 5) 그래도 없으면 default_range
        if parsed.start_date is None or parsed.end_date is None:
            s, e = _default_range(parsed.intent)
            parsed.start_date = parsed.start_date or s
            parsed.end_date = parsed.end_date or e
        parsed.start_date, parsed.end_date = _clamp_range(parsed.start_date, parsed.end_date)

        # 6) follow-up에서 스택 후보 추출 (1번만)
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
                    # OR로 확장
                    for s in stack_cands:
                        if s not in parsed.keywords_any:
                            parsed.keywords_any.append(s)
                else:
                    # 기본은 AND로 “추가 필터”(그중에서 파이썬 = 좁히기)
                    for s in stack_cands:
                        if s not in parsed.keywords_all:
                            parsed.keywords_all.append(s)

        # 7) 지역/행정구역 룰 추론
        if not parsed.regions_any:
            parsed.regions_any = infer_regions_from_text(msg)
        if not parsed.admin_areas_any and parsed.regions_any:
            parsed.admin_areas_any = infer_admin_areas_from_text(msg, parsed.regions_any)

        # 8) 키워드 split/dedupe
        all_norm, corr_all = normalize_keyword_list(parsed.keywords_all, max_items=30)
        any_norm, corr_any = normalize_keyword_list(parsed.keywords_any, max_items=30)
        parsed.keywords_all = all_norm
        parsed.keywords_any = any_norm

        # 9) limit
        default_limit = int(getattr(settings, "CHATBOT_DEFAULT_RESULT_LIMIT", 5))
        parsed.limit = _clamp_limit(parsed.limit, default=default_limit)

        # 10) LIST_POSTINGS random default
        if parsed.intent != ChatbotIntent.LIST_POSTINGS:
            parsed.random = None
        elif parsed.random is None:
            ml = msg.lower()
            parsed.random = ("랜덤" in msg) or ("무작위" in msg) or ("random" in ml)

        return {
            "parsed": parsed,
            "result": {
                "keyword_corrections": (corr_all + corr_any)[:20],
                "stack_candidates": (stack_cands[:20] if is_follow else []),
            },
        }


    async def _execute(self, state: ChatbotState) -> Dict[str, Any]:
        parsed: ChatbotParsedSpec = state["parsed"]
        rid = state.get("request_id") or str(uuid.uuid4())
        base = state.get("result") or {}

        if parsed.intent == ChatbotIntent.COUNT_POSTINGS:
            r = job_stats_repo.count_postings(
                parsed.start_date,
                parsed.end_date,
                regions_any=parsed.regions_any,
                admin_areas_any=parsed.admin_areas_any,
                keywords_all=parsed.keywords_all,
                keywords_any=parsed.keywords_any,
                job_role=parsed.job_role,
                min_salary_m만원=parsed.min_salary_m만원,
            )
            base.update(r)
            return {"result": base}

        if parsed.intent == ChatbotIntent.COMPETITION:
            r = job_stats_repo.competition(
                parsed.start_date,
                parsed.end_date,
                regions_any=parsed.regions_any,
                admin_areas_any=parsed.admin_areas_any,
                keywords_all=parsed.keywords_all,
                keywords_any=parsed.keywords_any,
                job_role=parsed.job_role,
            )
            base.update(r)
            return {"result": base}

        if parsed.intent == ChatbotIntent.LIST_POSTINGS:
            r = job_stats_repo.list_postings(
                parsed.start_date,
                parsed.end_date,
                request_id=rid,
                regions_any=parsed.regions_any,
                admin_areas_any=parsed.admin_areas_any,
                keywords_all=parsed.keywords_all,
                keywords_any=parsed.keywords_any,
                job_role=parsed.job_role,
                min_salary_m만원=parsed.min_salary_m만원,
                limit=int(parsed.limit or 5),
                random=bool(parsed.random),
            )
            base.update(r)
            return {"result": base}

        if parsed.intent == ChatbotIntent.TOP_STACKS:
            top_default = int(getattr(settings, "CHATBOT_TOP_STACKS_DEFAULT_LIMIT", 10))
            lim = int(parsed.limit or top_default)
            r = job_stats_repo.top_stacks(
                parsed.start_date,
                parsed.end_date,
                regions_any=parsed.regions_any,
                admin_areas_any=parsed.admin_areas_any,
                job_role=parsed.job_role,
                limit=lim,
            )
            base.update(r)
            return {"result": base}

        return {"result": base}

    async def _answer(self, state: ChatbotState) -> Dict[str, Any]:
        rid = state.get("request_id") or str(uuid.uuid4())
        parsed: ChatbotParsedSpec = state.get("parsed") or ChatbotParsedSpec(intent=ChatbotIntent.HELP, confidence=0.0)
        result = state.get("result") or {}

        if parsed.intent == ChatbotIntent.HELP:
            answer = (
                "지원 예시:\n"
                "- 오늘 공고 몇개 올라왔어?\n"
                "- 서울 강남구 연봉 4천 이상 공고 5개 랜덤\n"
                "- 8월 백엔드 자바 최신 5개\n"
                "- 요즘 올라오는 공고에서 제일 많이 요구하는 스택\n"
            )
            return {"answer": answer, "request_id": rid}

        start = result.get("start_date")
        end = result.get("end_date")

        def filters_summary() -> str:
            parts = []
            if result.get("regions_any"):
                parts.append(f"광역: {', '.join(result['regions_any'])}")
            if result.get("admin_areas_any"):
                parts.append(f"세부: {', '.join(result['admin_areas_any'])}")
            if result.get("job_role"):
                parts.append(f"직군: {result['job_role']}")
            if result.get("keywords_all"):
                parts.append(f"AND: {', '.join(result['keywords_all'])}")
            if result.get("keywords_any"):
                parts.append(f"OR: {', '.join(result['keywords_any'])}")
            if result.get("min_salary_m만원") is not None:
                parts.append(f"최소연봉: {result['min_salary_m만원']}만원")
            return (" | ".join(parts)) if parts else "필터: 없음"

        typo_note = _pick_stack_typo_note(result.get("stack_corrections") or [])

        if parsed.intent == ChatbotIntent.COUNT_POSTINGS:
            cnt = int(result.get("count", 0))
            body = f"{start} ~ {end} 공고 수: {cnt}개\n{filters_summary()}"
            answer = (typo_note + "\n" + body) if typo_note else body

        elif parsed.intent == ChatbotIntent.COMPETITION:
            postings = int(result.get("postings", 0))
            applications = int(result.get("applications", 0))
            avg = float(result.get("avg_apply_per_posting", 0.0))
            body = (
                f"{start} ~ {end} 경쟁률:\n"
                f"- 공고 수: {postings}개\n"
                f"- 총 지원 수: {applications}건\n"
                f"- 공고 1개당 평균 지원 수: {avg:.2f}건\n"
                f"{filters_summary()}"
            )
            answer = (typo_note + "\n" + body) if typo_note else body

        elif parsed.intent == ChatbotIntent.LIST_POSTINGS:
            items = result.get("items") or []
            lim = int(result.get("limit", 5))
            rnd = bool(result.get("random"))
            lines = [
                f"{start} ~ {end} 공고 {len(items)}개 (limit={lim}, {'랜덤' if rnd else '최신'}):",
                filters_summary(),
            ]
            for i, it in enumerate(items, 1):
                lines.append(
                    f"{i}. [{it.get('employer_name')}] {it.get('title')} / {it.get('location') or '-'} "
                    f"/ {it.get('salary_text') or '-'} / stack={it.get('stack') or '-'} "
                    f"/ apply={it.get('apply_count', 0)} / created_at={it.get('created_at') or '-'} "
                    f"(job_id={it.get('job_id')})"
                )
            body = "\n".join(lines)
            answer = (typo_note + "\n" + body) if typo_note else body

        elif parsed.intent == ChatbotIntent.TOP_STACKS:
            items = result.get("items") or []
            lim = int(result.get("limit", 10))
            lines = [f"{start} ~ {end} TOP 스택 (상위 {lim}):", filters_summary()]
            for i, it in enumerate(items, 1):
                lines.append(f"{i}. {it['stack']} ({it['count']}회)")
            body = "\n".join(lines)
            answer = (typo_note + "\n" + body) if typo_note else body

        else:
            answer = "지원하지 않는 질문이다."

        return {"answer": answer, "request_id": rid}

    async def ask(self, message: str, request_id: Optional[str] = None, conversation_id: Optional[str] = None) -> Dict[str, Any]:
        rid = request_id or str(uuid.uuid4())
        cid = conversation_id or str(uuid.uuid4())

        ctx = await self.memory.load(cid)
        if ctx is None:
            ctx = ChatbotConversationState(conversation_id=cid)

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

        if parsed is not None and getattr(parsed, "intent", None) != ChatbotIntent.HELP:
            ctx.last_parsed = parsed.model_dump(mode="json")
            ctx.last_intent = parsed.intent.value

            if ctx.mode == ConversationMode.FULL and parsed.intent == ChatbotIntent.LIST_POSTINGS:
                items = result.get("items") or []
                ctx.last_item_ids = [int(it["job_id"]) for it in items if it.get("job_id") is not None]

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


chatbot_agent = JobStatsChatbot()
