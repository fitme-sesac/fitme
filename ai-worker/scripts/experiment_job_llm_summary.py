
import asyncio
import os
import sys

# Windows Console Encoding Fix for Emojis
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding='utf-8')

# 프로젝트 루트 경로 추가
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.jobpostings.repository import job_repo
from app.resumes.repository import resume_repo
#from app.jobpostings.services import job_service # 기존 서비스 (Case A용)
from app.jobpostings.schemas import JobEmbeddingRequest
from app.resumes.services.vector import vector_service

# LLM 사용을 위한 임포트
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import JsonOutputParser
from pydantic import BaseModel, Field # Fixed import
from typing import List
import numpy as np # Added numpy
from app.core.config import settings

# --- [Case B: LLM Schema & Logic Setup] ---

class JobSummaryOutput(BaseModel):
    standard_role: str = Field(description="표준 직무명 (예: Backend Developer)")
    key_competencies: List[str] = Field(description="채용 공고에서 추출한 핵심 역량 키워드 리스트 (예: ['MSA 설계', 'Kafka 운영'])")
    short_summary: str = Field(description="3줄 이내의 매력적인 공고 요약")

async def generate_llm_summary(title: str, description: str, stack: List[str]) -> str:
    """
    LLM을 사용하여 공고를 이력서와 대칭되는 구조로 요약합니다.
    """
    llm = ChatOpenAI(
        model=settings.OPENAI_MODEL_NAME,
        temperature=0,
        openai_api_key=settings.OPENAI_API_KEY
    )
    
    parser = JsonOutputParser(pydantic_object=JobSummaryOutput)
    
    system_prompt = """
    너는 채용 공고(Job Description)를 분석하여 '인재 매칭 시스템'에 최적화된 데이터로 변환하는 AI 채용 담당자야.
    
    [목표]
    입력된 채용 공고를 분석하여 다음 3가지를 추출하라.
    1. standard_role: 공고가 찾는 직무를 가장 잘 설명하는 표준 영문 직무명.
    2. key_competencies: 공고 본문(자격요건, 우대사항)에 숨겨진 '핵심 역량'을 '표준 키워드'로 변환하여 리스트로 추출.
       - 예: "카프카 사용 경험" -> "Kafka 기반 메시징 시스템 운영"
       - 예: "대용량 트래픽" -> "대규모 트래픽 분산 처리 경험"
    3. short_summary: 지원자에게 어필할 수 있는 3줄 요약.
    """
    
    user_prompt = f"""
    [Job Title] {title}
    [Tech Stack] {', '.join(stack)}
    [Description]
    {description}
    """
    
    prompt = ChatPromptTemplate.from_messages([
        ("system", system_prompt),
        ("user", f"{user_prompt}\n\n{{format_instructions}}")
    ])
    
    chain = prompt | llm | parser
    
    try:
        result = await chain.ainvoke({"format_instructions": parser.get_format_instructions()})
        
        # 임베딩용 텍스트 생성 (Symmetric Format)
        embedding_text = (
            f"[Role] {result['standard_role']}\n"
            f"[Tech] {', '.join(stack)}\n"
            f"[Competency]\n" + 
            "\n".join([f"- {c}" for c in result['key_competencies']])
        )
        return embedding_text, result # 텍스트와 결과 객체 둘 다 반환
        
    except Exception as e:
        print(f"❌ LLM Error: {e}")
        return "", {}

# --- [Main Experiment Logic] ---

async def run_experiment(job_id: int, resume_id: int):
    import numpy as np # [FIX] Import numpy locally to ensure visibility
    print(f"\n🧪 [Job vs Resume Matching Experiment] Job ID: {job_id} vs Resume ID: {resume_id}")
    print("="*80)

    # 1. Fetch Data
    job_data = job_repo.get_job_posting(job_id)
    if not job_data:
        print(f"❌ Job {job_id} not found.")
        return
    
    # [FIX] Direct DB Access to fetch Embedding (Missing in Repo)
    from app.core.database import get_db_connection, init_db_extensions
    import json
    
    conn = get_db_connection()
    try:
        init_db_extensions(conn)
        with conn.cursor() as cur:
            cur.execute("SELECT embedding FROM resume WHERE resume_id = %s", (resume_id,))
            row = cur.fetchone()
            if not row or row[0] is None:
                print(f"❌ Resume {resume_id} embedding not found (NULL).")
                return
            
            # pgvector가 list로 자동 변환해주지만, 혹시 문자열로 오면 파싱
            if isinstance(row[0], str):
                resume_vector = np.array(json.loads(row[0]))
            else:
                resume_vector = np.array(row[0])
    finally:
        conn.close()

    job_desc = job_data.get('description', '')
    job_stack = job_data.get('stack', [])
    if isinstance(job_stack, str):
        job_stack = [s.strip() for s in job_stack.split(',')]

    # 2. Case A: Current Rule-based Logic
    # (JobService 로직을 여기서 간략히 재현하거나 직접 호출)
    from app.jobpostings.services import job_service
    
    print("\n🔹 [Case A] Existing Rule-based Logic")
    req = JobEmbeddingRequest(
        job_id=job_id, title=job_data['title'], stack=job_stack, 
        industry=job_data.get('industry', ''), description=job_desc, required_experience=0
    )
    # 현재 서비스의 로직을 타서 텍스트 생성
    text_case_a = job_service._create_symmetric_text(req, job_desc)
    print(f"-- Text Preview (Top 5 lines) --")
    print("\n".join(text_case_a.split('\n')[:5]) + "\n...")
    
    # 임베딩 생성
    vector_case_a = await vector_service.generate_vector(text_case_a)
    
    # 3. Case B: New LLM Logic
    print("\n🔹 [Case B] New LLM-based Logic (Creating...)")
    text_case_b, json_result = await generate_llm_summary(job_data['title'], job_desc, job_stack)
    print(f"-- Text Preview --")
    print(text_case_b)
    
    vector_case_b = await vector_service.generate_vector(text_case_b)

    # 4. Compare Similarity
    import numpy as np
    
    def cosine_similarity(v1, v2):
        return np.dot(v1, v2) / (np.linalg.norm(v1) * np.linalg.norm(v2))

    score_a = cosine_similarity(vector_case_a, resume_vector)
    score_b = cosine_similarity(vector_case_b, resume_vector)
    
    print("\n" + "="*80)
    print(f"📊 [Result Comparison]")
    print(f"   - Resume ID {resume_id} (Target)")
    print(f"   - Job ID {job_id}")
    print("-" * 40)
    print(f"🔴 Case A (Rule-based): Similarity = {score_a:.4f}")
    print(f"🟢 Case B (LLM-based) : Similarity = {score_b:.4f}")
    print("-" * 40)
    
    if score_b > score_a:
        print(f"🚀 LLM Improved Accuracy by +{score_b - score_a:.4f}")
    else:
        print(f"🤔 No Improvement or slightly lower (Diff: {score_b - score_a:.4f})")
    print("="*80)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        # Default for testing
        asyncio.run(run_experiment(1, 2))
    else:
        asyncio.run(run_experiment(int(sys.argv[1]), int(sys.argv[2])))
