from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from app.core.config import settings
import json

from langchain_core.output_parsers import PydanticOutputParser, StrOutputParser
from app.resumes.schemas import ResumeSummary, SummaryType

class SummaryService:
    def __init__(self):
        self.llm = ChatOpenAI(
            model=settings.OPENAI_MODEL_NAME,
            temperature=0,
            openai_api_key=settings.OPENAI_API_KEY
        )
        # [NEW] Output Parser 설정
        self.json_parser = PydanticOutputParser(pydantic_object=ResumeSummary)
        self.text_parser = StrOutputParser()

    async def generate_summary(self, data: dict | str, type: str, summary_type: SummaryType = SummaryType.STRUCTURED) -> str:
        """
        이력서 데이터를 받아 요약을 생성합니다.
        data: JSON 객체(dict) 또는 줄글(str)
        type: RESUME 또는 SELF_INTRO (데이터의 성격)
        summary_type: STRUCTURED (구조화된 8줄) 또는 TEXT (10줄 평문)
        """
        # 데이터 타입에 따른 텍스트 변환
        # isinstance()는 파이썬의 내장 함수로, 특정 객체가 어떤 클래스(타입)의 인스턴스인지 확인(비교)할 때 사용
        if isinstance(data, str):
            input_text = data
            data_type_desc = "텍스트 데이터"
        else:
            input_text = json.dumps(data, ensure_ascii=False, indent=2)
            data_type_desc = "JSON 데이터"

        # [Common System Instruction]
        # 리쿠르터라는 페르소나를 부여
        common_role = f"너는 IT 전문 기술 리쿠르터이자 기술 면접관이야. 제공된 {data_type_desc}를 분석하여 요약 리포트를 작성해줘."
        # 지켜야 할 "절대 원칙"을 정의
        common_constraints = """
        [Constraints]
        1. 보안 준수: 이름, 생년월일, 성별 등 개인 식별 정보(PII)는 절대 포함하지 않는다.
        2. 신뢰성 강조: 경력과 자격증 등 검증 가능한 정보는 신뢰도 있게 표현한다.
        3. 성과 구체화: 프로젝트 설명에 포함된 수치나 성과를 반영한다.
        4. 언어: 반드시 한국어로 작성한다.
        """

        # [System Instruction 분기]
        if summary_type == SummaryType.TEXT:
            # [TEXT 모드 개선] : 매력적인 세일즈 문구 중심
            system_instruction = f"""{common_role}
            {common_constraints}
            5. 형식: 후보자의 강점을 한눈에 파악할 수 있는 '헤드라인'을 포함한 15줄 내외의 줄글로 작성해줘. 
            문장마다 후보자의 전문성이 느껴지도록 강한 어조(예: ~를 달성함, ~를 주도함)를 사용해줘.
            """
            parser = self.text_parser
            format_instructions = ""
        else:
            # [STRUCTURED 모드 개선] : 트러블슈팅과 전문성 검증 중심
            # 5. 인사이트 통합 규칙은 포맷 지침과 함께 전달됨
            structured_instruction = "5. 인사이트 통합: 기술적 문제 해결 사례가 있다면 반드시 반영한다."
            
            system_instruction = f"""{common_role}
            {common_constraints}
            {structured_instruction}
            
            {{format_instructions}}
            """
            parser = self.json_parser
            format_instructions = parser.get_format_instructions()

        # [User Instruction 강화]
        if type == "RESUME":
            instruction = """
            이 이력서 데이터에서 후보자의 '기술적 전문성'과 '트러블슈팅 경험'을 찾아내어 분석해줘. 
            특히 이 사람이 팀에 합류했을 때 어떤 기술적 문제를 해결해줄 수 있을지 '강점' 위주로 요약해라.
            """
        else:
            instruction = """
            이 자기소개서에서 후보자의 '개발 철학', '학습 의지', '협업 태도'를 분석해줘. 
            단순한 경험 나열이 아닌, 시련을 극복한 과정(Troubleshooting)을 통해 드러난 잠재력을 강조해라.
            """
            
        prompt_template = ChatPromptTemplate.from_messages([
            ("system", system_instruction),
            ("user", f"{instruction}\n\nData:\n{{input_text}}")
        ])
        
        # 포맷 지침 주입 (TEXT 모드일 때는 빈 문자열 들어감)
        if summary_type == SummaryType.STRUCTURED:
             prompt = prompt_template.partial(format_instructions=format_instructions)
        else:
             prompt = prompt_template

        # [Chain 생성] Prompt -> LLM -> OutputParser
        chain = prompt | self.llm | parser
        
        # 실행 및 결과 변환
        try:
            result = await chain.ainvoke({"input_text": input_text})
            
            if summary_type == SummaryType.STRUCTURED:
                # Pydantic Object -> Formatted String
                return result.to_formatted_string()
            else:
                # Text String -> Return directly
                return result
                
        except Exception as e:
            print(f"Output Parsing Failed: {e}")
            raise e

summary_service = SummaryService()
