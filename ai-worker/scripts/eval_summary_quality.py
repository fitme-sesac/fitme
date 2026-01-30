
import asyncio
import os
import sys

# Windows Console Encoding Fix
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding='utf-8')

# 프로젝트 루트 경로 추가
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.resumes.repository import resume_repo
from app.resumes.services.summary import summary_service
from app.resumes.schemas import ResumeSummary, ResumeRequest

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import JsonOutputParser
from pydantic import BaseModel, Field
from app.core.config import settings
import csv
from datetime import datetime

# --- [Evaluation Output Schema] ---
class EvaluationResult(BaseModel):
    score: int = Field(description="1~5점 사이의 정수 점수")
    reasoning: str = Field(description="점수를 부여한 구체적인 이유")
    suggestions: str = Field(description="품질 개선을 위한 제안")

# 1. Display Summary Evaluator (사람용)
async def evaluate_display_quality(original_text: str, summary_text: str):
    llm = ChatOpenAI(model=settings.OPENAI_MODEL_NAME, temperature=0, openai_api_key=settings.OPENAI_API_KEY)
    parser = JsonOutputParser(pydantic_object=EvaluationResult)

    rubric = """
    [평가 기준: 읽기 좋은 요약 (Display Quality)]
    - 점수 5 (Perfect): 
        * 문제-해결-성과 구조가 완벽함.
        * [핵심] 성과를 증명하는 '구체적인 수치'(예: 30% 개선, 0.5초 단축, 100만 건 처리)가 반드시 포함되어야 함.
    - 점수 4 (Good): 구조는 좋으나, 수치적 성과가 일부 누락됨.
    - 점수 3 (Average): 내용은 있으나 정량적 근거가 부족함.
    - 점수 1~2: 단순 나열식이거나 핵심 내용 누락.
    """
    
    system_prompt = f"""
    너는 채용 담당자를 위한 이력서 평가 AI야. 다음 기준에 맞춰 요약본을 평가해줘.
    
    [필수 지침]
    1. 점수가 5점이 아니라면, 반드시 점수를 깎은 이유와 개선할 점을 'Suggestions'에 적어야 한다.
    2. 특히 '수치적 성과'가 누락되었다면, 원본 텍스트에서 어떤 수치를 가져왔어야 했는지 구체적으로 지적해라.
    
    {rubric}
    """

    # Escape Braces
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

# 2. Embedding Text Evaluator (기계/검색용)
async def evaluate_embedding_quality(original_text: str, embedding_text: str):
    llm = ChatOpenAI(model=settings.OPENAI_MODEL_NAME, temperature=0, openai_api_key=settings.OPENAI_API_KEY)
    parser = JsonOutputParser(pydantic_object=EvaluationResult)

    rubric = """
    [평가 기준: 검색 엔진 최적화 (Embedding Quality)]
    - 점수 5: 불필요한 조사/서술어(은/는/이/가, 했습니다)가 제거되고, '표준 기술 키워드' 위주로 밀도 있게 구성됨.
             구조화된 태그([Role], [Stack])가 정확히 사용됨.
    - 점수 3: 키워드는 있으나 서술형 문장이 섞여 있어 노이즈가 있음.
    - 점수 1: 줄글 형태이거나 비표준 용어(예: 자바, 스프링부트)가 난무함.
    """

    safe_original = original_text[:3000].replace("{", "{{").replace("}", "}}")
    safe_emb_text = embedding_text.replace("{", "{{").replace("}", "}}")

    prompt = ChatPromptTemplate.from_messages([
        ("system", f"너는 검색 엔진 엔지니어야. 임베딩 벡터 생성을 위한 텍스트 품질을 평가해줘.\n{rubric}"),
        ("user", f"[Original]\n{safe_original}\n\n[Embedding Text]\n{safe_emb_text}\n\n{{format_instructions}}")
    ])
    
    chain = prompt | llm | parser
    try:
        return await chain.ainvoke({"format_instructions": parser.get_format_instructions()})
    except Exception as e:
        return {"score": 0, "reasoning": str(e), "suggestions": ""}

async def run_quality_check(resume_id: int):
    print(f"\n🔍 [Dual Quality Check] Resume ID: {resume_id}")
    print("="*80)

    # 1. Fetch Data
    pkg = resume_repo.get_resume_by_id(resume_id)
    if not pkg:
        print(f"❌ Resume {resume_id} not found.")
        return
    request_data = ResumeRequest(**pkg)
    original_text_dump = str(pkg)

    # 2. Generate Summary
    print("🤖 Generating Summary...")
    res = await summary_service.generate_summary(request_data.dict(), type="RESUME", summary_type="STRUCTURED")
    
    if not isinstance(res, ResumeSummary):
        print("❌ Error: Result is not ResumeSummary object")
        return

    # Extract Two Forms
    display_text = res.to_formatted_string(include_reasoning=True)
    embedding_text = res.to_embedding_string()

    print(f"\n📄 [Display Summary Preview]\n{'-'*20}\n{display_text[:300]}...\n")
    print(f"\n🧩 [Embedding Text Preview]\n{'-'*20}\n{embedding_text[:300]}...\n")

    # 3. Evaluate Both
    print("⚖️  Evaluating Display Quality...")
    score_display = await evaluate_display_quality(original_text_dump, display_text)
    
    print("⚖️  Evaluating Embedding Quality...")
    score_emb = await evaluate_embedding_quality(original_text_dump, embedding_text)

    print(f"\n📊 [Final Report]")
    print(f"   PLEASE READ: {score_display['score']} / 5 (Display)")
    print(f"   SEARCH OPT : {score_emb['score']} / 5 (Embedding)")
    print("="*80)

    # 4. Save to CSV
    csv_file = os.path.join(os.path.dirname(__file__), "evaluation_history.csv")
    file_exists = os.path.isfile(csv_file)
    
    # Model Names
    gen_model = settings.OPENAI_MODEL_NAME
    eval_model = settings.OPENAI_MODEL_NAME # 현재는 동일한 설정값 사용

    with open(csv_file, mode='a', newline='', encoding='utf-8-sig') as f:
        writer = csv.writer(f)
        if not file_exists:
            writer.writerow([
                "Timestamp", "Resume_ID", "Gen_Model", "Eval_Model",
                "Display_Score", "Display_Reason", 
                "Embedding_Score", "Embedding_Reason", 
                "Display_Summary", "Embedding_Text"
            ])
        
        writer.writerow([
            datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            resume_id, gen_model, eval_model,
            score_display['score'], score_display['reasoning'],
            score_emb['score'], score_emb['reasoning'],
            display_text, embedding_text
        ])
    print(f"✅ Result saved to {csv_file}")

if __name__ == "__main__":
    target_id = int(sys.argv[1]) if len(sys.argv) > 1 else 2
    asyncio.run(run_quality_check(target_id))
