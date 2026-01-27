from fastapi import APIRouter, HTTPException
from app.jobs.schemas import JobRequest, JobProcessResult
from app.jobs.services.job_summary import job_summary_service
from app.jobs.services.vector import job_vector_service
from app.jobs.repository import job_repo

router = APIRouter(prefix="/jobs", tags=["jobs"])

@router.post("/process", response_model=JobProcessResult)
async def process_job(request: JobRequest):
    """
    [핵심 로직] 채용공고 처리 파이프라인 엔드포인트
    
    이 함수는 다음과 같은 순서로 동작합니다:
    1. **요청 수신**: 백엔드로부터 채용공고 데이터를 받습니다.
    2. **요약 생성 (Job Summary Service)**: LLM을 사용하여 채용공고 요약을 생성합니다.
    3. **벡터 생성 (Vector Service)**: 생성된 요약문을 1536차원의 벡터로 변환합니다.
    4. **DB 저장 (Repository)**: 요약문과 벡터 데이터를 PostgreSQL에 업데이트합니다.
    5. **결과 반환**: 처리 완료 상태와 생성된 요약을 반환합니다.

    Args:
        request (JobRequest): {job_id, title, description, stack, location, salary_text} 형태의 요청 본문
    
    Returns:
        JobProcessResult: 처리 결과 (summary, vector_status 포함)
    """
    try:
        # 1. AI 요약 생성
        summary = await job_summary_service.generate_summary(
            title=request.title,
            description=request.description,
            stack=request.stack,
            location=request.location,
            salary_text=request.salary_text
        )

        # 2. 임베딩 생성
        vector = await job_vector_service.generate_vector(summary)

        # 3. DB 저장
        job_repo.update_job_data(request.job_id, summary, vector)

        # 4. 결과 반환
        return JobProcessResult(
            id=str(request.job_id),
            summary=summary,
            vector_status="generated"
        )
    except Exception as e:
        print(f"Error processing job {request.job_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))
