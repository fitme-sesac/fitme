import logging
from typing import List, Union

from app.jobpostings.schemas import JobEmbeddingRequest
from app.resumes.services.pdf_helper import PDFHandler
from app.resumes.services.vector import vector_service
from app.jobpostings.repository import job_repo

class JobService:
    """
    [채용 공고 임베딩 서비스]
    채용 공고 데이터를 이력서 요약(Hybrid Schema)과 대칭되는 구조로 변환하여 임베딩을 생성합니다.
    """
    def __init__(self):
        self.logger = logging.getLogger(__name__)
        self.pdf_handler = PDFHandler()

    async def generate_and_update_embedding(self, job_id: int) -> List[float]:
        """
        [DB 기준 임베딩 생성 및 업데이트]
        DB에 저장된 채용 공고 데이터를 조회하여 임베딩을 생성하고 다시 DB에 업데이트합니다.
        
        Args:
            job_id (int): 채용 공고 ID
            
        Returns:
            List[float]: 생성된 임베딩 벡터
            
        Raises:
            ValueError: 유효하지 않은 Job ID인 경우
        """
        # 1. DB에서 공고 정보 조회
        job_data = job_repo.get_job_posting(job_id)
        if not job_data:
            raise ValueError(f"Job Posting not found for ID: {job_id}")
            
        # 2. Schema Mapping (DB Dict -> Pydantic Request)
        # 2-1. Stack Parsing (CSV String -> List)
        stack_raw = job_data.get('stack') or ""
        stack_list = [s.strip() for s in stack_raw.split(',') if s.strip()]
        
        # 2-2. Create Request Object
        # [Strict Validation] DB에 필수 정보가 없으면 에러를 발생시킵니다 (기본값 사용 X)
        industry = job_data.get('industry')
        if not industry:
             raise ValueError(f"Job ID {job_id}: 'industry' information is missing in Employer table.")

        request = JobEmbeddingRequest(
            job_id=job_id,
            title=job_data.get('title') or "",
            stack=stack_list,
            industry=industry,
            description=job_data.get('description') or ""
        )
        
        # 3. Generate & Update (Resuse existing logic)
        # generate_embedding 내부에서 update_job_embedding을 호출함
        return await self.generate_embedding(request)

    async def generate_embedding(self, request: JobEmbeddingRequest) -> List[float]:
        """
        Job Request -> Symmetric Text -> Vector 변환
        """
        
        # 1. Description 처리 (텍스트 vs PDF 링크 분기)
        description_content = await self._process_description(request.description)
        
        # 2. Symmetric Formatting (이력서 요약 구조와 미러링)
        embedding_text = self._create_symmetric_text(request, description_content)
        
        # 3. Vector Generation
        self.logger.info(f"[JobService] Generating Embedding for Job ID: {request.job_id}")
        vector = await vector_service.generate_vector(embedding_text)
        
        # 4. DB Update
        # job_id가 유효한 숫자일 때만 DB 업데이트 시도
        try:
            job_id_int = int(request.job_id)
            updated = job_repo.update_job_embedding(job_id_int, vector)
            
            if not updated:
                self.logger.warning(f"⚠️ [DB Update Info] Embedding was generated but NOT saved. Job ID {job_id_int} does not exist in DB.")
            else:
                self.logger.info(f"✅ [DB Update Info] Embedding saved successfully for Job ID {job_id_int}.")
                
        except ValueError:
            self.logger.warning(f"Skipping DB update for non-integer Job ID: {request.job_id}")
        
        return vector

    async def _process_description(self, raw_desc: str) -> str:
        """
        Context Aware Text Extraction:
        - URL이나 파일 경로인 경우 -> PDFHandler로 텍스트 추출
        - 일반 텍스트인 경우 -> 그대로 사용
        """
        if not raw_desc:
            return ""
            
        # 파일 경로 또는 URL 패턴 감지 (단순화된 로직)
        is_link_or_path = (
            raw_desc.strip().startswith("http") or 
            raw_desc.strip().lower().endswith(".pdf") or
            raw_desc.strip().lower().endswith(".docx")
        )
        
        if is_link_or_path:
            try:
                self.logger.info(f"[JobService] PDF/Link Detected: {raw_desc}")
                # PDFHandler는 동기 함수지만, I/O 바운드 작업이므로 필요 시 run_in_executor 사용 고려
                # 현재는 간단히 직접 호출 (PDFHandler 내부적으로 처리)
                result = self.pdf_handler.extract_text(raw_desc)
                return result['masked_text'] # 정제된 텍스트 반환
            except Exception as e:
                self.logger.error(f"[JobService] Failed to extract text from link: {e}")
                # 실패 시 링크 자체라도 텍스트로 사용 (최소한의 정보)
                return f"파일 로드 실패: {raw_desc}"
        
        # 일반 텍스트는 그대로 반환
        return raw_desc

    def _create_symmetric_text(self, request: JobEmbeddingRequest, description_content: str) -> str:
        """
        [Symmetric Embedding Logic]
        이력서의 Hybrid Schema 구조와 1:1로 매칭되도록 포맷팅합니다.
        
        Resume Structure:
        - # 전문성 및 기술 역량 (Professional Identity)
        - # 주요 업무 및 자격 요건 (Core Achievements & Problem Solving)
        """
        
        # Stack List -> String 변환
        stack_str = ", ".join(request.stack) if isinstance(request.stack, list) else request.stack
        
        # 1. Identity Section
        section_identity = f"""
# 전문성 및 기술 역량
- 포지션: {request.title}
- 필수 기술: {stack_str}
- 산업 분야: {request.industry}
"""
        # 2. Experience Section
        section_experience = f"""
# 주요 업무 및 자격 요건 (핵심 프로젝트 및 문제 해결)
{description_content}
"""

        # [Modify] 3. Context Section 제거 (Metadata Filtering으로 위임)
        # SQL 필터(연봉, 지역 등)와 역할 분담을 위해 임베딩 텍스트에서 제외합니다.
        
        # 최종 조합
        return f"{section_identity.strip()}\n\n{section_experience.strip()}"

job_service = JobService()
