from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from app.core.config import settings
from typing import Union

class JobSummaryService:
    def __init__(self):
        self.llm = ChatOpenAI(
            model=settings.OPENAI_MODEL_NAME,
            temperature=0,
            openai_api_key=settings.OPENAI_API_KEY
        )

    async def generate_summary(self, title: str, description: str, 
                              stack: str = None, location: str = None, 
                              salary_text: str = None) -> str:
        """
        채용공고 데이터를 받아 요약을 생성합니다.
        이 요약은 벡터 임베딩에 사용됩니다.
        """
        
        # 입력 데이터 구성
        stack_text = f"기술 스택: {stack}" if stack else ""
        location_text = f"근무지: {location}" if location else ""
        salary_info = f"연봉: {salary_text}" if salary_text else ""
        
        input_text = f"""
        [채용공고 제목]
        {title}
        
        [채용공고 상세 설명]
        {description}
        
        {stack_text}
        {location_text}
        {salary_info}
        """
        
        system_instruction = """
        너는 IT 채용 전문가야. 채용공고를 분석하여 다음 정보를 포함한 요약을 작성해줘.
        
        [작성 가이드라인]
        1. 채용공고에서 요구하는 핵심 기술 스택과 역량을 명확히 정리
        2. 업무 내용과 책임 범위를 요약
        3. 선호하는 경력 수준이나 배경을 언급
        4. 회사 문화나 근무 환경에 대한 정보가 있다면 포함
        
        [형식]
        - Markdown 헤더 구조로 작성 (# 제목, ## 소제목)
        - 기술 스택은 명확히 나열
        - 업무 내용은 핵심만 간결하게
        - 매칭에 필요한 정보 중심으로 작성
        
        [제약사항]
        - 한국어로 작성
        - 객관적이고 사실 기반으로 작성
        - 추측이나 가정은 포함하지 않음
        """
        
        instruction = """
        위 채용공고를 분석하여 이력서와의 매칭에 유용한 요약을 작성해줘.
        벡터 검색에 사용될 것이므로, 핵심 키워드와 요구사항을 명확히 포함해야 해.
        """
        
        prompt_template = ChatPromptTemplate.from_messages([
            ("system", system_instruction),
            ("user", f"{instruction}\n\nData:\n{{input_text}}")
        ])
        
        chain = prompt_template | self.llm
        
        try:
            result = await chain.ainvoke({"input_text": input_text})
            return result.content if hasattr(result, 'content') else str(result)
        except Exception as e:
            print(f"Job Summary Generation Failed: {e}")
            raise e

job_summary_service = JobSummaryService()
