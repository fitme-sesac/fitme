# app/chatbot/services/agent.py (핵심 변경판)
from __future__ import annotations

import logging
import re
import uuid
from datetime import date, datetime, timedelta
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
from app.core.config import settings
from app.core.redis import get_redis

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


def _default_range(intent: ChatbotIntent) -> tuple[date, date]:
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


class JobStatsChatbot:
    def __init__(self):
        self.llm = ChatOpenAI(
            model=settings.OPENAI_MODEL_NAME,
            temperature=0,
            openai_api_key=settings.OPENAI_API_KEY,
        )
        self.parser = PydanticOutputParser(pydantic_object=ChatbotParsedSpec)
        self.graph = self._build_graph()

        # ✅ 7일 TTL
        ttl = int(getattr(settings, "CHATBOT_CONVERSATION_TTL_SECONDS", 604800))
        self.memory = RedisChatbotMemoryStore(get_redis(), ttl_seconds=ttl)

        # ✅ FULL 모드 10턴
        self.max_full_turns = int(getattr(settings, "CHATBOT_MAX_FULL_TURNS", 10))

        # transcript cap
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

        # ✅ 스택/직군 동의어 사전 금지 → LLM이 canonicalize 하도록 지시
        system = """[Role]
너는 취업 공고 통계용 챗봇의 '질문 파서(parser)'다.
출력은 서버가 DB 조회 파라미터로만 사용한다.

[Rules]
- 추측 금지. 불확실하면 intent=HELP.
- 날짜는 YYYY-MM-DD. end_date는 exclusive(미포함).
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
            logger.exception("chatbot parse failed")
            return {"parsed": ChatbotParsedSpec(intent=ChatbotIntent.HELP, confidence=0.0)}

    async def _validate(self, state: ChatbotState) -> Dict[str, Any]:
        parsed = state.get("parsed") or ChatbotParsedSpec(intent=ChatbotIntent.HELP, confidence=0.0)
        msg = state.get("message", "")

        # confidence gate
        if parsed.confidence < 0.45:
            parsed.intent = ChatbotIntent.HELP

        # 날짜 기본값/클램프
        if parsed.intent != ChatbotIntent.HELP:
            if parsed.start_date is None or parsed.end_date is None:
                s, e = _default_range(parsed.intent)
                parsed.start_date = parsed.start_date or s
                parsed.end_date = parsed.end_date or e
            parsed.start_date, parsed.end_date = _clamp_range(parsed.start_date, parsed.end_date)

            # limit
            default_limit = int(getattr(settings, "CHATBOT_DEFAULT_RESULT_LIMIT", 5))
            parsed.limit = _clamp_limit(parsed.limit, default=default_limit)

            # 지역/행정구역 룰 추론(이건 도메인 고정이라 유지해도 확장성 문제 없음)
            if not parsed.regions_any:
                parsed.regions_any = infer_regions_from_text(msg)
            if not parsed.admin_areas_any and parsed.regions_any:
                parsed.admin_areas_any = infer_admin_areas_from_text(msg, parsed.regions_any)

            # ✅ 스택 정규화는 LLM에서 했다고 가정, 여기서는 일반 split/dedupe만 수행
            all_norm, corr_all = normalize_keyword_list(parsed.keywords_all, max_items=30)
            any_norm, corr_any = normalize_keyword_list(parsed.keywords_any, max_items=30)
            parsed.keywords_all = all_norm
            parsed.keywords_any = any_norm

            # LIST_POSTINGS random default
            if parsed.intent != ChatbotIntent.LIST_POSTINGS:
                parsed.random = None
            elif parsed.random is None:
                ml = msg.lower()
                parsed.random = ("랜덤" in msg) or ("무작위" in msg) or ("random" in ml)

            return {"parsed": parsed, "result": {"keyword_corrections": (corr_all + corr_any)[:20]}}

        return {"parsed": parsed, "result": {"keyword_corrections": []}}

    async def _execute(self, state: ChatbotState) -> Dict[str, Any]:
        parsed: ChatbotParsedSpec = state["parsed"]
        rid = state.get("request_id") or str(uuid.uuid4())
        base = state.get("result") or {}

        if parsed.intent == ChatbotIntent.COUNT_POSTINGS:
            r = job_stats_repo.count_postings(
                parsed.start_date, parsed.end_date,
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
                parsed.start_date, parsed.end_date,
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
                parsed.start_date, parsed.end_date,
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
                parsed.start_date, parsed.end_date,
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

        if parsed.intent == ChatbotIntent.COUNT_POSTINGS:
            cnt = int(result.get("count", 0))
            answer = f"{start} ~ {end} 공고 수: {cnt}개\n{filters_summary()}"

        elif parsed.intent == ChatbotIntent.COMPETITION:
            postings = int(result.get("postings", 0))
            applications = int(result.get("applications", 0))
            avg = float(result.get("avg_apply_per_posting", 0.0))
            answer = (
                f"{start} ~ {end} 경쟁률:\n"
                f"- 공고 수: {postings}개\n"
                f"- 총 지원 수: {applications}건\n"
                f"- 공고 1개당 평균 지원 수: {avg:.2f}건\n"
                f"{filters_summary()}"
            )

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
            answer = "\n".join(lines)

        elif parsed.intent == ChatbotIntent.TOP_STACKS:
            items = result.get("items") or []
            lim = int(result.get("limit", 10))
            lines = [f"{start} ~ {end} TOP 스택 (상위 {lim}):", filters_summary()]
            for i, it in enumerate(items, 1):
                lines.append(f"{i}. {it['stack']} ({it['count']}회)")
            answer = "\n".join(lines)

        else:
            answer = "지원하지 않는 질문이다."

        return {"answer": answer, "request_id": rid}

    async def ask(self, message: str, request_id: Optional[str] = None, conversation_id: Optional[str] = None) -> Dict[str, Any]:
        rid = request_id or str(uuid.uuid4())
        cid = conversation_id or str(uuid.uuid4())

        ctx = await self.memory.load(cid)
        if ctx is None:
            ctx = ChatbotConversationState(conversation_id=cid)

        # ✅ FULL 10턴 이후 SUMMARY로 전환
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

        # ✅ memory 업데이트
        if parsed is not None and getattr(parsed, "intent", None) != ChatbotIntent.HELP:
            ctx.last_parsed = parsed.model_dump(mode="json")
            ctx.last_intent = parsed.intent.value

            if ctx.mode == ConversationMode.FULL and parsed.intent == ChatbotIntent.LIST_POSTINGS:
                items = result.get("items") or []
                ctx.last_item_ids = [int(it["job_id"]) for it in items if it.get("job_id") is not None]

        # ✅ transcript 저장(7일 보관)
        now = datetime.now(tz=KST).isoformat()
        ctx.transcript.append(TranscriptItem(role="user", content=message, at=now))
        ctx.transcript.append(TranscriptItem(role="assistant", content=answer[:4000], at=now))  # 과도한 저장 방지
        if len(ctx.transcript) > self.max_transcript_items:
            ctx.transcript = ctx.transcript[-self.max_transcript_items :]

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
