from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from app.core.config import settings
import json

from langchain_core.output_parsers import PydanticOutputParser
from app.resumes.schemas import ResumeSummary

class SummaryService:
    def __init__(self):
        """
        Initialize the SummaryService by configuring the language model client and the Pydantic output parser.
        
        Configures an OpenAI chat model client using application settings and sets up a PydanticOutputParser bound to the ResumeSummary schema for structured output parsing.
        """
        self.llm = ChatOpenAI(
            model=settings.OPENAI_MODEL_NAME,
            temperature=0,
            openai_api_key=settings.OPENAI_API_KEY
        )
        # [NEW] Output Parser 설정
        self.parser = PydanticOutputParser(pydantic_object=ResumeSummary)

    async def generate_summary(self, data: dict | str, type: str) -> str:
        """
        Generate an eight-line, structured resume summary from resume or self-introduction data.
        
        Parameters:
            data (dict | str): Resume or self-introduction content; if a dict it will be serialized to JSON, if a string it will be used as-is.
            type (str): "RESUME" to treat the input as a resume; any other value treats the input as a self-introduction.
        
        Returns:
            formatted_summary (str): A human-readable, formatted string produced from a ResumeSummary object that follows the service's schema (PII excluded and verifiable achievements emphasized).
        """
        # 데이터 타입에 따른 텍스트 변환
        if isinstance(data, str):
            input_text = data
            data_type_desc = "텍스트 데이터"
        else:
            input_text = json.dumps(data, ensure_ascii=False, indent=2)
            data_type_desc = "JSON 데이터"

        # 시스템 프롬프트 (Output Parser가 포맷 지침을 자동으로 주입함)
        system_instruction = f"""[Role] 너는 IT 전문 기술 리쿠르터이자 기술 면접관이야. 제공된 {data_type_desc}를 분석하여 '역량 중심 요약서'를 작성해줘.

        [Constraints]
        1. 보안 준수: 이름, 생년월일, 성별 등 개인 식별 정보(PII)는 절대 포함하지 않는다.
        2. 신뢰성 강조: 경력과 자격증 등 검증 가능한 정보는 신뢰도 있게 표현한다.
        3. 성과 구체화: 프로젝트 설명에 포함된 수치나 성과를 반영한다.
        4. 인사이트 통합: 기술적 문제 해결 사례가 있다면 반드시 반영한다.
        5. 언어: 반드시 한국어로 작성한다.

        {{format_instructions}}
        """

        if type == "RESUME":
            instruction = "Summarize the following resume content."
        else:
            instruction = "Summarize the following self-introduction content."
            
        prompt = ChatPromptTemplate.from_messages([
            ("system", system_instruction),
            ("user", f"{instruction}\n\nData:\n{{input_text}}")
        ])
        
        # 포맷 지침 주입 (Format Instructions Injection)
        # PydanticOutputParser가 만든 "JSON 스키마 설명서"를 프롬프트 뒷단에 자동으로 붙여줍니다.
        # 이렇게 하면 LLM이 "JSON으로 대답해야 하는구나" 하고 스키마를 준수하게 됩니다.
        prompt = prompt.partial(format_instructions=self.parser.get_format_instructions())
        
        # [Chain 생성] Prompt -> LLM -> OutputParser
        # 1. Prompt: 완성된 질문 생성
        # 2. LLM: 답변 생성 (JSON 형태의 문자열)
        # 3. OutputParser: JSON 문자열을 파이썬 객체(ResumeSummary)로 변환
        chain = prompt | self.llm | self.parser
        
        # 실행 및 결과 변환 (Pydantic Object -> String)
        try:
            # ainvoke를 통해 비동기로 실행합니다.
            result: ResumeSummary = await chain.ainvoke({"input_text": input_text})
            
            # 최종적으로 DB에 저장하기 좋은 깔끔한 문자열 포맷으로 변환하여 반환합니다.
            return result.to_formatted_string()
        except Exception as e:
            print(f"Output Parsing Failed: {e}")
            raise e

summary_service = SummaryService()