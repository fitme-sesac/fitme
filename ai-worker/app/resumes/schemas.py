from pydantic import BaseModel, Field
from typing import Dict, Any, List, Optional, Union

class ResumeRequest(BaseModel):
    id: int = Field(..., description="이력서 또는 자기소개서 ID")
    type: str = Field(..., description="데이터 타입 (RESUME 또는 SELF_INTRO)")
    data: Union[Dict[str, Any], str] = Field(..., description="이력서/자기소개서 데이터 (JSON 객체 또는 일반 텍스트)")

    class Config:
        json_schema_extra = {
            "example": {
                "id": 1,
                "type": "RESUME",
                "data": {
                    "experience": [],
                    "skills": ["Python", "Java"],
                    "ai_description": "..."
                }
            }
        }

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
