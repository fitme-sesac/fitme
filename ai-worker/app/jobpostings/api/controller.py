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

@router.post(
    "/{job_id}/embedding",
    response_model=JobEmbeddingResponse,
    status_code=status.HTTP_200_OK,
    summary="채용 공고 임베딩 생성 및 DB 업데이트 (ID 기반)"
)
async def trigger_job_embedding(job_id: int):
    """
    **[채용 공고 ID 기반 임베딩 생성]**
    
    DB에 저장된 채용 공고 ID를 받아, 해당 공고의 데이터(제목, 내용, 기술 스택 등)를 조회하여
    임베딩을 생성하고 DB에 업데이트합니다.
    
    - **Trigger**: 관리자 도구 또는 공고 등록/수정 완료 후 비동기로 호출 권장
    - **Process**: DB 조회 -> 텍스트 변환 -> 임베딩 생성 -> DB 저장
    """
    try:
        logger.info(f"Triggering embedding generation for Job ID: {job_id}")
        
        vector = await job_service.generate_and_update_embedding(job_id)
        
        return JobEmbeddingResponse(
            job_id=job_id,
            vector_status="updated",
            embedding_dim=len(vector),
            vector=vector
        )
    except ValueError as ve:
        logger.warning(f"Job ID {job_id} not found: {ve}")
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Job ID {job_id} not found"
        )
    except Exception as e:
        logger.error(f"Error updating embedding for Job ID {job_id}: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )
