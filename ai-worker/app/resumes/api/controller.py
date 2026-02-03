from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any
from app.resumes.schemas import ResumeRequest, AIProcessResult
from app.resumes.services.summary import summary_service
from app.resumes.services.vector import vector_service
from app.resumes.repository import resume_repo

router = APIRouter(prefix="/resumes", tags=["resumes"])


# ============================================================================
# [NEW] Stateless Worker 응답 스키마
# Java가 받아서 DB에 저장할 데이터
# ============================================================================
class SummaryResponse(BaseModel):
    """Python → Java로 반환하는 AI 처리 결과"""
    summary: str                    # 화면 표시용 요약 (to_formatted_string)
    embedding: List[float]          # 1536차원 벡터
    embedding_text: str             # 임베딩에 사용된 텍스트 (디버깅용)
    eval_info: Dict[str, Any]       # 평가 메타데이터 (score, similarity, history)


# ============================================================================
# [NEW] Stateless Summary Generation
# Java에서 호출 → AI 계산만 수행 → JSON 반환 (DB 저장 X)
# ============================================================================
@router.post("/generate-summary", response_model=SummaryResponse)
async def generate_summary(request: ResumeRequest):
    """
    [Stateless Worker Endpoint]
    
    Java 백엔드에서 호출하여 AI 요약 + 임베딩을 생성합니다.
    DB 저장은 Java가 담당하므로, 여기서는 계산 결과만 반환합니다.
    
    흐름:
    1. 이력서 데이터 수신 (JSON)
    2. AI 요약 생성 (Self-Correction 포함)
    3. 임베딩 벡터 생성 (1536차원)
    4. JSON 반환 (DB 저장 없음!)
    
    Args:
        request: 이력서 데이터 (resume_id, basic_info, content, projects, careers 등)
    
    Returns:
        SummaryResponse: {
            "summary": "화면 표시용 요약문",
            "embedding": [0.1, 0.2, ...],  // 1536차원
            "embedding_text": "임베딩에 사용된 텍스트",
            "eval_info": {"try_count": 1, "history": [...]}
        }
    """
    try:
        # 1. AI 요약 생성 (Self-Correction 포함)
        summary_result, eval_info = await summary_service.generate_summary(
            request.model_dump(), 
            request.basic_info.field, 
            request.summary_type,
            resume_id=request.resume_id  # 로깅용
        )

        # 2. 표시용 텍스트와 임베딩용 텍스트 분리
        if isinstance(summary_result, str):
            display_summary = summary_result
            embedding_text = summary_result
        else:
            # 구조화된 객체인 경우
            meta_title = request.basic_info.title or ""
            meta_stack = ", ".join(request.basic_info.re_stack) if request.basic_info.re_stack else ""
            
            display_summary = summary_result.to_formatted_string(include_reasoning=request.include_reasoning)
            embedding_text = summary_result.to_embedding_string(title=meta_title, tech_stack=meta_stack)

        # 3. 임베딩 생성
        embedding = await vector_service.generate_vector(embedding_text)

        # 4. JSON 반환 (DB 저장 없음!)
        return SummaryResponse(
            summary=display_summary,
            embedding=embedding,
            embedding_text=embedding_text,
            eval_info=eval_info
        )

    except Exception as e:
        print(f"[ERROR] generate_summary failed for resume {request.resume_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ============================================================================
# [LEGACY] 기존 엔드포인트 - 호환성 유지
# Python이 직접 DB에 저장하는 방식 (점진적 마이그레이션 후 삭제 예정)
# ============================================================================

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
    return await _process_resume_pipeline(request)


@router.post("/{resume_id}/summary", response_model=AIProcessResult)
async def process_resume_by_id(resume_id: int):
    """
    [신규 로직] 이력서 ID 기반 처리 파이프라인 엔드포인트

    DB에 저장된 이력서 데이터를 조회하여 AI 요약 및 임베딩을 수행합니다.
    
    흐름:
    1. DB에서 이력서 데이터 조회
    2. ResumeRequest 객체로 변환
    3. 파이프라인 실행 (요약 + 임베딩 + DB 저장)
    """
    try:
        # 1. DB에서 데이터 조회
        resume_data = resume_repo.get_resume_by_id(resume_id)
        if not resume_data:
            raise HTTPException(status_code=404, detail=f"Resume {resume_id} not found.")

        # 2. Request 객체로 변환
        request = ResumeRequest(**resume_data)

        # 3. 파이프라인 실행
        return await _process_resume_pipeline(request)

    except Exception as e:
        print(f"Error processing resume {resume_id}: {e}")
        # HTTPException은 그대로 다시 던짐
        if isinstance(e, HTTPException):
            raise e
        raise HTTPException(status_code=500, detail=str(e))


async def _process_resume_pipeline(request: ResumeRequest) -> AIProcessResult:
    """
    내부 처리 파이프라인 (공통 로직)
    
    이 함수는 실제 AI 처리 로직을 수행합니다:
    1. AI 요약 생성 (LLM 호출)
    2. 임베딩 생성 (Embedding API 호출)
    3. DB 저장
    4. 결과 반환
    """
    try:
        # 1. AI 요약 생성 (LLM 호출)
        # LangChain Output Parser를 통해 구조화된 텍스트가 반환됩니다.
        # request 전체를 dict로 변환하여 전달 (새로운 스키마 대응)
        # [Refactor] 이제 (result, meta_info) 튜플을 반환합니다.
        summary_result, eval_info = await summary_service.generate_summary(
            request.model_dump(), 
            request.basic_info.field, 
            request.summary_type,
            resume_id=request.resume_id
        )

        # [NEW] 표시용 텍스트와 임베딩용 텍스트 분리
        # 구조화된 객체(ResumeSummary, ResumeInsightReport)인 경우,
        # 임베딩 시에는 검색에 불필요한 메타 데이터(매칭 정보 등)를 제거하고 Markdown 헤더를 적용하여 성능을 높입니다.
        if isinstance(summary_result, str):
            display_summary = summary_result
            embedding_summary = summary_result
        else:
            # 구조화된 객체인 경우 (화면용 vs 검색용 분리)
            meta_title = request.basic_info.title or ""
            meta_stack = ", ".join(request.basic_info.re_stack) if request.basic_info.re_stack else ""

            # 1. 화면용: 사람이 읽기 좋게 포맷팅 (이유 포함 가능)
            display_summary = summary_result.to_formatted_string(include_reasoning=request.include_reasoning)

            # 2. 임베딩용: 검색 정확도를 위해 메타데이터(직무, 기술스택) 주입
            embedding_summary = summary_result.to_embedding_string(title=meta_title, tech_stack=meta_stack)

        # 2. 임베딩 생성 (Embedding API 호출)
        # 검색 최적화된 텍스트(embedding_summary)를 벡터화합니다.
        vector = await vector_service.generate_vector(embedding_summary)

        # 3. DB 저장 (DB 커넥션 및 트랜잭션 처리)
        # 사용자에게 보여줄 원본 요약(display_summary)을 저장합니다.
        # eval_info를 함께 전달하여 점수 정보를 기록합니다.
        resume_repo.update_resume_data(request.resume_id, display_summary, vector, eval_info=eval_info)

        # 4. 결과 반환
        return AIProcessResult(
            id=str(request.resume_id),
            summary=display_summary,
            vector_status="generated"
        )
    except Exception as e:
        print(f"Error processing resume {request.resume_id}: {e}")
        # 이미 500 에러를 던지고 있다면 그대로 두거나 처리
        raise e
