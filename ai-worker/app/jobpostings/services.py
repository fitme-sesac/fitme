import logging
import re
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
        stack_raw = job_data.get('stack') or []
        if isinstance(stack_raw, list):
            stack_list = stack_raw
        else:
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
            description=job_data.get('description') or "",
            required_experience=job_data.get('required_experience')
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
        
        # [DEBUG] Print generated text to verify v2 logic
        print(f"\n[DEBUG] Generated Embedding Text for Job {request.job_id}:\n{'-'*40}\n{embedding_text}\n{'-'*40}\n")

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
        [Rule-based Narrative Embedding Logic - User v2]
        사용자 요청에 따라 서술형 포맷을 고도화합니다.
        1. 회사명, 복지 등 불필요 정보 제거 (섹션 파싱 강화)
        2. '주요 업무', '자격 요건', '우대 사항'을 분리하여 템플릿 문장에 삽입
        3. 자연스러운 문장 연결
        """
        
        # --- 1. Section Parsing Strategy ---
        # 텍스트를 줄 단위로 읽으며 '헤더'를 감지하고, 해당 섹션의 내용을 별도 리스트에 모읍니다.
        
        responsibilities = []
        requirements = []
        preferences = []
        
        # 헤더 키워드 매핑
        header_map = {
            '주요 업무': 'RESP', '담당 업무': 'RESP', '주요업무': 'RESP', '담당업무': 'RESP', 'Role': 'RESP',
            '자격 요건': 'REQ', '필수 요건': 'REQ', '자격요건': 'REQ', '필수요건': 'REQ', 'Requirements': 'REQ',
            '우대 사항': 'PREF', '우대사항': 'PREF', 'Preferred': 'PREF', 'Plus': 'PREF',
            '기술 스택': 'IGNORE', 'Tech Stack': 'IGNORE', # 스택은 DB 필드 사용
            '근무 조건': 'IGNORE', '근무조건': 'IGNORE', '혜택': 'IGNORE', '복지': 'IGNORE', 
            '복리후생': 'IGNORE', '복리 후생': 'IGNORE', '근무환경': 'IGNORE', '근무 환경': 'IGNORE',
            '채용 절차': 'IGNORE', '전형 절차': 'IGNORE', '안내 사항': 'IGNORE', '회사 소개': 'IGNORE',
            '접수 방법': 'IGNORE', '유의 사항': 'IGNORE'
        }

        lines = description_content.split('\n')
        current_mode = 'UNKNOWN' # 초기값
        
        # 불렛 제거용 정규식
        bullet_pattern = re.compile(r'^[\s\W\d]+') # 문장 앞의 특수문자, 숫자, 공백 제거
        
        for line in lines:
            line_stripped = line.strip()
            if not line_stripped: continue
            
            # 1) 헤더 감지 
            # (대괄호 [], 특수문자 ■, or 단순히 짧은 문장 등)
            # 안전을 위해 키워드가 포함되어 있고 길이가 짧은(20자 이하) 경우 헤더로 의심
            clean_line_for_header = re.sub(r'[\[\]\(\)\-\*■▶]', ' ', line_stripped).strip()
            is_header = False
            
            for key, mode in header_map.items():
                if key in clean_line_for_header and len(clean_line_for_header) < 20:
                    current_mode = mode
                    is_header = True
                    break
            
            if is_header:
                continue # 헤더 라인 자체는 저장 안 함
            
            # 2) 내용 수집 (IGNORE 모드가 아닐 때만)
            if current_mode == 'IGNORE':
                continue
            
            # 3) 텍스트 정제 (불렛 제거, 문장화)
            cleaned_text = bullet_pattern.sub('', line_stripped).strip()
            if not cleaned_text: continue
            
            # 문장부호 정리 (끝에 점 찍기)
            if cleaned_text[-1] not in ['.', '!', '?']:
                cleaned_text += '.'
                
            if current_mode == 'RESP':
                responsibilities.append(cleaned_text)
            elif current_mode == 'REQ':
                requirements.append(cleaned_text)
            elif current_mode == 'PREF':
                preferences.append(cleaned_text)
            elif current_mode == 'UNKNOWN':
                # 헤더를 못 찾은 경우, 초반부는 회사 소개일 가능성이 높으므로 일단 Skip하거나
                # 또는 Job Description이 통짜로 되어 있을 수도 있음.
                # 전략: '하다', '합니다'로 끝나면 업무일 확률 높음.
                # 일단 안전하게 RESP에 넣되, 너무 길면(회사소개) Risk.
                # v2에서는 Unknown은 과감히 버리거나, 맨 뒤에 기타로 붙임. 여기서는 RESP로 가정.
                responsibilities.append(cleaned_text)

        # --- 2. Narrative Reconstruction ---
        narrative_parts = []
        
        # 2-1. Intro
        narrative_parts.append(f"이 포지션은 {request.title} 채용 공고입니다.")
        
        # 2-2. Responsibility
        if responsibilities:
            # "A. B. C. 역할을 수행합니다."
            resp_text = " ".join(responsibilities)
            narrative_parts.append(f"{resp_text} 역할을 수행합니다.")
            
        # 2-3. Tech & Requirement
        # "해당 개발자는 {Stack} 기반의 시스템을 개발합니다."
        if request.stack:
            stack_str = ", ".join(request.stack) if isinstance(request.stack, list) else str(request.stack)
            narrative_parts.append(f"{stack_str} 기술 스택 활용 경험이 요구됩니다.")
            
        if requirements:
            req_text = " ".join(requirements)
            narrative_parts.append(f"{req_text}")
            
        # 2-4. Experience
        req_exp = request.required_experience
        if req_exp is not None and req_exp > 0:
            narrative_parts.append(f"관련 경력 {req_exp}년 이상이 필수이며, 신입 지원은 불가합니다.")
        else:
             narrative_parts.append("신입 또는 관련 프로젝트 경험 보유자 지원 가능합니다.")

        # 2-5. Preference
        if preferences:
            pref_text = " ".join(preferences)
            narrative_parts.append(f"{pref_text} 우대합니다.")
            
        return "\n\n".join(narrative_parts)

job_service = JobService()
