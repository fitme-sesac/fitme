from __future__ import annotations

from datetime import date
from typing import Any, Dict, List, Optional, TypedDict

from app.chatbot.schemas import ChatbotParsedSpec


class ChatbotState(TypedDict, total=False):
    """LangGraph state payload for the chatbot agent."""

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

    busiest_week_start_date: date
    busiest_week_end_date: date
    busiest_week_label: str

    scope_job_ids: List[int]


def coerce_last(last_parsed_dict: Dict[str, Any]) -> Optional[ChatbotParsedSpec]:
    """Best-effort conversion from cached dict to ChatbotParsedSpec."""
    try:
        return ChatbotParsedSpec.model_validate(last_parsed_dict or {})
    except Exception:
        return None
