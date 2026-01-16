from pydantic import BaseModel, Field
from typing import Dict, Any, List, Optional, Union
from enum import Enum

class Preference(BaseModel):
    location: str = Field(description="희망 근무지")
    salary: str = Field(description="희망 연봉")
    employment_type: str = Field(description="고용 형태")

class BasicInfo(BaseModel):
    title: str = Field(description="한줄 소개")
    tagline: str = Field(description="태그라인")
    re_stack: List[str] = Field(description="종합 보유 기술 (나의 정체성)")
    field: str = Field(description="데이터 타입 (RESUME 또는 SELF_INTRO)")
    preference: Preference

class SummaryType(str, Enum):
    STRUCTURED = "STRUCTURED"
    TEXT = "TEXT"

class Project(BaseModel):
    project_name: str
    period: str
    total_tech_stack: List[str]
    my_tech_stack: List[str]
    contribution: str
    description: str

class Career(BaseModel):
    company_name: str
    role: str
    period: str
    description: str

class ResumeRequest(BaseModel):
    resume_id: int = Field(description="이력서 ID")
    basic_info: BasicInfo
    content: str = Field(description="자기소개 본문")
    projects: List[Project]
    careers: List[Career]
    summary_type: SummaryType = Field(default=SummaryType.STRUCTURED, description="요약 형태 (STRUCTURED: 구조화, TEXT: 줄글)")

class AIProcessResult(BaseModel):
    id: str
    summary: str
    vector_status: str

# [NEW] AI 출력 구조화를 위한 모델
class ResumeSummary(BaseModel):
    summary: str = Field(description="전문성 요약: 전체 경력 연차와 핵심 직무 정체성")
    verified_skills: str = Field(description="검증된 역량: 인증된 경력 기반의 주요 전문 분야 (is_verified: true 활용)")
    tech_stack: str = Field(description="기술 스택 강점: 실무에서 가장 숙련도 높게 사용하는 기술 조합")
    key_achievement: str = Field(description="핵심 프로젝트 성과: 가장 기여도가 높은 프로젝트의 실질적 결과 (contribution_pct 활용)")
    problem_solving: str = Field(description="문제 해결 능력: ai_description 분석을 통한 기술적 깊이")
    credibility: str = Field(description="대외 신뢰도: 자격증 및 외부 링크를 통한 역량 증빙")
    collaboration: str = Field(description="협업 및 가치관: 후보자가 추구하는 개발 문화와 태도")
    matching_info: str = Field(description="채용 매칭 정보: 희망 근무지 및 고용 형태 등")

    def to_formatted_string(self) -> str:
        """DB 저장을 위해 8줄 평문 텍스트로 변환"""
        return (
            f"전문성 요약: {self.summary}\n"
            f"검증된 역량: {self.verified_skills}\n"
            f"기술 스택 강점: {self.tech_stack}\n"
            f"핵심 프로젝트 성과: {self.key_achievement}\n"
            f"문제 해결 능력: {self.problem_solving}\n"
            f"대외 신뢰도: {self.credibility}\n"
            f"협업 및 가치관: {self.collaboration}\n"
            f"채용 매칭 정보: {self.matching_info}"
        )
