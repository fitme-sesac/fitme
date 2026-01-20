from fastapi import APIRouter, HTTPException, status
from app.jobpostings.schemas import JobEmbeddingRequest, JobEmbeddingResponse
from app.jobpostings.services import job_service
import logging

router = APIRouter(
    prefix="/jobs",
    tags=["Job Postings"]
)

logger = logging.getLogger(__name__)

@router.post(
    "/embedding", 
    response_model=JobEmbeddingResponse, 
    status_code=status.HTTP_200_OK,
    summary="채용 공고 임베딩 생성 (Symmetric Embedding)"
)
async def create_job_embedding(request: JobEmbeddingRequest):
    """
    **[채용 공고 -> 벡터 변환]**

    채용 공고 데이터를 입력받아, 이력서 요약(Hybrid Schema)과 대칭되는 구조의 텍스트를 만들고
    이를 벡터화하여 반환합니다.

    - **Symmetric Matching**: 이력서의 '# 전문성', '# 업무 경험' 헤더와 매칭되도록 텍스트를 구조화합니다.
    - **Smart Context**: `description`에 URL이나 PDF 파일 경로가 있으면 자동으로 내용을 추출합니다.
    """
    try:
        logger.info(f"Received embedding request for Job ID: {request.job_id}")
        
        # Service 호출
        vector = await job_service.generate_embedding(request)
        
        return JobEmbeddingResponse(
            job_id=request.job_id,
            vector_status="generated",
            embedding_dim=len(vector),
            vector=vector
        )
        
    except Exception as e:
        logger.error(f"Error processing job embedding: {e}")
        # 500 Error instead of 400, strictly for server-side processing failures
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to generate embedding: {str(e)}"
        )
