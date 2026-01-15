from fastapi import APIRouter, HTTPException
from app.resumes.schemas import ResumeRequest, AIProcessResult
from app.resumes.services.summary import summary_service
from app.resumes.services.vector import vector_service
from app.resumes.repository import resume_repo

router = APIRouter(prefix="/resumes", tags=["resumes"])

@router.post("/process", response_model=AIProcessResult)
async def process_resume(request: ResumeRequest):
    """
    Process a resume request to generate an AI-created summary, produce a 1536-dimension embedding, store both in the database, and return the processing result.
    
    Parameters:
        request (ResumeRequest): Request body containing `id`, `type`, and `data` (raw resume or cover letter content).
    
    Returns:
        AIProcessResult: Result containing `id` (string), the generated `summary`, and `vector_status` set to `"generated"`.
    
    Raises:
        HTTPException: Raised with status code 500 and error detail if processing fails.
    """
    try:
        # 1. AI 요약 생성 (LLM 호출)
        # LangChain Output Parser를 통해 구조화된 텍스트가 반환됩니다.
        summary = await summary_service.generate_summary(request.data, request.type)

        # 2. 임베딩 생성 (Embedding API 호출)
        vector = await vector_service.generate_vector(summary)

        # 3. DB 저장 (DB 커넥션 및 트랜잭션 처리)
        # resume_repo.update_resume_data(request.id, summary, vector) # TODO: DB연결 설정 후 주석 해제
        resume_repo.update_resume_data(request.id, summary, vector)

        # 4. 결과 반환
        return AIProcessResult(
            id=str(request.id),
            summary=summary,
            vector_status="generated"
        )
    except Exception as e:
        print(f"Error processing resume {request.id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))