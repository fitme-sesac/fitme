from pydantic import BaseModel, Field
from typing import List, Optional

class JobRequest(BaseModel):
    """채용공고 벡터 생성 요청"""
    job_id: int = Field(description="채용공고 ID")
    title: str = Field(description="채용공고 제목")
    description: str = Field(description="채용공고 상세 설명")
    stack: Optional[str] = Field(default=None, description="기술 스택 (쉼표로 구분)")
    location: Optional[str] = Field(default=None, description="근무지")
    salary_text: Optional[str] = Field(default=None, description="연봉 정보")

class JobProcessResult(BaseModel):
    """채용공고 벡터 생성 결과"""
    id: str
    summary: str
    vector_status: str
