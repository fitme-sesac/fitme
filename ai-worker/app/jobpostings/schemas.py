from pydantic import BaseModel, Field
from typing import List, Optional, Union

class JobEmbeddingRequest(BaseModel):
    """
    [채용 공고 임베딩 요청 스키마]
    채용 공고 데이터를 받아 이력서와 매칭 가능한 벡터를 생성하기 위한 입력 구조입니다.
    """
    job_id: Union[int, str] = Field(description="공고 고유 식별자 (DB 매핑용)")
    
    # 1. Identity Matching Fields
    title: str = Field(description="공고 제목 (예: 백엔드 개발자)")
    stack: List[str] = Field(description="필수 및 우대 기술 스택 리스트")
    industry: str = Field(description="기업 도메인 (예: 핀테크, 커머스)")
    
    # 2. Experience Matching Fields
    # URL이나 PDF 파일 경로가 들어올 수도 있음 -> Service에서 처리
    description: str = Field(description="주요 업무, 자격 요건, 우대 사항 (Text or PDF Link)")
    
    # 3. Context Fields (For additional matching context)
    # [Modify] 메타데이터(기업명, 지역, 연봉)는 SQL 필터링으로 처리하므로 벡터 생성 과정에서 제외합니다.

class JobEmbeddingResponse(BaseModel):
    job_id: Union[int, str]
    vector_status: str = "generated"
    embedding_dim: int
    vector: List[float] = Field(description="생성된 임베딩 벡터")
