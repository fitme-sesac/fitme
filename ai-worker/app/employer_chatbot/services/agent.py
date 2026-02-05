# app/employer_chatbot/services/agent.py
"""
기업용 채용 AI 챗봇 에이전트
- LangGraph 기반 상태 머신
- 모듈화된 구조로 유지보수 용이
"""
from __future__ import annotations

import logging
import uuid
from datetime import date, datetime, timedelta
from typing import Any, Dict, List, Optional, TypedDict

from langchain_core.output_parsers import PydanticOutputParser
from langchain_openai import ChatOpenAI
from langgraph.graph import END, StateGraph
from zoneinfo import ZoneInfo

from app.employer_chatbot.repository import employer_stats_repo
from app.employer_chatbot.schemas import EmployerChatbotIntent, EmployerChatbotParsedSpec
from app.employer_chatbot.services.date_range import (
    today_kst,
    infer_range_from_text,
    default_range,
    clamp_range,
    clamp_limit,
)
from app.employer_chatbot.services.memory import (
    EmployerChatbotConversationState,
    ConversationMode,
    RedisEmployerChatbotMemoryStore,
    TranscriptItem,
)
from app.employer_chatbot.services.prompts import parse_message
from app.employer_chatbot.services.render import render_response
from app.core.config import settings
from app.core.redis import get_redis

logger = logging.getLogger(__name__)
KST = ZoneInfo("Asia/Seoul")


# =============================================================================
# 후속 질문 감지
# =============================================================================

_FOLLOWUP_MARKERS = (
    "그 중", "그중", "그 중에", "그중에",
    "그러면", "그럼", "거기서", "방금",
    "이전", "그 지원자", "해당", "그 공고",
    "그 사람", "그 중에서", "거기에서",
)


def _is_followup(msg: str) -> bool:
    """후속 질문 여부 판단"""
    return bool(msg) and any(m in msg for m in _FOLLOWUP_MARKERS)


def _coerce_last(last_parsed_dict: Dict[str, Any]) -> Optional[EmployerChatbotParsedSpec]:
    """이전 파싱 결과 변환"""
    try:
        return EmployerChatbotParsedSpec.model_validate(last_parsed_dict or {})
    except Exception:
        return None


# =============================================================================
# 규칙 기반 인텐트 추론 (fallback)
# =============================================================================

def _infer_intent_by_rule(msg: str) -> Optional[EmployerChatbotIntent]:
    """규칙 기반 인텐트 추론 (LLM 실패 시 fallback)"""
    if not msg:
        return None
    
    ml = msg.lower()
    
    # 지원자 목록
    if any(k in msg for k in ("목록", "리스트", "보여줘", "알려줘", "누구")):
        if any(k in msg for k in ("지원자", "지원", "응시자")):
            return EmployerChatbotIntent.LIST_APPLICATIONS
    
    # 지원자 수
    if any(k in msg for k in ("몇명", "몇 명", "몇건", "몇 건", "지원자 수", "지원수", "카운트")):
        return EmployerChatbotIntent.COUNT_APPLICATIONS
    
    # 통계/분석
    if any(k in msg for k in ("통계", "분석", "현황", "상태별", "분포")):
        return EmployerChatbotIntent.APPLICATION_STATS
    
    # 추이
    if any(k in msg for k in ("추이", "변화", "트렌드", "추세")):
        return EmployerChatbotIntent.APPLICANT_TREND
    
    # 공고 성과
    if any(k in msg for k in ("공고 성과", "성과", "퍼포먼스", "인기")):
        return EmployerChatbotIntent.POSTING_PERFORMANCE
    
    # 공고 비교
    if any(k in msg for k in ("비교", "vs", "대비")):
        return EmployerChatbotIntent.COMPARE_POSTINGS
    
    # 스킬 상위
    if any(k in msg for k in ("스킬", "기술", "역량")):
        if any(k in msg for k in ("많이", "상위", "탑", "top", "인기")):
            return EmployerChatbotIntent.TOP_SKILLS
    
    # 프로필 분석
    if any(k in msg for k in ("프로필", "평균 경력", "경력 분포")):
        return EmployerChatbotIntent.APPLICANT_PROFILE
    
    # 전환율
    if any(k in msg for k in ("전환율", "합격률", "퍼널", "conversion")):
        return EmployerChatbotIntent.CONVERSION_RATE
    
    # 긴급
    if any(k in msg for k in ("긴급", "급한", "urgent", "처리해야")):
        return EmployerChatbotIntent.URGENT_ACTIONS
    
    # 대기 검토
    if any(k in msg for k in ("대기", "pending", "검토 안", "미검토")):
        return EmployerChatbotIntent.PENDING_REVIEW
    
    return None


