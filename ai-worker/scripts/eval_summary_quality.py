
import asyncio
import os
import sys

# 윈도우 콘솔 인코딩 수정
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding='utf-8')

# 프로젝트 루트 경로 추가
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.resumes.repository import resume_repo
from app.resumes.services.summary import summary_service
from app.resumes.services.vector import vector_service
from app.resumes.schemas import ResumeSummary, ResumeRequest
from app.core.database import get_db_connection

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import JsonOutputParser
from pydantic import BaseModel, Field
from app.core.config import settings
import csv
from datetime import datetime

# --- [평가 결과 출력 스키마] ---
class EvaluationResult(BaseModel):
    score: int = Field(description="1~5점 사이의 정수 점수")
    reasoning: str = Field(description="점수를 부여한 구체적인 이유")
    suggestions: str = Field(description="품질 개선을 위한 제안")

# 1. Display Summary Evaluator (사람이 읽는 용도 평가)
async def evaluate_display_quality(original_text: str, summary_text: str):
    # [Judge] 더 똑똑한 모델(gpt-5)을 심사위원으로 사용
    llm = ChatOpenAI(model="gpt-5", temperature=0, openai_api_key=settings.OPENAI_API_KEY)
    parser = JsonOutputParser(pydantic_object=EvaluationResult)

    rubric = """
    [평가 기준: 읽기 좋은 요약 (Display Quality) - 관대한 평가(Lenient)]
    - 점수 5 (Perfect):
        * 문제-해결-성과 구조가 명확하며, 문맥이 매끄러움.
        * 원본의 핵심 내용이 잘 반영됨. (표현 방식이 달라도 의미가 통하면 만점)
    - 점수 4 (Good): 팩트는 정확하나 서사가 다소 평범함.
    - 점수 3 (Average): 내용이 부실하거나 너무 짧음.
    - 점수 1 (Critical Fail): **완전한 날조(Total Fabrication)**. 
      * 원본에 아예 없는 회사명, 수상 이력, 프로젝트를 지어낸 경우에만 해당.
      * [주의] 단순한 표현 차이(예: 유지->단축, 달성->성공)나 수치 변환(예: 100건->대량)은 **절대 1점이 아님** (오히려 잘 쓴 요약으로 간주).
    """
    
    system_prompt = f"""
    너는 채용 담당자를 위한 이력서 평가 AI야. 
    다음 기준에 맞춰 요약본을 평가하되, **융통성 있게(Flexible)** 심사해줘.
    
    [필수 지침]
    1. **[Hallucination 기준 완화]** 원본에 없는 내용을 창조한 경우에만 1점을 줘라. 원본 내용을 바탕으로 수치를 재가공(예: 절대값 -> %)하거나 일반화한 것은 허용한다.
    2. 수치가 없다고 점수를 깎지 마라.
    3. 점수가 5점이 아니라면, 개선할 점을 'Suggestions'에 잘 적어라.
    
    {rubric}
    """

    # 중괄호 Escape 처리 (LangChain 프롬프트 템플릿 충돌 방지)
    safe_original = original_text[:3000].replace("{", "{{").replace("}", "}}")
    safe_summary = summary_text.replace("{", "{{").replace("}", "}}")

    prompt = ChatPromptTemplate.from_messages([
        ("system", system_prompt),
        ("user", f"[Original]\n{safe_original}\n\n[Summary]\n{safe_summary}\n\n{{format_instructions}}")
    ])
    
    chain = prompt | llm | parser
    try:
        return await chain.ainvoke({"format_instructions": parser.get_format_instructions()})
    except Exception as e:
        return {"score": 0, "reasoning": str(e), "suggestions": ""}

# 2. Embedding Text Evaluator (기계 검색용 평가)
async def evaluate_embedding_quality(original_text: str, embedding_text: str):
    llm = ChatOpenAI(model="gpt-5", temperature=0, openai_api_key=settings.OPENAI_API_KEY)
    parser = JsonOutputParser(pydantic_object=EvaluationResult)

    rubric = """
    [평가 기준: 검색 최적화 요약 (Embedding Quality)]
    - 점수 5 (Perfect): 
        * '의미 없는 미세 수치'(예: 13.5%, 3ms)는 잘 제거됨.
        * **중요:** '기술 버전(Java 17)'이나 '규모(Scale)' 정보는 잘 보존됨.
    - 점수 3 (Average): 수치가 일부 남아있거나, 너무 많이 지워짐.
    - 점수 1 (Bad): 불필요한 미세 수치가 그대로 복사됨.
    """
    
    system_prompt = f"""
    너는 벡터 검색을 위한 임베딩 데이터 품질 평가자야.
    
    [핵심 규칙]
    1. **[기술 버전 보호]** 'Java 17', 'Spring 3.0', 'MySQL 8' 같은 **버전 정보는 수치(Metric)가 아니므로 절대 감점하지 마라.** 오히려 있어야 좋은 것이다.
    2. **[수치 정규화]** '32% 증가' -> '대폭 증가' 처럼 일반화된 표현을 우대하라.
    
    {rubric}
    """
    
    safe_original = original_text[:3000].replace("{", "{{").replace("}", "}}")
    safe_emb_text = embedding_text.replace("{", "{{").replace("}", "}}")
    
    prompt = ChatPromptTemplate.from_messages([
        ("system", system_prompt),
        ("user", f"[Original]\n{safe_original}\n\n[Embedding Text]\n{safe_emb_text}\n\n{{format_instructions}}")
    ])
    
    chain = prompt | llm | parser
    try:
        return await chain.ainvoke({"format_instructions": parser.get_format_instructions()})
    except Exception as e:
        return {"score": 0, "reasoning": str(e), "suggestions": ""}

