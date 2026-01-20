from fastapi import APIRouter, HTTPException
from app.resumes.schemas import ResumeRequest, AIProcessResult
from app.resumes.services.summary import summary_service
from app.resumes.services.vector import vector_service
from app.resumes.repository import resume_repo

router = APIRouter(prefix="/resumes", tags=["resumes"])

@router.post("/process", response_model=AIProcessResult)
async def process_resume(request: ResumeRequest):
    """
    [핵심 로직] 이력서/자기소개서 처리 파이프라인 엔드포인트
    
    이 함수는 다음과 같은 순서로 동작합니다:
    1. **요청 수신**: 프론트엔드로부터 이력서 데이터(JSON)를 받습니다.
    2. **요약 생성 (Summary Service)**: LLM을 사용하여 8줄의 구조화된 요약을 생성합니다.
    3. **벡터 생성 (Vector Service)**: 생성된 요약문을 1536차원의 벡터 숫자로 변환합니다.
    4. **DB 저장 (Repository)**: 요약문과 벡터 데이터를 PostgreSQL에 업데이트합니다.
    5. **결과 반환**: 처리 완료 상태와 생성된 요약을 반환합니다.

    Args:
        request (ResumeRequest): {resume_id, basic_info, content, projects, careers} 형태의 구조화된 요청 본문
    
    Returns:
        AIProcessResult: 처리 결과 (summary, vector_status 포함)
    """
    try:
        # 1. AI 요약 생성 (LLM 호출)
        # LangChain Output Parser를 통해 구조화된 텍스트가 반환됩니다.
        # request 전체를 dict로 변환하여 전달 (새로운 스키마 대응)
        summary_result = await summary_service.generate_summary(request.model_dump(), request.basic_info.field, request.summary_type)

        # [NEW] 표시용 텍스트와 임베딩용 텍스트 분리
        # 구조화된 객체(ResumeSummary, ResumeInsightReport)인 경우, 
        # 임베딩 시에는 검색에 불필요한 메타 데이터(매칭 정보 등)를 제거하고 Markdown 헤더를 적용하여 성능을 높입니다.
        if isinstance(summary_result, str):
            display_summary = summary_result
            embedding_summary = summary_result
        else:
            display_summary = summary_result.to_formatted_string(include_reasoning=request.include_reasoning)
            embedding_summary = summary_result.to_embedding_string()

        # 2. 임베딩 생성 (Embedding API 호출)
        # 검색 최적화된 텍스트(embedding_summary)를 벡터화합니다.
        vector = await vector_service.generate_vector(embedding_summary)

        # 3. DB 저장 (DB 커넥션 및 트랜잭션 처리)
        # 사용자에게 보여줄 원본 요약(display_summary)을 저장합니다.
        # resume_repo.update_resume_data(request.id, display_summary, vector) # TODO: DB연결 설정 후 주석 해제
        resume_repo.update_resume_data(request.resume_id, display_summary, vector)

        # 4. 결과 반환
        return AIProcessResult(
            id=str(request.resume_id),
            summary=display_summary,
            vector_status="generated"
        )
    except Exception as e:
        print(f"Error processing resume {request.resume_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))
