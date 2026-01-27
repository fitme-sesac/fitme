from __future__ import annotations

from datetime import date
from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field, field_validator


class ChatbotIntent(str, Enum):
    COUNT_POSTINGS = "COUNT_POSTINGS"
    COMPETITION = "COMPETITION"
    LIST_POSTINGS = "LIST_POSTINGS"
    TOP_STACKS = "TOP_STACKS"
    HELP = "HELP"


class ChatbotParsedSpec(BaseModel):
    intent: ChatbotIntent

    start_date: Optional[date] = None
    end_date: Optional[date] = None

    keywords_all: List[str] = Field(default_factory=list, max_length=30)
    keywords_any: List[str] = Field(default_factory=list, max_length=30)

    regions_any: List[str] = Field(default_factory=list, max_length=10)
    admin_areas_any: List[str] = Field(default_factory=list, max_length=10)

    job_role: Optional[str] = Field(default=None, max_length=40)

    # salary (만원 단위)
    min_salary_m만원: Optional[int] = Field(default=None, ge=0, le=20000)
    max_salary_m만원: Optional[int] = Field(default=None, ge=0, le=20000)  # ✅ 추가

    limit: Optional[int] = Field(default=None, ge=1, le=20)
    random: Optional[bool] = None
    confidence: float = Field(default=0.8, ge=0.0, le=1.0)

    @field_validator("keywords_all", "keywords_any", "regions_any", "admin_areas_any", mode="before")
    @classmethod
    def none_to_empty_list(cls, v):
        if v is None:
            return []
        if isinstance(v, str):
            return [v]
        return v


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