async def run_quality_check(resume_id: int):
    print(f"\n[Dual Quality Check] Resume ID: {resume_id}", flush=True)
    print("="*80, flush=True)

    # 1. Fetch Data
    # 'pkg' 변수는 DB 리포지토리에서 가져온 Raw Dictionary 데이터(날것의 원본)를 담고 있습니다.
    # 이 데이터는 아래에서 두 가지 서로 다른 목적을 위해 분기되어 사용됩니다.
    pkg = resume_repo.get_resume_by_id(resume_id)
    if not pkg:
        print(f"Resume {resume_id} not found.", flush=True)
        return

    # [용도 1: ResumeRequest] - 실제 서비스 로직 입력용
    # 목적: 데이터 유효성 검사(Validation) 및 안전한 객체 접근
    # 설명: Raw 데이터를 Pydantic 모델로 변환하면, 내부적으로 타입 검사(int, str 등 확인)가 수행됩니다.
    #       이렇게 검증된 객체(request_data)를 써야 서비스 로직(generate_summary)이 안전하게 돌아갑니다.
    request_data = ResumeRequest(**pkg)

    # 목적: LLM에게 "이게 원본이야"라고 알려주기 위함 (Ground Truth 제공)
    # 설명: Pydantic을 거치지 않은 '원본 딕셔너리'를 그대로 문자열로 변환(str)해서 저장합니다.
    #       나중에 평가 모델(Judge)에게 "원본 데이터가 이건데, 요약문이 내용을 지어내진 않았니?"라고 물어볼 때 비교군으로 씁니다.
    original_text_dump = str(pkg)

    # 2. Fetch Stored Summary & Original Context from DB
    print("Fetching Data from DB...")
    conn = get_db_connection()
    original_text_dump = ""
    
    try:
        with conn.cursor() as cur:
            # Summary Fetch
            cur.execute("SELECT summary, content FROM resume WHERE resume_id = %s", (resume_id,))
            row = cur.fetchone()
            
            if not row or not row[0]:
                print("Error: No summary found in DB.")
                return

            saved_summary = row[0]
            
            # [NEW] Full Context Fetch (Intro, Skills, Preference)
            # Fetch EVERYTHING from resume table in one go
            cur.execute("""
                SELECT content, re_stack, preference_location, preference_salary, employment_type 
                FROM resume WHERE resume_id = %s
            """, (resume_id,))
            res_row = cur.fetchone()
            
            original_intro = res_row[0] if res_row else ""
            re_stack = res_row[1] if res_row and res_row[1] else []
            re_stack_str = ", ".join(re_stack) if isinstance(re_stack, list) else str(re_stack)
            
            pref_loc = res_row[2] or "N/A"
            pref_sal = res_row[3] or "N/A"
            pref_type = res_row[4] or "N/A"
            pref_text = f"Location: {pref_loc}, Salary: {pref_sal}, Type: {pref_type}"

            # Projects Fetch (Added dates)
            cur.execute("SELECT title, description, tech_stack, start_date, end_date FROM resume_project WHERE resume_id = %s", (resume_id,))
            projects = cur.fetchall()

            # Careers Fetch (Added dates)
            cur.execute("SELECT company_name, role, start_date, end_date FROM resume_career WHERE resume_id = %s", (resume_id,))
            careers = cur.fetchall()
            
            # Construct Original Context String (FULL - Adjusted to Schema)
            project_text = "\n".join([f"- {p[0]} ({p[3]}~{p[4]}): {p[1]} (Tech: {p[2]})" for p in projects])
            career_text = "\n".join([f"- {c[0]} ({c[2]}~{c[3]}): {c[1]}" for c in careers])

            original_text_dump = (
                f"[Basic Info]\nSkills: {re_stack_str}\nPreference: {pref_text}\n\n"
                f"[Self Intro]\n{original_intro}\n\n"
                f"[Careers]\n{career_text}\n\n"
                f"[Projects]\n{project_text}"
            )
            
            # Split Display & Embedding using Regex for robustness
            import re
            
            # Debug: Check exact separator presence
            print(f"[Debug] Raw Summary snippet (mid): ...{repr(saved_summary[len(saved_summary)//2 - 50 : len(saved_summary)//2 + 50])}...", flush=True)
            
            separator_pattern = r"\s*<<<<EMBEDDING_SOURCE>>>>\s*"
            parts = re.split(separator_pattern, saved_summary)
            
            if len(parts) < 2:
                print(f"Warning: Separator not found with regex. Trying strict split...", flush=True)
                # Fallback check
                if "<<<<EMBEDDING_SOURCE>>>>" in saved_summary:
                    parts = saved_summary.split("<<<<EMBEDDING_SOURCE>>>>")
                else:
                    print("Error: Separator really not found!", flush=True)
                    display_text = saved_summary
                    embedding_text = "(Embedding text missing)"
            
            if len(parts) >= 2:
                display_text = parts[0].strip()
                embedding_text = parts[1].strip()
                
            try_count = 0 
            feedback_log = "Loaded from DB"

    finally:
        conn.close()

    print(f"\n[Display Summary Preview]\n{'-'*20}\n{display_text[:200]}...\n")
    print(f"\n[Embedding Text Preview]\n{'-'*20}\n{embedding_text[:200]}...\n")

    # 3. 품질 평가 실행 (Evaluate Both)
    print("Evaluating Display Quality...")
    score_display = await evaluate_display_quality(original_text_dump, display_text)
    
    print("Evaluating Embedding Quality... (SKIPPED for Speed)")
    # score_emb = await evaluate_embedding_quality(original_text_dump, embedding_text)
    score_emb = {"score": 0, "reasoning": "Skipped"}

    print(f"\n[Final Report]")
    print(f"   PLEASE READ: {score_display['score']} / 5 (Display)")
    print(f"   SEARCH OPT : {score_emb['score']} / 5 (Embedding)")
    print(f"   RETRIES    : {try_count} (Feedback Included)")
    print("="*80)

    # 4. CSV 파일로 저장 (Save to CSV)
    csv_file = os.path.join(os.path.dirname(__file__), "final_report.csv")
    file_exists = os.path.isfile(csv_file)
    
    # 모델명 설정
    gen_model = settings.OPENAI_MODEL_NAME
    eval_model = "gpt-5" # [Judge] 평가 모델 명시

    with open(csv_file, mode='a', newline='', encoding='utf-8-sig') as f:
        writer = csv.writer(f)
        if not file_exists:
            writer.writerow([
                "Timestamp", "Resume_ID", "Gen_Model", "Eval_Model",
                "Display_Score", "Display_Reason", 
                "Embedding_Score", "Embedding_Reason", 
                "Try_Count", "Feedback_Log", # [NEW] Metadata Columns
                "Display_Summary", "Embedding_Text",
                "Original_Context" # [NEW] Added Column
            ])
        
        # Sanitize text for CSV (Replace newlines with pipes)
        def sanitize(text):
            if not text: return ""
            return text.replace("\r\n", " | ").replace("\n", " | ").replace("\r", " | ")

        writer.writerow([
            datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            resume_id, gen_model, eval_model,
            score_display['score'], score_display['reasoning'],
            score_emb['score'], score_emb['reasoning'],
            try_count, feedback_log, # [NEW] Data
            sanitize(display_text), 
            sanitize(embedding_text),
            sanitize(original_text_dump) # [NEW] Save Original Context
        ])
    print(f"Result saved to {csv_file}")
    
    # 5. [NEW] DB Update Skipped (Read-Only Mode)
    # 평가 목적이므로 DB를 업데이트하지 않음 (원본 데이터 보존)
    print("\n[DB Update Skipped] Evaluation Mode (Read-Only)")
    # if True: 
    #     ... (Legacy update logic removed for safety)

