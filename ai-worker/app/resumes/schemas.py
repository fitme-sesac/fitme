from pydantic import BaseModel, Field
from typing import Dict, Any, List, Optional, Union
from enum import Enum

class Preference(BaseModel):
    location: Optional[str] = Field(default=None, description="희망 근무지")
    salary: Optional[str] = Field(default=None, description="희망 연봉")
    employment_type: Optional[str] = Field(default=None, description="고용 형태")

class Education(BaseModel):
    status: str = Field(description="학적 상태 (예: 졸업, 재학, 휴학)")
    major: str = Field(description="전공 (예: 컴퓨터공학, 비전공)")
    school_name: Optional[str] = Field(default=None, description="학교명 (선택)")

class BasicInfo(BaseModel):
    title: str = Field(description="한줄 소개")
    tagline: Optional[str] = Field(default=None, description="태그라인 (선택)")
    re_stack: List[str] = Field(description="종합 보유 기술 (나의 정체성)")
    field: str = Field(description="데이터 타입 (RESUME, PORTFOLIO, INTRO)")
    education: Optional[Education] = Field(default=None, description="학력 정보")
    preference: Preference

class TargetJobInfo(BaseModel):
    job_id: int
    title: str
    description: str
    required_skills: List[str] = []
    location: Optional[str] = None
    salary_text: Optional[str] = None
    company_name: Optional[str] = None

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
    projects: List[Union[Project, str]]
    careers: List[Union[Career, str]]
    summary_type: SummaryType = Field(default=SummaryType.STRUCTURED, description="요약 형태 (STRUCTURED: 구조화, TEXT: 줄글, REPORT: 인사이트 보고서)")
    include_reasoning: bool = Field(default=False, description="AI 분석 근거(Page/Section Reference) 포함 여부")
    target_job: Optional[TargetJobInfo] = Field(default=None, description="맞춤 상담을 위한 타겟 채용공고 정보")

class AIProcessResult(BaseModel):
    id: str
    summary: str
    vector_status: str

