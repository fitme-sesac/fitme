from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from app.core.config import settings
import json

from langchain_core.output_parsers import PydanticOutputParser, StrOutputParser
from app.resumes.schemas import ResumeSummary, SummaryType

import os
from dotenv import load_dotenv

# .env 파일에 정의된 환경 변수들을 읽어옵니다.
load_dotenv()

# 이제 별도의 os.environ 설정 없이도 LangChain이 시스템 환경 변수를 인식합니다.
# 랭스미스는 환경 변수만 올바르게 설정되어 있으면 자동으로 추적을 시작합니다.

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
        1. 보안 준수: 개인 식별 정보(PII)는 절대 포함하지 않는다.
        2. 성과 구체화: 프로젝트 설명에 포함된 수치나 구체적 방법론(예: N+1 해결, 인덱싱)을 최우선으로 반영한다.
        3. 할루시네이션 방지(중요): 데이터에 없는 내용을 임의로 생성하거나 추측하지 않는다. 
        4. 정보 부재 시 대응: 특정 항목을 작성할 데이터가 부족한 경우, 아래 예시와 같이 세련되게 표현한다.
           - 예: "현재 데이터상으로는 확인되지 않으나, 관련 프로젝트 경험을 통해 [특정 역량]을 보유했을 것으로 기대됨"
           - 예: "해당 분야에 대한 구체적인 경험 기록이 보완된다면 더욱 매력적인 후보자가 될 것으로 보임"
        5. 언어: 반드시 한국어로 작성한다.
        """

        # [System Instruction 분기]
        if summary_type == SummaryType.TEXT:
            # [TEXT 모드 개선] : 매력적인 세일즈 문구 중심
            system_instruction = f"""{common_role}
        {common_constraints}

        [작성 가이드라인: 서사적 전문 추천서]
        너는 후보자의 데이터를 바탕으로 채용 담당자에게 전달할 '전문 추천사'를 작성해야 한다. 
        아래의 4단계 논리 흐름을 반드시 지켜서 하나의 완성된 문단(Paragraph)으로 구성하라.

        1. 도입부(Headline & Hook): 후보자의 핵심 정체성과 연차, 주력 스택을 한 문장으로 정의하는 강렬한 요약으로 시작한다.
        2. 본론(Technical Evidence): 'Constraints 2번'을 적극 반영하여, 트러블슈팅 경험을 서술한다. 
           - 반드시 'A 상황에서 발생한 B 문제를 C 기술(방법론)로 해결하여 D의 수치적 성과를 냄'이라는 인과관계를 포함한다.
        3. 강점 및 가치(Value Analysis): 기술적 역량이 실제 협업이나 비즈니스에 어떤 긍정적 영향을 줄지 분석한다.
        4. 결론(Future Potential): 데이터에 기반한 성장 가능성과 조직 기여도를 언급하며 마무리한다.

        [형식 제약 사항 - 절대 준수]
        - 리스트 사용 금지: 불렛 포인트(-, *), 번호 매기기(1., 2.) 등 모든 형태의 리스트 형식을 **절대 사용하지 않는다.** 오직 줄글로만 작성한다.
        - 문체 및 어조: '~함', '~임' 등의 명조체 또는 '~입니다'의 경어체 중 하나를 선택하여 일관되게 유지하라. 전문 리쿠르터의 신뢰감 있는 톤을 유지해야 한다.
        - 분량 제어: 10~15줄 내외의 긴 호흡을 가진 문장들로 구성하여, 내용의 깊이감을 확보하라.
        - 정보 부재 시 대응: 'Constraints 4번'에 따라, 없는 정보를 지어내지 말고 "보유 기술의 특성상 ~한 잠재력이 기대됨"과 같이 리쿠르터 특유의 전문적 추론으로 문장을 완성하라.
        """
            parser = self.text_parser
            format_instructions = ""
        else:
            # [STRUCTURED 모드 개선] : 트러블슈팅과 전문성 검증 중심
            # 5. 인사이트 통합 규칙은 포맷 지침과 함께 전달됨
            structured_instruction = """
            5. 인사이트 통합: 기술적 문제 해결 사례(Troubleshooting)가 있다면 반드시 '방법론 -> 결과' 순으로 배치한다.
            6. 데이터 결여 시: '대외 신뢰도'나 '협업 가치관' 등 증빙 데이터가 아예 없는 항목은 "이력서 내 관련 정보 미기재"라고 짧게 표기하는 대신, 후보자의 전체적인 톤앤매너를 통해 유추할 수 있는 '성향' 위주로 서술해줘.
            """
            
            system_instruction = f"""{common_role}
            {common_constraints}
            {structured_instruction}
            
            {{format_instructions}}
            """
            parser = self.json_parser
            format_instructions = parser.get_format_instructions()

        # [User Instruction 강화]
        if type == "RESUME":
            # instruction은 "지금 내가 주는 이 데이터에서 정확히 무엇을 뽑아내야 하는지"를 지정하는 개별 작업 지시에 해당
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
            ("system", system_instruction), # 1. 너는 누구이고 규칙은 이래 (배경/규칙)
            ("user", f"{instruction}\n\nData:\n{{input_text}}") # 2. 자, 이제 이 지시대로(instruction) 이 데이터(input_text)를 분석해!
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
