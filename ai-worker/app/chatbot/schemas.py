# app/chatbot/schemas.py
from __future__ import annotations

from datetime import date
from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field, field_validator


class ChatbotIntent(str, Enum):
    COUNT_POSTINGS = "COUNT_POSTINGS"
    COMPETITION = "COMPETITION"
    LIST_POSTINGS = "LIST_POSTINGS"
    NAVIGATE_FILTERED_PAGE = "NAVIGATE_FILTERED_PAGE"
    TOP_SALARY_POSTINGS = "TOP_SALARY_POSTINGS"  # ✅ 연봉 상위 공고
    BOTTOM_SALARY_POSTINGS = "BOTTOM_SALARY_POSTINGS"  # ✅ 연봉 하위 공고
    TOP_STACKS = "TOP_STACKS"
    BOTTOM_STACKS = "BOTTOM_STACKS"
    RATE_STATS = "RATE_STATS"  # ✅ 지원률/경쟁률(%) 통계

    # ✅ 신규: 주간/일간 최대 공고
    BUSIEST_WEEK = "BUSIEST_WEEK"
    BUSIEST_DAY = "BUSIEST_DAY"

    # ✅ 신규: 경쟁률/지원률 극값
    LOW_COMPETITION_POSTINGS = "LOW_COMPETITION_POSTINGS"
    HIGH_COMPETITION_POSTINGS = "HIGH_COMPETITION_POSTINGS"
    LOW_STACK_APPLY_RATE = "LOW_STACK_APPLY_RATE"
    HIGH_STACK_APPLY_RATE = "HIGH_STACK_APPLY_RATE"

    # ✅ 신규: 산업 집계/특정 조건
    TOP_INDUSTRIES = "TOP_INDUSTRIES"
    BOTTOM_INDUSTRIES = "BOTTOM_INDUSTRIES"
    MOST_APPLICANTS_POSTINGS = "MOST_APPLICANTS_POSTINGS"
    DETAIL_URLS = "DETAIL_URLS"
    HELP = "HELP"


class ChatbotParsedSpec(BaseModel):
    intent: ChatbotIntent

    start_date: Optional[date] = None
    end_date: Optional[date] = None

    keywords_all: List[str] = Field(default_factory=list, max_length=30)
    keywords_any: List[str] = Field(default_factory=list, max_length=30)

    # ✅ 산업/업종 필터 (OR)
    industries_any: List[str] = Field(default_factory=list, max_length=10)

    regions_any: List[str] = Field(default_factory=list, max_length=10)
    admin_areas_any: List[str] = Field(default_factory=list, max_length=10)

    job_role: Optional[str] = Field(default=None, max_length=40)

    # ✅ 포지션 필터(OR): backend JobPositionUtil 기준 UI 라벨
    # 예: ['서버/백엔드','프론트엔드']
    positions_any: List[str] = Field(default_factory=list, max_length=15)

    # salary (만원 단위)
    min_salary_m만원: Optional[int] = Field(default=None, ge=0, le=20000)
    max_salary_m만원: Optional[int] = Field(default=None, ge=0, le=20000)

    # ✅ 공고별 경쟁률(%) = apply_count / recruitment_capacity * 100
    min_competition_pct: Optional[float] = Field(default=None, ge=0, le=100000)
    max_competition_pct: Optional[float] = Field(default=None, ge=0, le=100000)

    # ✅ required_experience(년) 필터
    min_required_experience_years: Optional[int] = Field(default=None, ge=0, le=60)
    max_required_experience_years: Optional[int] = Field(default=None, ge=0, le=60)

    limit: Optional[int] = Field(default=None, ge=1, le=20)
    random: Optional[bool] = None
    confidence: float = Field(default=0.8, ge=0.0, le=1.0)

    @field_validator("keywords_all", "keywords_any", "industries_any", "regions_any", "admin_areas_any", "positions_any", mode="before")
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
