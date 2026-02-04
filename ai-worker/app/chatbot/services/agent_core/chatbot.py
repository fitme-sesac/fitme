from __future__ import annotations

import uuid
from datetime import date, datetime
from typing import Any, Dict, Optional

from langchain_core.output_parsers import PydanticOutputParser
from langchain_openai import ChatOpenAI

from app.chatbot.repository import job_stats_repo
from app.chatbot.schemas import ChatbotIntent, ChatbotParsedSpec
from app.chatbot.services.agent_core.graph import build_graph
from app.chatbot.services.memory import (
    ChatbotConversationState,
    ConversationMode,
    KST,
    RedisChatbotMemoryStore,
    TranscriptItem,
)
from app.core.config import settings
from app.core.redis import get_redis


class JobStatsChatbot:
    """Job stats chatbot agent (LangGraph)."""

    def __init__(self):
        chatbot_model = getattr(settings, "CHATBOT_OPENAI_MODEL_NAME", None) or settings.OPENAI_MODEL_NAME
        chatbot_temp = float(getattr(settings, "CHATBOT_OPENAI_TEMPERATURE", 0.0))

        self.llm = ChatOpenAI(
            model=chatbot_model,
            temperature=chatbot_temp,
            openai_api_key=settings.OPENAI_API_KEY,
        )

        self.parser = PydanticOutputParser(pydantic_object=ChatbotParsedSpec)
        self.graph = build_graph(llm=self.llm, parser=self.parser, repo=job_stats_repo)

        ttl = int(getattr(settings, "CHATBOT_CONVERSATION_TTL_SECONDS", 604800))
        self.memory = RedisChatbotMemoryStore(get_redis(), ttl_seconds=ttl)

        self.max_full_turns = int(getattr(settings, "CHATBOT_MAX_FULL_TURNS", 10))
        self.max_transcript_items = int(getattr(settings, "CHATBOT_MAX_TRANSCRIPT_ITEMS", 200))

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
                "busiest_week_start_date": ctx.busiest_week_start_date,
                "busiest_week_end_date": ctx.busiest_week_end_date,
                "busiest_week_label": ctx.busiest_week_label or "",
            }
        )

        parsed = out.get("parsed") or None
        result = out.get("result") or {}
        answer = out.get("answer", "")

        if parsed is not None and getattr(parsed, "intent", None) != ChatbotIntent.HELP:
            ctx.last_parsed = parsed.model_dump(mode="json")
            ctx.last_intent = parsed.intent.value

            if ctx.mode == ConversationMode.FULL:
                items = result.get("items") or []
                ids = [int(it["job_id"]) for it in items if it.get("job_id") is not None]
                if ids:
                    ctx.last_item_ids = ids

            # BUSIEST_WEEK 결과 스코프 저장 (follow-up BUSIEST_DAY 지원)
            if parsed.intent == ChatbotIntent.BUSIEST_WEEK:
                ws = result.get("busiest_week_start_date")
                we = result.get("busiest_week_end_date")
                if ws and we:
                    try:
                        ctx.busiest_week_start_date = date.fromisoformat(str(ws)[:10])
                        ctx.busiest_week_end_date = date.fromisoformat(str(we)[:10])
                    except Exception:
                        pass
                lbl = result.get("busiest_week_label")
                if lbl:
                    ctx.busiest_week_label = str(lbl)

        now = datetime.now(tz=KST).isoformat()
        ctx.transcript.append(TranscriptItem(role="user", content=message, at=now))
        ctx.transcript.append(TranscriptItem(role="assistant", content=answer[:4000], at=now))
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
