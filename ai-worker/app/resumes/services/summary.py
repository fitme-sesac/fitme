from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from app.core.config import settings
import json
from typing import Union

from langchain_core.output_parsers import PydanticOutputParser, StrOutputParser
from app.resumes.schemas import ResumeSummary, ResumeInsightReport, SummaryType

import os
from dotenv import load_dotenv

# .env 파일에 정의된 환경 변수들을 읽어옵니다.
load_dotenv()

# [NEW] 필요한 모듈 임포트
from app.resumes.services.pdf_helper import PDFHandler
from app.resumes.prompts import OCR_ADDITIONAL_INSTRUCTION

class SummaryService:
    def __init__(self):
        self.llm = ChatOpenAI(
            model=settings.OPENAI_MODEL_NAME,
            temperature=0,
            openai_api_key=settings.OPENAI_API_KEY
        )
        # Output Parser 설정
        self.json_parser = PydanticOutputParser(pydantic_object=ResumeSummary)
        self.report_parser = PydanticOutputParser(pydantic_object=ResumeInsightReport)
        self.text_parser = StrOutputParser()
        
        # PDF Handler 초기화
        tesseract_path = r"C:\Program Files\Tesseract-OCR\tesseract.exe"
        poppler_path = r"C:\Program Files\Release-25.12.0-0\poppler-25.12.0\Library\bin"
        
        self.pdf_handler = PDFHandler(tesseract_cmd_path=tesseract_path, poppler_path=poppler_path)

    async def generate_summary(self, data: dict, type: str, summary_type: SummaryType = SummaryType.STRUCTURED) -> Union[str, ResumeSummary, ResumeInsightReport]:
        """
        이력서 데이터를 받아 요약을 생성합니다.
        data: ResumeRequest 객체 (Schema Validation을 거친 데이터가 들어옴)
        type: RESUME 또는 SELF_INTRO (데이터의 성격)
        summary_type: STRUCTURED, TEXT, REPORT
        """
        
        input_content = data.get("content", "")
        file_links = data.get("file_links", [])
        basic_info = data.get("basic_info", {})
        projects = data.get("projects", [])
        careers = data.get("careers", [])
        include_reasoning = data.get("include_reasoning", False)
        
        # Basic Info를 텍스트로 변환
        basic_info_text = ""
        if basic_info:
            basic_info_text = f"""
            - Title: {basic_info.get('title', '')}
            - Tech Stack: {', '.join(basic_info.get('re_stack', []))}
            - Preference: {basic_info.get('preference', {})}
            """

        # Projects를 텍스트로 변환
        projects_text = ""
        if projects:
            projects_list = []
            for p in projects:
                # [NEW] Java DTO에서 String 리스트로 오는 경우 처리
                if isinstance(p, str):
                    projects_list.append(f"- {p}")
                    continue

                # p는 dict 형태일 수도 있고 Pydantic model일 수도 있음. 
                # ResumeRequest로 들어오면 Pydantic model이지만, dict로 변환되어 들어올 수도 있음.
                # 안전하게 처리하기 위해 dict access 시도
                if hasattr(p, 'dict'): p = p.dict()
                
                p_text = f"""
                - Project Name: {p.get('project_name', '')} ({p.get('start_date','')} ~ {p.get('end_date','')})
                - Tech Stack: {', '.join(p.get('total_tech_stack', []))}
                - Contribution: {p.get('contribution', '')}
                - Description: {p.get('description', '')}
                """
                projects_list.append(p_text)
            projects_text = "\n".join(projects_list)

        # Careers를 텍스트로 변환
        careers_text = ""
        if careers:
            careers_list = []
            for c in careers:
                # [NEW] Java DTO에서 String 리스트로 오는 경우 처리
                if isinstance(c, str):
                    careers_list.append(f"- {c}")
                    continue

                if hasattr(c, 'dict'): c = c.dict()
                
                c_text = f"""
                - Company: {c.get('company_name', '')} ({c.get('role', '')})
                - Period: {c.get('start_date', '')} ~ {c.get('end_date', '')}
                - Description: {c.get('description', '')}
                """
                careers_list.append(c_text)
            careers_text = "\n".join(careers_list)
        
        # PDF 파일 처리
        file_content_parts = []
        is_ocr_data = False
        
        if file_links:
            for idx, file_link in enumerate(file_links):
                try:
                    if file_link.startswith("http"):
                        print(f"URL Download not implemented yet: {file_link}")
                        continue
                    
                    print(f"Extracting text from PDF ({idx+1}/{len(file_links)}): {file_link}")
                    pdf_result = self.pdf_handler.extract_text(file_link)
                    
                    pdf_internal_text = pdf_result["masked_text"]
                    extracted = f"[File {idx+1}: {os.path.basename(file_link)}]\n{pdf_internal_text}"
                    file_content_parts.append(extracted)
                    is_ocr_data = True
                    
                except Exception as e:
                    print(f"PDF Processing Failed for {file_link}: {e}")
        
        file_content = "\n\n".join(file_content_parts)
        
        # LLM 입력 데이터 구성
        final_input_text = f"""
        [Candidate Basic Info]
        {basic_info_text}
        
        [Projects Experience]
        {projects_text}

        [Professional Careers]
        {careers_text}

        [Self Introduction / Cover Letter]
        {input_content}
        
        [Attached File Content (OCR Extracted)]
        {file_content}
        """

        # [Common System Instruction]
        common_role = f"너는 IT 전문 기술 리쿠르터이자 기술 면접관이야. 제공된 데이터를 분석하여 요약 리포트를 작성해줘."
        
        common_constraints = """
        [Constraints]
        1. 보안 준수(Critical): **개인 식별 정보(PII)는 '이름', '나이', '성별', '거주지', '사진'을 포함하여 일체 제외한다.** 
           - 후보자를 지칭할 때는 오직 '지원자' 또는 '후보자'로만 통일한다.
           - [Negative Constraints - 절대 하지 말 것]
             (X) "박지연(29세, 여) 지원자는..." -> 이름, 나이, 성별 노출 금지
             (X) "판교에 거주하는..." -> 거주지 노출 금지
             (X) "2024년 2월 졸업 예정인..." -> 학력은 기술하되, 특정 연도를 통해 나이를 유추할 수 있는 표현 자제
           - [Positive Example - 권장]
             (O) "해당 지원자는 5년차 백엔드 개발자로서..."
             (O) "이전 직장에서 대규모 트래픽 처리를 경험하며..."
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
            
            parser = self.text_parser
            format_instructions = ""
            
        elif summary_type == SummaryType.REPORT:
            # [NEW] 인사이트 보고서 모드 (User Request: 0.1초 승부)
            report_instruction = """
            [작성 목표: 헤드라인 중심의 인사이트 보고서]
            1. Headline (0.1초의 승부): 후보자의 핵심 경쟁력을 가장 잘 드러내는 강렬한 한 줄 카피를 작성하라. (예: "0.1초의 승부, RTB 최적화 전문가")
            2. Recruiter Insight (관전 포인트): 단순 사실 나열이 아닌, "왜 이 사람을 뽑아야 하는가?"에 대한 너의 전문적인 평가를 서술하라.
            3. Technical Achievements (리스트): 줄글이 아닌 '리스트 형태'로 작성하여 가독성을 극대화하라. (문제->해결->성과 구조)
            4. Deep Dive: 얕은 나열 대신, 가장 깊이 있게 파고든 기술적 경험 하나를 선정하여 심층 분석하라.
            
            5. Reasoning (출처 명시 필수): ai_reasoning 필드에 **[Citation Rules]**에 따라 출처를 명확히 표기하라.
               - 뭉뚱그려 "프로젝트 경험을 통해..."라고 쓰지 말고, 정확히어떤 프로젝트인지, 어떤 섹션인지 지목하라.
            """
            
            system_instruction = f"""{common_role}
            {common_constraints}
            {report_instruction}
            
            {{format_instructions}}
            """
            parser = self.report_parser
            format_instructions = parser.get_format_instructions()
            
        else: # SummaryType.STRUCTURED
            # [STRUCTURED 모드 개선] : Hybrid Schema (3-in-1 Integration) + Bullet Points (개조식)
            structured_instruction = """
            [작성 가이드라인 - Dual Strategy (Display vs Embedding)]
            
            **전략 1. Display Fields (사람이 읽는 용도): 문맥(Context)과 설득력 있는 서사 중심**
            **전략 2. Embedding Fields (기계 매칭 용도): 채용 공고(JD) 표준 용어 중심**
            
            1. professional_identity: (서사 중심) 후보자의 직무 정체성과 핵심 강점을 매력적인 문장으로 요약
               - 예: "대규모 트래픽 처리 경험을 보유한 5년차 백엔드 개발자로서, 안정적인 시스템 설계를 주도합니다."
            
            2. key_achievement: (성과 중심) 프로젝트의 수치적 성과와 기여도를 구체적으로 명시
               - 예: "결제 시스템 MSA 전환 프로젝트를 통해 TPS를 30% 개선하고 장애율을 0%로 낮춤"
            
            3. problem_solving: (과정 중심) 어떤 상황에서 어떤 기술로 문제를 해결했는지 인과관계 명시
               - 예: "이벤트 발행 실패 문제를 해결하기 위해 Transactional Outbox 패턴을 도입하여 데이터 정합성 확보"

            4. credibility: (팩트 중심) 학력, 자격증, 수상 내역 중 최상위 3개
            5. collaboration: (태도 중심) 협업 스타일 및 리더십 경험
            6. matching_info: 희망 연봉 및 근무지
            
            # [중요] universal_competencies (리스트): **매칭을 위한 '채용 공고(JD) 표준 용어'로 변환**
            7. universal_competencies (리스트):
               - 위 내용들을 채용 공고에 자주 등장하는 '일반화된 역량 키워드'로 변환하여 리스트로 나열하라.
               - **[Critical Constraint 1 - Hallucination Prevention]**:
                 - 반드시 **입력 데이터에 명시된 사실**에 기반해야 한다.
                 - **도구의 단순 사용을 해당 도구가 속한 전체 카테고리(General Concept)로 과대포장하지 마라.**
                 - 예: 단순히 "Docker로 Redis를 띄워봤다"고 해서 "컨테이너 오케스트레이션 및 배포 자동화"라고 쓰지 말 것. (그냥 "Docker 활용 경험" 정도가 적절)
               
               - **[Critical Constraint 2 - Attribution Check (Individual vs Team)]**:
                 - **팀 프로젝트에서 사용된 기술이라도, 후보자가 '직접' 다루거나 기여했다는 명확한 서술이 없으면 역량으로 포함하지 마라.**
                 - 예: 팀이 MSA를 도입했어도, 후보자가 단순히 API 하나만 개발했다면 "MSA 아키텍처 설계"라고 쓰지 말고 "MSA 환경에서의 API 개발 경험" 정도로 한정할 것.
                 - 주체적인 기여(Designed, Implemented, Solved)가 확인된 것만 "설계", "구축", "운영" 등의 단어를 사용하라. 그 외에는 "사용 경험", "활용 경험"으로 낮춰 표현하라.

            8. job_category: 
               - 후보자의 경험과 기술 스택을 종합하여 **가장 적합한 표준 직무명(Standard Job Category)** 하나를 추출하라.
               - 예: "백엔드 개발자", "프론트엔드 개발자", "데이터 사이언티스트", "데브옵스 엔지니어", "모바일 앱 개발자" 등.
               - 너무 긴 설명형 문장이 아닌, 명확한 직무 카테고리 명사를 **반드시 한국어**로 사용하라.

            9. ai_reasoning: 분석 근거 (Page/Section Reference)
            """
            
            system_instruction = f"""{common_role}
            {common_constraints}
            {structured_instruction}
            
            {{format_instructions}}
            """
            parser = self.json_parser
            format_instructions = parser.get_format_instructions()

        # [Citation Rules Definition]
        citation_rules = """
        [Citation Rules - 절대 준수]
        모든 ai_reasoning 항목은 아래 5가지 섹션 태그 중 하나를 반드시 포함해야 한다.
        
        1. [Candidate Basic Info]: 기본 정보(기술 스택, 희망 연봉 등)
        2. [Projects Experience]: 프로젝트 경험 (반드시 '프로젝트명'을 함께 언급할 것)
           - 예: "- [Projects Experience] '유튜브 추천 서비스'의 Qdrant 사용 부분"
        3. [Professional Careers]: 경력 사항
        4. [Self Introduction]: 자기소개서 본문
        5. [Attached File Content]: PDF 첨부 파일 (반드시 'Page 번호'를 명시할 것)
           - 예: "- [Attached File Content] Page 3 포트폴리오 성과 부분"
        
        * 경고: 위 5개 외의 모호한 출처(예: "전반적인 내용", "입력 데이터")는 사용하지 말 것.
        """
        
        # [Global Reinforcement]
        # 모든 모드 공통으로 마지막에 PII 금지 사항을 강력하게 재주입 (Recency Bias 활용)
        pii_reinforcement = """
        [Critical Reminder]
        - **절대 금지**: 이름, 나이, 성별, 사진 정보는 출력 결과에 절대 포함하지 마라.
        - 후보자를 지칭할 때는 오직 '지원자' 또는 '후보자'로만 통일한다.
        """
        system_instruction += f"\n\n{citation_rules}\n\n{pii_reinforcement}"

        # [Hybrid Prompting]
        if is_ocr_data:
            system_instruction += f"\n\n{OCR_ADDITIONAL_INSTRUCTION}"

        # [Instruction 분기]
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
        
        # 포맷 지침 주입
        if summary_type in [SummaryType.STRUCTURED, SummaryType.REPORT]:
             prompt = prompt_template.partial(format_instructions=format_instructions)
        else:
             prompt = prompt_template

        # [Chain 실행]
        chain = prompt | self.llm | parser
        
        try:
            result = await chain.ainvoke({"input_text": final_input_text})
            
            if summary_type == SummaryType.STRUCTURED or summary_type == SummaryType.REPORT:
                # Controller에서 용도(Display vs Embedding)에 따라 다르게 포맷팅할 수 있도록 객체 자체를 반환
                return result
            else:
                return result
                
        except Exception as e:
            print(f"Output Parsing Failed: {e}")
            raise e

summary_service = SummaryService()
