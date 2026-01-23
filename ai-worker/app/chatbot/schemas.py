# app/chatbot/schemas.py
from __future__ import annotations

from datetime import date
from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class ChatbotIntent(str, Enum):
    COUNT_POSTINGS = "COUNT_POSTINGS"
    COMPETITION = "COMPETITION"
    LIST_POSTINGS = "LIST_POSTINGS"
    TOP_STACKS = "TOP_STACKS"
    HELP = "HELP"


class ChatbotParsedSpec(BaseModel):
    intent: ChatbotIntent = Field(..., description="User intent")

    # half-open [start_date, end_date)
    start_date: Optional[date] = Field(default=None, description="inclusive")
    end_date: Optional[date] = Field(default=None, description="exclusive")

    keywords_all: List[str] = Field(default_factory=list, max_length=30)
    keywords_any: List[str] = Field(default_factory=list, max_length=30)

    regions_any: List[str] = Field(default_factory=list, max_length=10)
    admin_areas_any: List[str] = Field(default_factory=list, max_length=10)

    # ✅ 직군은 Enum 하드코딩 대신 free-form 문자열로 (확장성)
    job_role: Optional[str] = Field(default=None, max_length=40)

    min_salary_m만원: Optional[int] = Field(default=None, ge=0, le=20000)

    limit: Optional[int] = Field(default=None, ge=1, le=20)
    random: Optional[bool] = Field(default=None)

    confidence: float = Field(default=0.8, ge=0.0, le=1.0)


class ChatbotQueryRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=500)
    conversation_id: Optional[str] = Field(default=None)


class ChatbotQueryResponse(BaseModel):
    request_id: str
    conversation_id: str
    answer: str
    intent: ChatbotIntent
    data: Dict[str, Any] = Field(default_factory=dict)

    turn: int = 1
    mode: str = "FULL"  # FULL or SUMMARY