# 1. (기존) 데이터 필드형 요약
# 1. (Hybrid) 데이터 필드형 요약
class ResumeSummary(BaseModel):
    # [Merged] 전문성 + 검증된 역량 + 기술 스택 -> 'Professional Identity'
    professional_identity: str = Field(description="[전문성 요약 + 검증된 역량 + 핵심 기술 스택]을 모두 통합하여, 후보자의 직무 정체성과 주력 기술을 3~5줄로 포괄적으로 정의")

    # [Restored] 상세 역량 분석 필드들
    key_achievement: str = Field(description="핵심 프로젝트 성과: 가장 기여도가 높은 프로젝트의 실질적 결과 (contribution_pct 활용)")
    problem_solving: str = Field(description="문제 해결 능력: ai_description 분석을 통한 기술적 깊이 (Method -> Result 구조)")
    credibility: str = Field(description="대외 신뢰도: 자격증 및 교육 수료 등을 통한 역량 강조")
    collaboration: str = Field(description="협업 및 가치관: 후보자가 추구하는 개발 문화와 태도, 팀원과의 협업 능력")
    matching_info: str = Field(description="채용 매칭 정보: 희망 근무지 및 고용 형태 등")

    # [Start] Embed-Only Field
    universal_competencies: List[str] = Field(default=[], description="[Embed Only] 매칭을 위해 이력서 내용을 채용 공고(JD) 표준 용어로 변환한 보편적 역량 키워드 리스트 (예: '대규모 트래픽 분산 처리', 'MSA 아키텍처 설계')")
    job_category: str = Field(default="", description="[Embed Only] 매칭을 위한 표준 직무 카테고리 (예: 'Backend Developer', 'Data Scientist', 'Frontend Developer'). 후보자의 경력과 기술을 바탕으로 가장 적합한 표준 직무명 하나만 추출")

    # [NEW] Vector Optimized Summary (Numeric-Free)
    embedding_summary: str = Field(default="", description="[Embed Only] 벡터 검색 최적화를 위한 의미(Semantic) 중심 요약. 구체적인 수치(%, ms, 건 등)를 제거하고 '대폭 개선', '고성능 아키텍처 구축' 등 일반화된 표현 사용. 기술적 문맥과 문제 해결 키워드 위주로 작성.")

    # [System] 분석 근거
    ai_reasoning: List[str] = Field(description="분석 근거: 각 항목을 작성하기 위해 참고한 문서의 페이지 번호나 섹션 출처 (리스트 형태)")

    def to_formatted_string(self, include_reasoning: bool = False) -> str:
        base_text = (
            f"전문성 및 기술 역량: {self.professional_identity}\n"
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

    def to_embedding_string(self, title: str = "", tech_stack: str = "") -> str:
        """
        [임베딩 전용 포맷 - Symmetric Structure]
        1순위: embedding_summary (Semantic & Numeric-Free)
        2순위: universal_competencies (Keyword List)
        """
        
        # 1. Competency Block Construction
        if self.embedding_summary:
            # [NEW] 의미 기반 요약이 있으면 그것을 통째로 Competency 블록으로 사용
            competency_block = self.embedding_summary
        elif self.universal_competencies:
            competency_block = "\n".join([f"- {c}" for c in self.universal_competencies])
        else:
            competency_block = (
                f"- Professional Identity: {self.professional_identity}\n"
                f"- Key Achievements: {self.key_achievement}\n"
                f"- Problem Solving: {self.problem_solving}"
            )

        # Role: job_category가 있으면 그것을 사용, 없으면 user input title 사용
        role_text = self.job_category if self.job_category else title

        # Final Construction (Tag Style)
        return (
            f"[Role] {role_text}\n"
            f"[Tech Stack] {tech_stack}\n"
            f"[Competencies]\n{competency_block}"
        )

# 2. (신규) 인사이트 보고서형 요약
class ResumeInsightReport(BaseModel):
    headline: str = Field(description="후보자를 정의하는 한 줄의 기술적 정체성")
    professional_profile: str = Field(description="전체 경력 요약 및 주요 기술 스택의 결합된 강점")
    technical_achievements: List[str] = Field(description="구체적인 기술적 성과 리스트")
    deep_dive: str = Field(description="특정 기술에 대한 깊이 있는 이해도 및 트러블슈팅 역량")
    recruiter_insight: str = Field(description="리쿠르터 입장에서 본 이 후보자의 가장 매력적인 기술적 차별점")
    soft_skills: str = Field(description="협업 태도 및 가치관")
    matching_info: str = Field(description="희망 조건 및 매칭 정보")

    job_category: str = Field(default="", description="[Embed Only] 매칭을 위한 표준 직무 카테고리")
    
    # [NEW] Vector Optimized Summary
    embedding_summary: str = Field(default="", description="[Embed Only] 벡터 검색 최적화를 위한 의미(Semantic) 중심 요약. 수치 제거, 문제해결 문맥 위주.")
    
    ai_reasoning: List[str] = Field(description="분석 근거")

    def to_formatted_string(self, include_reasoning: bool = False) -> str:
        achievements_str = "\n".join([f"  • {item}" for item in self.technical_achievements])

        base_text = (
            f"### {self.headline}\n\n"
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

    def to_embedding_string(self, title: str = "", tech_stack: str = "") -> str:
        """
        [임베딩 전용 포맷]
        """
        role_text = self.job_category if self.job_category else title

        # [NEW] embedding_summary 우선 사용
        if self.embedding_summary:
            competency_body = self.embedding_summary
        else:
            achievements_str = ", ".join(self.technical_achievements)
            competency_body = (
                f"{self.headline}\n"
                f"{self.professional_profile}\n"
                f"{achievements_str}\n"
                f"{self.deep_dive}"
            )

        return (
            f"[Role] {role_text}\n"
            f"[Tech Stack] {tech_stack}\n"
            f"[Competencies]\n{competency_body}"
        )