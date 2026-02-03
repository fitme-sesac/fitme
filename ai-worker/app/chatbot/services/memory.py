from __future__ import annotations

import json
from datetime import datetime
from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field
from zoneinfo import ZoneInfo

KST = ZoneInfo("Asia/Seoul")


class ConversationMode(str, Enum):
    FULL = "FULL"
    SUMMARY = "SUMMARY"


class TranscriptItem(BaseModel):
    role: str  # "user" | "assistant"
    content: str
    at: str


class ChatbotConversationState(BaseModel):
    conversation_id: str
    mode: ConversationMode = ConversationMode.FULL
    turn: int = 0  # mode 내 turn 카운트

    last_parsed: Optional[Dict[str, Any]] = None
    last_intent: Optional[str] = None
    last_item_ids: List[int] = Field(default_factory=list)

    transcript: List[TranscriptItem] = Field(default_factory=list)
    updated_at: str = Field(default_factory=lambda: datetime.now(tz=KST).isoformat())


class RedisChatbotMemoryStore:
    def __init__(self, redis_client, key_prefix: str = "chatbot:conv:", ttl_seconds: int = 604800):
        self.redis = redis_client
        self.key_prefix = key_prefix
        self.ttl_seconds = ttl_seconds

    def _key(self, conversation_id: str) -> str:
        return f"{self.key_prefix}{conversation_id}"

    async def load(self, conversation_id: str) -> Optional[ChatbotConversationState]:
        raw = await self.redis.get(self._key(conversation_id))
        if not raw:
            return None
        data = json.loads(raw)
        return ChatbotConversationState.model_validate(data)

    async def save(self, state: ChatbotConversationState) -> None:
        payload = state.model_dump(mode="json")
        await self.redis.set(
            self._key(state.conversation_id),
            json.dumps(payload, ensure_ascii=False),
            ex=self.ttl_seconds,  # ✅ 7일 TTL
        )

    async def clear(self, conversation_id: str) -> None:
        await self.redis.delete(self._key(conversation_id))
