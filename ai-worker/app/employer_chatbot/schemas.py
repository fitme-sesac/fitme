# app/employer_chatbot/schemas.py
from __future__ import annotations

from datetime import date
from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field, field_validator


class EmployerChatbotIntent(str, Enum):
    """기업용 챗봇 인텐트"""
    COUNT_APPLICATIONS = "COUNT_APPLICATIONS"  # 지원자 수 조회
    LIST_APPLICATIONS = "LIST_APPLICATIONS"    # 지원자 목록 조회
    APPLICATION_STATS = "APPLICATION_STATS"    # 지원 통계 (공고별/기간별)
    POSTING_PERFORMANCE = "POSTING_PERFORMANCE"  # 공고 성과 분석
    TOP_SKILLS = "TOP_SKILLS"                  # 지원자 보유 스킬 상위
    HELP = "HELP"


class EmployerChatbotParsedSpec(BaseModel):
    intent: EmployerChatbotIntent

    start_date: Optional[date] = None
    end_date: Optional[date] = None

    # 공고 필터
    job_posting_ids: List[int] = Field(default_factory=list, max_length=50)
    job_title_keywords: List[str] = Field(default_factory=list, max_length=30)

    # 지원자 필터
    applicant_skills_any: List[str] = Field(default_factory=list, max_length=30)
    applicant_skills_all: List[str] = Field(default_factory=list, max_length=30)

    # 지원 상태 필터
    application_status_any: List[str] = Field(default_factory=list, max_length=10)
    # PENDING, REVIEWED, SHORTLISTED, REJECTED, HIRED 등

    # 경력 필터
    min_experience_years: Optional[int] = Field(default=None, ge=0, le=60)
    max_experience_years: Optional[int] = Field(default=None, ge=0, le=60)

    # 희망 연봉 필터 (만원 단위)
    min_expected_salary_m만원: Optional[int] = Field(default=None, ge=0, le=50000)
    max_expected_salary_m만원: Optional[int] = Field(default=None, ge=0, le=50000)

    # 지역 필터
    regions_any: List[str] = Field(default_factory=list, max_length=10)

    limit: Optional[int] = Field(default=None, ge=1, le=50)
    random: Optional[bool] = None
    confidence: float = Field(default=0.8, ge=0.0, le=1.0)

    @field_validator(
        "job_posting_ids",
        "job_title_keywords",
        "applicant_skills_any",
        "applicant_skills_all",
        "application_status_any",
        "regions_any",
        mode="before"
    )
    @classmethod
    def none_to_empty_list(cls, v):
        if v is None:
            return []
        if isinstance(v, str):
            return [v]
        return v


class EmployerChatbotQueryRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=500)
    conversation_id: Optional[str] = Field(default=None)
    employer_id: Optional[int] = Field(default=None)  # 기업 ID (인증 시 사용)


class EmployerChatbotQueryResponse(BaseModel):
    request_id: str
    conversation_id: str
    answer: str
    intent: EmployerChatbotIntent
    data: Dict[str, Any] = Field(default_factory=dict)

    turn: int = 1
    mode: str = "FULL"  # FULL or SUMMARY