# =============================================================================
# 상태 타입
# =============================================================================

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


# =============================================================================
# 메인 챗봇 클래스
# =============================================================================

class EmployerStatsChatbot:
    """기업용 채용 AI 챗봇"""
    
    def __init__(self):
        # LLM 설정
        chatbot_model = getattr(settings, "CHATBOT_OPENAI_MODEL_NAME", None) or settings.OPENAI_MODEL_NAME
        chatbot_temp = float(getattr(settings, "CHATBOT_OPENAI_TEMPERATURE", 0.0))
        
        self.llm = ChatOpenAI(
            model=chatbot_model,
            temperature=chatbot_temp,
            openai_api_key=settings.OPENAI_API_KEY,
        )
        
        self.parser = PydanticOutputParser(pydantic_object=EmployerChatbotParsedSpec)
        self.graph = self._build_graph()
        
        # 메모리 설정
        ttl = int(getattr(settings, "CHATBOT_CONVERSATION_TTL_SECONDS", 604800))
        self.memory = RedisEmployerChatbotMemoryStore(get_redis(), ttl_seconds=ttl)
        
        # 제한 설정
        self.max_full_turns = int(getattr(settings, "CHATBOT_MAX_FULL_TURNS", 10))
        self.max_transcript_items = int(getattr(settings, "CHATBOT_MAX_TRANSCRIPT_ITEMS", 200))
        self.default_lookback_days = int(getattr(settings, "CHATBOT_DEFAULT_LOOKBACK_DAYS", 30))
        self.max_range_days = int(getattr(settings, "CHATBOT_MAX_RANGE_DAYS", 365))
        self.default_limit = int(getattr(settings, "CHATBOT_DEFAULT_RESULT_LIMIT", 10))
    
    def _build_graph(self):
        """LangGraph 상태 머신 구성"""
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
    
    # -------------------------------------------------------------------------
    # Parse 노드
    # -------------------------------------------------------------------------
    async def _parse(self, state: EmployerChatbotState) -> Dict[str, Any]:
        """LLM 기반 메시지 파싱"""
        msg = state.get("message", "")
        last_parsed_dict = state.get("last_parsed_dict")
        today = today_kst()
        
        parsed = await parse_message(
            self.llm,
            self.parser,
            message=msg,
            last_parsed_dict=last_parsed_dict,
            today=today,
        )
        
        return {"parsed": parsed}
    
    # -------------------------------------------------------------------------
    # Validate 노드
    # -------------------------------------------------------------------------
    async def _validate(self, state: EmployerChatbotState) -> Dict[str, Any]:
        """파싱 결과 검증 및 보정"""
        parsed = state.get("parsed") or EmployerChatbotParsedSpec(
            intent=EmployerChatbotIntent.HELP, confidence=0.0
        )
        msg = state.get("message", "") or ""
        
        last = _coerce_last(state.get("last_parsed_dict") or {})
        is_follow = _is_followup(msg) and (last is not None)
        
        # 1) 후속 질문에서 HELP/저신뢰면 규칙 기반으로 복구
        if is_follow and (parsed.intent == EmployerChatbotIntent.HELP or parsed.confidence < 0.45):
            fb = _infer_intent_by_rule(msg)
            if fb is not None:
                parsed.intent = fb
                parsed.confidence = max(parsed.confidence, 0.7)
            elif last is not None:
                try:
                    parsed.intent = last.intent
                    parsed.confidence = max(parsed.confidence, 0.7)
                except Exception:
                    pass
        
        # 2) 신뢰도 임계값 체크
        threshold = 0.25 if is_follow else 0.45
        if parsed.confidence < threshold:
            parsed.intent = EmployerChatbotIntent.HELP
        
        if parsed.intent == EmployerChatbotIntent.HELP:
            return {"parsed": parsed, "result": {}, "scope_application_ids": []}
        
        # 3) 후속 질문 필터 상속
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
        s2, e2 = infer_range_from_text(msg)
        if s2 and e2:
            parsed.start_date, parsed.end_date = s2, e2
        
        if parsed.start_date is None or parsed.end_date is None:
            s, e = default_range(self.default_lookback_days)
            parsed.start_date = parsed.start_date or s
            parsed.end_date = parsed.end_date or e
        
        parsed.start_date, parsed.end_date = clamp_range(
            parsed.start_date,
            parsed.end_date,
            self.max_range_days
        )
        
        # 5) limit 보정
        parsed.limit = clamp_limit(parsed.limit, default=self.default_limit)
        
        # 6) random 보정
        if parsed.intent != EmployerChatbotIntent.LIST_APPLICATIONS:
            parsed.random = None
        elif parsed.random is None:
            parsed.random = ("랜덤" in msg) or ("무작위" in msg) or ("random" in msg.lower())
        
        return {
            "parsed": parsed,
            "scope_application_ids": [],
            "result": {},
        }
    
    # -------------------------------------------------------------------------
    # Execute 노드
    # -------------------------------------------------------------------------
    async def _execute(self, state: EmployerChatbotState) -> Dict[str, Any]:
        """DB 조회 실행"""
        parsed: EmployerChatbotParsedSpec = state["parsed"]
        rid = state.get("request_id") or str(uuid.uuid4())
        employer_id = state.get("employer_id") or 0
        base = state.get("result") or {}
        
        # HELP는 조회 없음
        if parsed.intent == EmployerChatbotIntent.HELP:
            return {"result": base}
        
        # 각 인텐트별 실행
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
        
        elif parsed.intent == EmployerChatbotIntent.LIST_APPLICATIONS:
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
        
        elif parsed.intent == EmployerChatbotIntent.APPLICATION_STATS:
            r = employer_stats_repo.application_stats(
                employer_id=employer_id,
                start_date=parsed.start_date,
                end_date=parsed.end_date,
                job_posting_ids=parsed.job_posting_ids or None,
                job_title_keywords=parsed.job_title_keywords or None,
            )
            base.update(r)
        
        elif parsed.intent == EmployerChatbotIntent.APPLICANT_TREND:
            # 추이 분석 (Repository 메서드 추가 필요)
            r = await self._execute_applicant_trend(employer_id, parsed)
            base.update(r)
        
        elif parsed.intent == EmployerChatbotIntent.POSTING_PERFORMANCE:
            r = employer_stats_repo.posting_performance(
                employer_id=employer_id,
                start_date=parsed.start_date,
                end_date=parsed.end_date,
                job_posting_ids=parsed.job_posting_ids or None,
                limit=int(parsed.limit or 10),
            )
            base.update(r)
        
        elif parsed.intent == EmployerChatbotIntent.COMPARE_POSTINGS:
            # 공고 비교 (Repository 메서드 추가 필요)
            r = await self._execute_compare_postings(employer_id, parsed)
            base.update(r)
        
        elif parsed.intent == EmployerChatbotIntent.TOP_SKILLS:
            r = employer_stats_repo.top_skills(
                employer_id=employer_id,
                start_date=parsed.start_date,
                end_date=parsed.end_date,
                job_posting_ids=parsed.job_posting_ids or None,
                limit=int(parsed.limit or 10),
            )
            base.update(r)
        
        elif parsed.intent == EmployerChatbotIntent.APPLICANT_PROFILE:
            # 프로필 분석 (Repository 메서드 추가 필요)
            r = await self._execute_applicant_profile(employer_id, parsed)
            base.update(r)
        
        elif parsed.intent == EmployerChatbotIntent.CONVERSION_RATE:
            # 전환율 분석 (Repository 메서드 추가 필요)
            r = await self._execute_conversion_rate(employer_id, parsed)
            base.update(r)
        
        elif parsed.intent == EmployerChatbotIntent.URGENT_ACTIONS:
            # 긴급 항목 (Repository 메서드 추가 필요)
            r = await self._execute_urgent_actions(employer_id)
            base.update(r)
        
        elif parsed.intent == EmployerChatbotIntent.PENDING_REVIEW:
            # 대기 검토 (Repository 메서드 추가 필요)
            r = await self._execute_pending_review(employer_id, parsed)
            base.update(r)
        
        return {"result": base}
    
    # -------------------------------------------------------------------------
    # 신규 인텐트 실행 메서드 (기본 구현)
    # -------------------------------------------------------------------------
    async def _execute_applicant_trend(
        self,
        employer_id: int,
        parsed: EmployerChatbotParsedSpec
    ) -> Dict[str, Any]:
        """지원자 추이 분석"""
        r = employer_stats_repo.applicant_trend(
            employer_id=employer_id,
            start_date=parsed.start_date,
            end_date=parsed.end_date,
            time_unit=parsed.time_unit or "day",
            job_posting_ids=parsed.job_posting_ids or None,
        )
        return r
    
    async def _execute_compare_postings(
        self,
        employer_id: int,
        parsed: EmployerChatbotParsedSpec
    ) -> Dict[str, Any]:
        """공고 비교 분석"""
        # POSTING_PERFORMANCE를 활용
        r = employer_stats_repo.posting_performance(
            employer_id=employer_id,
            start_date=parsed.start_date,
            end_date=parsed.end_date,
            job_posting_ids=parsed.job_posting_ids or None,
            limit=int(parsed.limit or 10),
        )
        return r
    
    async def _execute_applicant_profile(
        self,
        employer_id: int,
        parsed: EmployerChatbotParsedSpec
    ) -> Dict[str, Any]:
        """지원자 프로필 분석"""
        r = employer_stats_repo.applicant_profile(
            employer_id=employer_id,
            start_date=parsed.start_date,
            end_date=parsed.end_date,
            job_posting_ids=parsed.job_posting_ids or None,
        )
        return r
    
    async def _execute_conversion_rate(
        self,
        employer_id: int,
        parsed: EmployerChatbotParsedSpec
    ) -> Dict[str, Any]:
        """전환율 분석"""
        # APPLICATION_STATS의 상태 분포 활용
        stats = employer_stats_repo.application_stats(
            employer_id=employer_id,
            start_date=parsed.start_date,
            end_date=parsed.end_date,
            job_posting_ids=parsed.job_posting_ids or None,
        )
        
        status_dist = stats.get("status_distribution") or {}
        total = int(stats.get("total_applications", 0))
        hired = int(status_dist.get("HIRED", 0))
        
        return {
            "stages": status_dist,
            "overall_conversion_rate": (hired / total * 100) if total > 0 else 0,
            "start_date": str(parsed.start_date),
            "end_date": str(parsed.end_date),
        }
    
    async def _execute_urgent_actions(self, employer_id: int) -> Dict[str, Any]:
        """긴급 처리 항목"""
        # PENDING 상태로 오래된 지원 조회
        today = today_kst()
        old_pending = employer_stats_repo.count_applications(
            employer_id=employer_id,
            start_date=today - timedelta(days=30),
            end_date=today - timedelta(days=7),
            application_status_any=["PENDING"],
        )
        
        items = []
        pending_count = int(old_pending.get("count", 0))
        if pending_count > 0:
            items.append({
                "type": "오래된 대기 지원",
                "count": pending_count,
                "description": "7일 이상 검토되지 않은 지원자가 있습니다.",
            })
        
        return {"items": items}
    
    async def _execute_pending_review(
        self,
        employer_id: int,
        parsed: EmployerChatbotParsedSpec
    ) -> Dict[str, Any]:
        """대기 검토 지원자"""
        r = employer_stats_repo.list_applications(
            employer_id=employer_id,
            start_date=parsed.start_date,
            end_date=parsed.end_date,
            request_id=str(uuid.uuid4()),
            application_status_any=["PENDING"],
            limit=int(parsed.limit or 10),
            random=False,
        )
        
        count_r = employer_stats_repo.count_applications(
            employer_id=employer_id,
            start_date=parsed.start_date,
            end_date=parsed.end_date,
            application_status_any=["PENDING"],
        )
        
        return {
            "count": count_r.get("count", 0),
            "items": r.get("items", []),
            "start_date": str(parsed.start_date),
            "end_date": str(parsed.end_date),
        }
    
    # -------------------------------------------------------------------------
    # Answer 노드
    # -------------------------------------------------------------------------
    async def _answer(self, state: EmployerChatbotState) -> Dict[str, Any]:
        """응답 생성"""
        rid = state.get("request_id") or str(uuid.uuid4())
        parsed = state.get("parsed") or EmployerChatbotParsedSpec(
            intent=EmployerChatbotIntent.HELP, confidence=0.0
        )
        result = state.get("result") or {}
        
        response = await render_response(parsed, result, rid)
        
        return {
            "answer": response.get("answer", ""),
            "request_id": response.get("request_id", rid),
        }
    
    # -------------------------------------------------------------------------
    # 공개 API
    # -------------------------------------------------------------------------
    async def ask(
        self,
        message: str,
        request_id: Optional[str] = None,
        conversation_id: Optional[str] = None,
        employer_id: Optional[int] = None,
    ) -> Dict[str, Any]:
        """
        사용자 질문 처리
        
        Args:
            message: 사용자 메시지
            request_id: 요청 ID (없으면 자동 생성)
            conversation_id: 대화 ID (없으면 자동 생성)
            employer_id: 기업 ID
            
        Returns:
            응답 딕셔너리 (request_id, conversation_id, turn, mode, answer, parsed, result)
        """
        rid = request_id or str(uuid.uuid4())
        cid = conversation_id or str(uuid.uuid4())
        
        # 대화 컨텍스트 로드
        ctx = await self.memory.load(cid)
        if ctx is None:
            ctx = EmployerChatbotConversationState(
                conversation_id=cid,
                employer_id=employer_id
            )
        
        if employer_id is not None:
            ctx.employer_id = employer_id
        
        # 턴 관리
        ctx.turn += 1
        if ctx.mode == ConversationMode.FULL and ctx.turn > self.max_full_turns:
            ctx.mode = ConversationMode.SUMMARY
            ctx.turn = 1
            ctx.last_item_ids = []
        
        # 그래프 실행
        out = await self.graph.ainvoke({
            "message": message,
            "request_id": rid,
            "conversation_id": cid,
            "employer_id": ctx.employer_id or 0,
            "conversation_mode": ctx.mode.value,
            "turn": ctx.turn,
            "last_parsed_dict": ctx.last_parsed or {},
            "last_intent": ctx.last_intent or "",
            "last_item_ids": ctx.last_item_ids if ctx.mode == ConversationMode.FULL else [],
        })
        
        parsed = out.get("parsed")
        result = out.get("result") or {}
        answer = out.get("answer", "")
        
        # 컨텍스트 업데이트
        if parsed is not None and getattr(parsed, "intent", None) != EmployerChatbotIntent.HELP:
            ctx.last_parsed = parsed.model_dump(mode="json")
            ctx.last_intent = parsed.intent.value
            
            if ctx.mode == ConversationMode.FULL and parsed.intent == EmployerChatbotIntent.LIST_APPLICATIONS:
                items = result.get("items") or []
                ctx.last_item_ids = [
                    int(it["application_id"])
                    for it in items
                    if it.get("application_id") is not None
                ]
        
        # 대화 이력 저장
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


# 싱글톤 인스턴스
employer_chatbot_agent = EmployerStatsChatbot()
