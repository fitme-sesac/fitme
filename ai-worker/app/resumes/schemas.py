from pydantic import BaseModel, Field
from typing import Dict, Any, List, Optional, Union
from enum import Enum

class Preference(BaseModel):
    location: str = Field(description="희망 근무지")
    salary: str = Field(description="희망 연봉")
    employment_type: str = Field(description="고용 형태")

class BasicInfo(BaseModel):
    title: str = Field(description="한줄 소개")
    re_stack: List[str] = Field(description="종합 보유 기술 (나의 정체성)")
    field: str = Field(description="데이터 타입 (RESUME 또는 SELF_INTRO)")
    preference: Preference

class SummaryType(str, Enum):
    STRUCTURED = "STRUCTURED"
    TEXT = "TEXT"
    REPORT = "REPORT"

class Project(BaseModel):
    project_name: str
    start_date: str
    end_date: str
    total_tech_stack: List[str]
    contribution: str
    description: str

class Career(BaseModel):
    company_name: str
    role: str
    start_date: str = Field(description="입사일 (YYYY.MM)")
    end_date: str = Field(description="퇴사일 (YYYY.MM)")
    description: str

class ResumeRequest(BaseModel):
    resume_id: int = Field(description="이력서 ID")
    basic_info: BasicInfo
    content: str = Field(description="자기소개 본문")
    file_links: List[str] = Field(default=[], description="PDF 파일 링크 목록 (선택사항, 예: ['url1', 'url2'])")
    projects: List[Project]
    careers: List[Career]
    summary_type: SummaryType = Field(default=SummaryType.STRUCTURED, description="요약 형태 (STRUCTURED: 구조화, TEXT: 줄글, REPORT: 인사이트 보고서)")
    include_reasoning: bool = Field(default=False, description="AI 분석 근거(Page/Section Reference) 포함 여부")

class AIProcessResult(BaseModel):
    id: str
    summary: str
    vector_status: str

# 1. (기존) 데이터 필드형 요약
class ResumeSummary(BaseModel):
    summary: str = Field(description="전문성 요약: 전체 경력 연차와 핵심 직무 정체성")
    verified_skills: str = Field(description="검증된 역량: 인증된 경력 기반의 주요 전문 분야 (is_verified: true 활용)")
    tech_stack: str = Field(description="기술 스택 강점: 실무에서 가장 숙련도 높게 사용하는 기술 조합")
    key_achievement: str = Field(description="핵심 프로젝트 성과: 가장 기여도가 높은 프로젝트의 실질적 결과 (contribution_pct 활용)")
    problem_solving: str = Field(description="문제 해결 능력: ai_description 분석을 통한 기술적 깊이")
    credibility: str = Field(description="대외 신뢰도: 자격증 및 교육 수료 등을 통한 역량 강조")
    collaboration: str = Field(description="협업 및 가치관: 후보자가 추구하는 개발 문화와 태도, 팀원과의 협업 능력")
    matching_info: str = Field(description="채용 매칭 정보: 희망 근무지 및 고용 형태 등")
    ai_reasoning: List[str] = Field(description="분석 근거: 각 항목을 작성하기 위해 참고한 문서의 페이지 번호나 섹션 출처 (리스트 형태)")

    def to_formatted_string(self, include_reasoning: bool = False) -> str:
        base_text = (
            f"전문성 요약: {self.summary}\n"
            f"검증된 역량: {self.verified_skills}\n"
            f"기술 스택 강점: {self.tech_stack}\n"
            f"핵심 프로젝트 성과: {self.key_achievement}\n"
            f"문제 해결 능력: {self.problem_solving}\n"
            f"대외 신뢰도: {self.credibility}\n"
            f"협업 및 가치관: {self.collaboration}\n"
            f"채용 매칭 정보: {self.matching_info}"
        )
        if include_reasoning:
            reasoning_str = "\n".join(self.ai_reasoning)
            base_text += f"\n\n[AI 분석 근거]\n{reasoning_str}"
        return base_text

    def to_embedding_string(self) -> str:
        """
        [임베딩 전용 포맷]
        - 목적: 유사도 검색 정확도 향상
        - 전략: 
            1. '채용 매칭 정보(location, salary)' 등 검색 노이즈가 될 수 있는 조건성 정보 제외
            2. Markdown Header(#)를 사용하여 의미적 블록 구분 강화
        """
        return (
            f"# 전문성 요약\n{self.summary}\n\n"
            f"# 검증된 역량\n{self.verified_skills}\n\n"
            f"# 기술 스택 강점\n{self.tech_stack}\n\n"
            f"# 핵심 프로젝트 성과\n{self.key_achievement}\n\n"
            f"# 문제 해결 능력\n{self.problem_solving}\n\n"
            f"# 대외 신뢰도\n{self.credibility}\n\n"
            f"# 협업 및 가치관\n{self.collaboration}"
        )

# 2. (신규) 인사이트 보고서형 요약
class ResumeInsightReport(BaseModel):
    headline: str = Field(description="후보자를 정의하는 한 줄의 기술적 정체성 (예: 0.1초의 승부, RTB 최적화 전문가)")
    professional_profile: str = Field(description="전체 경력 요약 및 주요 기술 스택의 결합된 강점")
    technical_achievements: List[str] = Field(description="구체적인 기술적 성과 리스트 (반드시 리스트 형태)")
    deep_dive: str = Field(description="특정 기술에 대한 깊이 있는 이해도 및 트러블슈팅 역량")
    recruiter_insight: str = Field(description="리쿠르터 입장에서 본 이 후보자의 가장 매력적인 기술적 차별점")
    soft_skills: str = Field(description="협업 태도 및 가치관")
    matching_info: str = Field(description="희망 조건 및 매칭 정보")
    ai_reasoning: List[str] = Field(description="분석 근거: 항목별 페이지 출처 (리스트 형태)")

    def to_formatted_string(self, include_reasoning: bool = False) -> str:
        achievements_str = "\n".join([f"  • {item}" for item in self.technical_achievements])
        
        base_text = (
            f"### 🚀 {self.headline}\n\n"
            f"**[전문가 프로필]**\n{self.professional_profile}\n\n"
            f"**[핵심 기술적 성과]**\n{achievements_str}\n\n"
            f"**[기술적 깊이 및 해결 능력]**\n{self.deep_dive}\n\n"
            f"**[리쿠르터의 관전 포인트]**\n💡 {self.recruiter_insight}\n\n"
            f"--- \n"
            f"**[추가 정보]**\n- 협업/가치관: {self.soft_skills}\n- 매칭 정보: {self.matching_info}"
        )
        
        if include_reasoning:
            reasoning_str = "\n".join(self.ai_reasoning)
            base_text += f"\n\n**[AI 분석 근거]**\n{reasoning_str}"
            
        return base_text

    def to_embedding_string(self) -> str:
        """
        [임베딩 전용 포맷]
        - 매칭 정보(matching_info) 제외
        - Markdown Header 구조화
        """
        achievements_str = "\n".join([f"- {item}" for item in self.technical_achievements])
        
        return (
            f"# Headline\n{self.headline}\n\n"
            f"# 전문가 프로필\n{self.professional_profile}\n\n"
            f"# 핵심 기술적 성과\n{achievements_str}\n\n"
            f"# 기술적 깊이 및 해결 능력\n{self.deep_dive}\n\n"
            f"# 리쿠르터의 관전 포인트\n{self.recruiter_insight}\n\n"
            f"# 협업 태도 및 가치관\n{self.soft_skills}"
        )