if __name__ == "__main__":
    import asyncio
    
    # Usage: python eval_summary_quality.py [start_id] [count]
    # Example: python eval_summary_quality.py 5060 10
    start_id = int(sys.argv[1]) if len(sys.argv) > 1 else 5060
    count = int(sys.argv[2]) if len(sys.argv) > 2 else 1
    
    print(f"🚀 Starting Batch Evaluation: ID {start_id} to {start_id + count - 1}")
    
    async def main_batch():
        # [NEW] Initialize CSV with headers (Overwrite mode) - NEW FILENAME
        csv_file = os.path.join(os.path.dirname(__file__), "final_report.csv")
        try:
            with open(csv_file, mode='w', newline='', encoding='utf-8-sig') as f:
                writer = csv.writer(f)
                writer.writerow([
                    "Timestamp", "ResumeID", "Gen_Model", "Eval_Model", 
                    "Display_Score", "Display_Reason", 
                    "Embedding_Score", "Embedding_Reason", 
                    "Try_Count", "Feedback_Log",
                    "Display_Summary", "Embedding_Text",
                    "Original_Context"
                ])
            print(f"📄 CSV initialized: {csv_file}")
        except PermissionError:
            print("⚠️ CSV file is open! Cannot initialize headers. Appending instead...")

        for i in range(count):
            target_id = start_id + i
            await run_quality_check(target_id)
            print("\n" + "="*30 + " Next " + "="*30 + "\n")

    asyncio.run(main_batch())
