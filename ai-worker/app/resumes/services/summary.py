from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from app.core.config import settings
import json
import numpy as np
from typing import Union
import csv
from datetime import datetime
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
            # temperature=0, # o1/o3 모델은 temperature 지원 안 할 수 있음
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

    async def generate_summary(self, data: dict, type: str, summary_type: SummaryType = SummaryType.STRUCTURED, resume_id: int = None) -> Union[str, ResumeSummary, ResumeInsightReport]:
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

        # [Extraction] Resume ID (Log용)
        resume_id = data.get("resume_id") or data.get("id") or "N/A"

        # Basic Info를 텍스트로 변환
        basic_info_text = ""
        if basic_info:
            # Education Text Handling
            edu_info = basic_info.get('education')
            edu_text = "Not Specified"
            if edu_info:
                # Pydantic Model or Dict handle
                if hasattr(edu_info, 'dict'): edu_dict = edu_info.dict()
                else: edu_dict = edu_info

                status = edu_dict.get('status', '')
                major = edu_dict.get('major', '')
                school = edu_dict.get('school_name', '')
                edu_text = f"{status}, {major}"
                if school: edu_text += f" ({school})"

            basic_info_text = f"""
            - Title: {basic_info.get('title', '')}
            - Tech Stack: {', '.join(basic_info.get('re_stack', []))}
            - Education: {edu_text}
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

        # [NEW] Target Job Info 처리 (맞춤형 첨삭/요약)
        target_job = data.get("target_job")
        if target_job:
            # dict 변환 (Pydantic model or dict)
            if hasattr(target_job, 'dict'): 
                target_job = target_job.dict()
            
            job_text = f"""
            - Job Title: {target_job.get('title', 'N/A')}
            - Key Skills: {', '.join(target_job.get('required_skills', []))}
            - Description: {target_job.get('description', '')}
            """
            final_input_text += f"\n\n[Target Job Description]\n{job_text}"

        # [Common System Instruction]
        common_role = f"너는 IT 전문 기술 리쿠르터이자 기술 면접관이야. 제공된 데이터를 분석하여 요약 리포트를 작성해줘. 만약 [Target Job Description]이 제공되었다면, 해당 채용 공고와의 연관성을 중심으로 분석해."

        common_constraints = """
        [Constraints]
        1. 보안 준수(Critical): **개인 식별 정보(PII)는 '이름', '나이', '성별', '거주지', '사진'을 포함하여 일체 제외한다. 
           - 후보자를 지칭할 때는 오직 '지원자' 또는 '후보자'로만 통일한다.
        
        [Strict Fact Rules - 절대 준수]
        1. **Fact Preservation (사실 보존):** 원본의 수치나 상태를 자의적으로 해석하거나 변환하여 적지 마라.
           - (X) '사용자 100명 달성' -> '사용자 폭증' (해석 금지)
           - (O) '사용자 100명 달성' (있는 그대로 인용)
           - (X) '300ms 유지' -> '300ms로 단축' (동사 왜곡 금지)
        
        2. **No Hallucination (날조 금지):** 원본 데이터에 명시되지 않은 수치(%, 시간, 금액 등)를 절대 창조하지 마라.
           - 문맥상 수치가 없어 밋밋하더라도, 없는 숫자를 지어내는 것보다는 낫다.
        
        3. **성과 구체화:** 수치가 있다면 최우선으로 반영하되, 위 1번 규칙(보존)을 따른다. 수치가 없다면 기술적 방법론(Methodology)을 구체적으로 서술한다.
        
        4. 정보 부재 시 대응: "데이터상 확인되지 않으나..."와 같이 솔직하게 서술한다.
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
            
            **전략 1. Display Fields (사람이 읽는 용도): 문맥(Context)과 설득력 있는 서사 중심 + 수치적 성과(가용 시) 포함**
            **전략 2. Embedding Fields (기계 매칭 용도): 채용 공고(JD) 표준 용어 중심 + 수치 제거(일반화)**
            
            1. professional_identity: 후보자의 직무 정체성과 핵심 강점을 2-3문장 이내로 요약
            
            2. key_achievement: 가장 핵심적인 성과를 **'•' 기호를 사용하여 개조식 텍스트**로 작성 (최대 3줄)
            
            3. problem_solving: 트러블슈팅 경험을 **최대 150자 이내**의 간결한 평문으로 작성
               - **[금지 사항]**: 마크다운 리스트 문법(`-`, `*`)을 절대 사용하지 마라. 대신 리스트 느낌이 필요하면 텍스트 기호(`•`)를 직접 사용하라.
               - 핵심 기술, 직면한 문제, 해결책만 압축해서 서술할 것.

            4. collaboration: 협업 스타일 및 가치관 (1-2문장)
            
            5. universal_competencies (리스트): 채용 공고 표준 역량 키워드 변환
            6. job_category: 표준 직무명 하나
            
            7. embedding_summary (필수 - 검색 최적화):
               - 수치를 일반화하여 문맥 위주로 요약. 기술 버전은 보존.

            8. ai_reasoning: 분석 근거 (가장 핵심적인 근거 2-3개만)
            
            * 주의: 마크다운 리스트 기호(`-`, `*`)는 가독성을 해치므로 쓰지 마라. 모든 항목은 일반 텍스트 문단으로 작성한다.
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
            # [Step 1] Initial Generation
            print(f"🚀 [SummaryService] Initial Generation ({summary_type})")
            result = await chain.ainvoke({"input_text": final_input_text})
            
            # Text Only 모드면 바로 반환 (메타 정보 없음)
            if summary_type not in [SummaryType.STRUCTURED, SummaryType.REPORT]:
                return result, {}

            # [Step 2] Self-Correction Loop (Quality Check)
            current_summary_obj = result
            if hasattr(current_summary_obj, 'to_formatted_string'):
                current_summary_text = current_summary_obj.to_formatted_string(include_reasoning=True)
            else:
                current_summary_text = str(current_summary_obj)

            max_retries = 2
            
            # Metadata Logging for Experiment
            meta_info = {
                "try_count": 0,
                "history": [] 
            }

            # [NEW] 원문 임베딩 1회 캐싱 (유사도 측정용)
            from app.resumes.services.vector import vector_service
            E_orig = await vector_service.generate_vector(final_input_text[:8000])  # 토큰 제한
            print(f"[Similarity] Original embedding cached (dim: {len(E_orig)})")

            for i in range(max_retries):
                meta_info["try_count"] = i + 1
                
                # [NEW] 임베딩용 요약으로 유사도 측정 (실제 DB 저장 형식과 동일)
                if hasattr(current_summary_obj, 'to_embedding_string'):
                    embedding_text = current_summary_obj.to_embedding_string()
                else:
                    embedding_text = current_summary_text[:2000]
                E_sum = await vector_service.generate_vector(embedding_text)
                similarity = float(np.dot(E_orig, E_sum) / (np.linalg.norm(E_orig) * np.linalg.norm(E_sum)))
                print(f"[Similarity] Attempt {i+1} -> {similarity:.4f}")
                
                # 4점 이상 목표 (5점 만점)
                score, feedback = await self._evaluate_quality(final_input_text, current_summary_text)
                
                print(f"[Self-Correction] Attempt {i+1} -> Score: {score}/5")
                meta_info["history"].append({"score": score, "feedback": feedback, "similarity": similarity})

                # [CSV Logging] 실시간 평가 이력 기록
                await self._log_to_csv(resume_id, i+1, score, similarity, feedback, current_summary_text)

                if score >= 4:
                    print("Quality Passed!")
                    break
                
                print(f"Quality Low (Feedback: {feedback}). Regenerating...")
                try:
                    # _regenerate_summary는 이미 파싱된 객체를 반환함
                    current_summary_obj = await self._regenerate_summary(
                        final_input_text, current_summary_text, feedback, format_instructions
                    )
                    
                    if hasattr(current_summary_obj, 'to_formatted_string'):
                        current_summary_text = current_summary_obj.to_formatted_string(include_reasoning=True)
                except Exception as e:
                    print(f"❌ Regeneration failed: {e}. Keeping original.")
                    meta_info["history"].append({"error": str(e)})
                    break
            
            return current_summary_obj, meta_info

        except Exception as e:
            print(f"Output Parsing Failed: {e}")
            raise e

    # --- [Self-Correction Helpers] ---
    
    async def _evaluate_quality(self, original: str, summary: str):
        """내부 평가용 (LLM-as-a-Judge)"""
        from pydantic import BaseModel, Field # Fixed import
        from langchain_core.output_parsers import JsonOutputParser
        
        class EvalOut(BaseModel):
            score: int = Field(description="1~5 Score")
            suggestions: str = Field(description="Actionable Feedback")
            
        parser = JsonOutputParser(pydantic_object=EvalOut)
        llm = ChatOpenAI(model=settings.OPENAI_MODEL_NAME, temperature=0, openai_api_key=settings.OPENAI_API_KEY)
        
        # Escape Braces
        safe_original = original[:2000].replace("{", "{{").replace("}", "}}")
        safe_summary = summary.replace("{", "{{").replace("}", "}}")

        prompt = ChatPromptTemplate.from_messages([
            ("system", """
            너는 이력서 요약 평가 심사관이다. 다음 기준에 맞춰 유연하게(Flexible) 심사하라.
            
            [평가 기준]
            1. **[Hallucination 기준 완화]** 원본에 없는 '사실'을 완전히 창조(예: 없는 회사명, 수상 이력)한 경우에만 1점을 부여하라.
            2. **[표현 허용]** 원본의 내용을 바탕으로 한 '논리적 추론', '표현 변경(예: 달성->성공)', '수치 변환(예: 100건->대량)'은 허용하며, 오히려 가독성이 좋다면 5점(만점)을 부여하라.
            3. **[불필요한 감점 금지]** 원본에 수치가 없어서 요약에도 수치가 없는 것은 감점 사유가 아니다.
            4. User Preference(연봉, 근무지 등)나 Skill 정보가 [Basic Info]에 있다면 이를 반영한 요약은 정당하다.
            """),
            ("user", f"[Original]\n{safe_original}\n\n[Summary]\n{safe_summary}\n\n{{format_instructions}}")
        ])
        
        chain = prompt | llm | parser
        try:
            res = await chain.ainvoke({"format_instructions": parser.get_format_instructions()})
            return res.get('score', 3), res.get('suggestions', '')
        except:
            return 5, "" # Fail-safe

    async def _regenerate_summary(self, original: str, prev_summary: str, feedback: str, fmt_instr: str):
        """피드백 기반 재생성"""
        llm = ChatOpenAI(model=settings.OPENAI_MODEL_NAME, temperature=0, openai_api_key=settings.OPENAI_API_KEY)
        parser = self.json_parser
        
        # Escape Braces
        safe_original = original[:2000].replace("{", "{{").replace("}", "}}")
        safe_prev_summary = prev_summary.replace("{", "{{").replace("}", "}}")
        safe_feedback = feedback.replace("{", "{{").replace("}", "}}")

        prompt = ChatPromptTemplate.from_messages([
            ("system", f"이전 요약이 거절되었다. 심사관의 피드백을 반영하여 다시 작성해라.\n[Feedback]: {safe_feedback}"),
            ("user", f"[Original]\n{safe_original}\n\n[Previous Summary]\n{safe_prev_summary}\n\n{{format_instructions}}")
        ])
        chain = prompt | llm | parser
        return await chain.ainvoke({"format_instructions": fmt_instr})

    async def _log_to_csv(self, resume_id, attempt, score, similarity, feedback, summary_text):
        """평가 이력을 CSV + Plain Text 파일에 기록"""
        try:
            log_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../../logs"))
            os.makedirs(log_dir, exist_ok=True)
            
            timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            status = "PASS" if score >= 4 else "RETRY"
            
            # 1. CSV 로그 (분석용)
            csv_file = os.path.join(log_dir, "summary_eval_log.csv")
            file_exists = os.path.isfile(csv_file)
            
            with open(csv_file, mode='a', newline='', encoding='utf-8-sig') as f:
                writer = csv.writer(f)
                if not file_exists:
                    writer.writerow(["Timestamp", "ResumeID", "Attempt", "Score", "Similarity", "Feedback", "Summary_Snippet"])
                
                writer.writerow([
                    timestamp,
                    resume_id,
                    attempt,
                    score,
                    f"{similarity:.4f}",
                    feedback,
                    summary_text[:100].replace("\n", " ") + "..."
                ])
            
            # 2. Plain Text 로그 (터미널 확인용)
            txt_file = os.path.join(log_dir, "summary_eval.log")
            with open(txt_file, mode='a', encoding='utf-8') as f:
                f.write(f"[{timestamp}] ID:{resume_id} | Attempt:{attempt} | Score:{score}/5 | Sim:{similarity:.4f} | {status}\n")
                
        except Exception as e:
            print(f"Logging Failed: {e}")

summary_service = SummaryService()
