"""
[Test] Original Full Context vs Stored Embedding Similarity
- Uses the same full context structure as summary.py
- Compares with the stored embedding vector from DB (no extra API call for summary)
"""
import asyncio
import sys
import os
import io
import numpy as np

sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.core.database import get_db_connection
from app.resumes.services.vector import vector_service

def cosine_similarity(vec1, vec2):
    vec1 = np.array(vec1)
    vec2 = np.array(vec2)
    return float(np.dot(vec1, vec2) / (np.linalg.norm(vec1) * np.linalg.norm(vec2)))

async def test_similarity(resume_id: int):
    print(f"[Similarity Test] Resume ID: {resume_id}")
    print("="*60)
    
    conn = get_db_connection()
    
    with conn.cursor() as cur:
        # 1. Resume 기본 정보 + 저장된 임베딩 가져오기
        cur.execute("""
            SELECT content, re_stack, preference_location, preference_salary, employment_type, 
                   title, school_state, school_class, embedding
            FROM resume WHERE resume_id = %s
        """, (resume_id,))
        row = cur.fetchone()
        
        if not row:
            print(f"Resume ID {resume_id} not found.")
            return
        
        content = row[0] or ""
        re_stack = row[1] or []
        pref_loc = row[2] or ""
        pref_sal = row[3] or ""
        emp_type = row[4] or ""
        title = row[5] or ""
        edu_status = row[6] or ""
        edu_major = row[7] or ""
        stored_embedding = row[8]  # DB에 저장된 임베딩 벡터
        
        # 2. 프로젝트 가져오기
        cur.execute("""
            SELECT title, start_date, end_date, tech_stack, description 
            FROM resume_project WHERE resume_id = %s
        """, (resume_id,))
        projects = cur.fetchall()
        
        # 3. 경력 가져오기
        cur.execute("""
            SELECT company_name, role, start_date, end_date 
            FROM resume_career WHERE resume_id = %s
        """, (resume_id,))
        careers = cur.fetchall()
    
    conn.close()
    
    if stored_embedding is None:
        print("No stored embedding found. Run batch_process first.")
        return
    
    # 4. Full Context 조립 (summary.py의 final_input_text와 동일한 구조)
    basic_info_text = f"""
    - Title: {title}
    - Tech Stack: {', '.join(re_stack) if re_stack else 'N/A'}
    - Education: {edu_status}, {edu_major}
    - Preference: Location={pref_loc}, Salary={pref_sal}, Type={emp_type}
    """
    
    projects_text = ""
    if projects:
        for p in projects:
            p_text = f"""
            - Project Name: {p[0]} ({p[1]} ~ {p[2]})
            - Tech Stack: {p[3] or 'N/A'}
            - Description: {p[4] or 'N/A'}
            """
            projects_text += p_text
    
    careers_text = ""
    if careers:
        for c in careers:
            c_text = f"""
            - Company: {c[0]} ({c[1]})
            - Period: {c[2]} ~ {c[3]}
            """
            careers_text += c_text
    
    # summary.py와 동일한 구조
    full_context = f"""
    [Basic Info]
    {basic_info_text}

    [Projects Experience]
    {projects_text if projects_text else 'N/A'}

    [Professional Careers]
    {careers_text if careers_text else 'N/A'}

    [Self Introduction / Cover Letter]
    {content}
    """
    
    print("[Full Context Preview (First 500 chars)]")
    print(full_context[:500] + "...\n")
    
    # 5. 원문 임베딩 생성
    print("Generating original context embedding...")
    E_orig = await vector_service.generate_vector(full_context)
    
    # 6. DB에서 요약문 가져와서 임베딩 (stored vector 대신)
    conn2 = get_db_connection()
    with conn2.cursor() as cur:
        cur.execute("SELECT summary FROM resume WHERE resume_id = %s", (resume_id,))
        summary_row = cur.fetchone()
    conn2.close()
    
    if not summary_row or not summary_row[0]:
        print("No summary found.")
        return
    
    summary_text = summary_row[0][:2000]  # Truncate for embedding
    print("Generating summary embedding...")
    E_sum = await vector_service.generate_vector(summary_text)
    
    sim = cosine_similarity(E_orig, E_sum)
    
    print("="*60)
    print(f"[Result] Cosine Similarity: {sim:.4f}")
    print("="*60)
    
    if sim >= 0.85:
        print("[Excellent] Almost identical meaning")
    elif sim >= 0.75:
        print("[Good] Acceptable similarity")
    elif sim >= 0.65:
        print("[Okay] Some divergence, but acceptable")
    else:
        print("[Warning] Significant divergence - check for hallucination")
    
    return sim

if __name__ == "__main__":
    resume_id = int(sys.argv[1]) if len(sys.argv) > 1 else 5090
    asyncio.run(test_similarity(resume_id))
