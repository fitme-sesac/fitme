"""
[테스트] 이력서 원문 vs AI 요약본 임베딩 유사도 검사 스크립트
목적: AI가 요약한 내용이 원문의 핵심 의미를 얼마나 잘 보존하고 있는지 수치적으로 확인합니다.
"""
import asyncio
import sys
import os
import io
import numpy as np

# 터미널에서 한글이 정상적으로 출력되도록 설정
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

# 프로젝트 루트 경로를 path에 추가하여 app 패키지를 불러올 수 있게 함
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.core.database import get_db_connection
from app.resumes.services.vector import vector_service

def cosine_similarity(vec1, vec2):
    """
    두 벡터 간의 코사인 유사도를 계산합니다.
    1.0에 가까울수록 의미가 일치하며, 0.0에 가까울수록 관계가 없음을 의미합니다.
    """
    vec1 = np.array(vec1)
    vec2 = np.array(vec2)
    return float(np.dot(vec1, vec2) / (np.linalg.norm(vec1) * np.linalg.norm(vec2)))

async def test_similarity(resume_id: int):
    print(f"\n[유사도 테스트 시작] 이력서 ID: {resume_id}")
    print("="*60)
    
    conn = get_db_connection()
    
    try:
        with conn.cursor() as cur:
            # 1. DB에서 이력서의 모든 기본 정보와 저장된 임베딩 호출
            cur.execute("""
                SELECT content, re_stack, preference_location, preference_salary, employment_type, 
                       title, school_state, school_class, embedding
                FROM resume WHERE resume_id = %s
            """, (resume_id,))
            row = cur.fetchone()
            
            if not row:
                print(f"❌ Resume ID {resume_id}를 찾을 수 없습니다.")
                return
            
            # 조회된 데이터 변수에 할당
            content = row[0] or ""
            re_stack = row[1] or []
            pref_loc = row[2] or ""
            pref_sal = row[3] or ""
            emp_type = row[4] or ""
            title = row[5] or ""
            edu_status = row[6] or ""
            edu_major = row[7] or ""
            stored_embedding = row[8]  # 이미 DB에 저장되어 있는 벡터값
            
            # 2. 관련 프로젝트 경험 데이터 호출
            cur.execute("""
                SELECT title, start_date, end_date, tech_stack, description 
                FROM resume_project WHERE resume_id = %s
            """, (resume_id,))
            projects = cur.fetchall()
            
            # 3. 관련 경력 사항 데이터 호출
            cur.execute("""
                SELECT company_name, role, start_date, end_date 
                FROM resume_career WHERE resume_id = %s
            """, (resume_id,))
            careers = cur.fetchall()
        
        # 4. 임베딩 벡터가 없는 경우 테스트 중단
        if stored_embedding is None:
            print("⚠️ 저장된 임베딩이 없습니다. 먼저 batch_process_resume.py를 실행해주세요.")
            return
        
        # 5. [원문 조립] AI에게 전달되기 전의 전체 컨텍스트를 하나의 텍스트로 합칩니다.
        # 이 과정은 summary.py에서 요약을 위해 AI에게 보내는 데이터 포맷과 동일해야 정확합니다.
        basic_info_text = f"""
        - 제목: {title}
        - 기술 스택: {', '.join(re_stack) if re_stack else '없음'}
        - 학력: {edu_status}, {edu_major}
        - 희망 조건: 지역={pref_loc}, 급여={pref_sal}, 고용형태={emp_type}
        """
        
        projects_text = ""
        if projects:
            for p in projects:
                p_text = f"""
                - 프로젝트명: {p[0]} ({p[1]} ~ {p[2]})
                - 기술 스택: {p[3] or '없음'}
                - 설명: {p[4] or '없음'}
                """
                projects_text += p_text
        
        careers_text = ""
        if careers:
            for c in careers:
                c_text = f"""
                - 회사명: {c[0]} (직무: {c[1]})
                - 기간: {c[2]} ~ {c[3]}
                """
                careers_text += c_text
        
        # 최종 원문 텍스트 (Full Context)
        full_context = f"""
        [기본 정보]
        {basic_info_text}

        [프로젝트 경험]
        {projects_text if projects_text else '없음'}

        [직무 경력]
        {careers_text if careers_text else '없음'}

        [자기소개서/내용]
        {content}
        """
        
        print(f"📄 원문 데이터 조립 완료 (글자수: {len(full_context)}자)")
        print(f"[원문 미리보기]\n{full_context[:300]}...\n")
        
        # 6. [원문 임베딩 생성] 전체 원문을 다시 벡터로 변환 (OpenAI API 호출)
        print("🔄 원문 텍스트에 대한 임베딩 벡터 생성 중...")
        E_orig = await vector_service.generate_vector(full_context)
        
        # 7. [요약본 임베딩 생성] DB에 저장된 요약문을 가져와서 벡터로 변환
        with conn.cursor() as cur:
            cur.execute("SELECT summary FROM resume WHERE resume_id = %s", (resume_id,))
            summary_row = cur.fetchone()
        
        if not summary_row or not summary_row[0]:
            print("❌ 저장된 요약본(Summary)이 없습니다.")
            return
        
        summary_text = summary_row[0]
        print(f"🔄 요약본에 대한 임베딩 벡터 생성 중... (요약본 글자수: {len(summary_text)}자)")
        E_sum = await vector_service.generate_vector(summary_text)
        
        # 8. [유사도 비교] 원문 벡터와 요약본 벡터 간의 코사인 유사도 계산
        sim = cosine_similarity(E_orig, E_sum)
        
        print("\n" + "="*60)
        print(f"📊 [결과] 이력서 원문 vs 요약본 유사도 점수: {sim:.4f}")
        print("="*60)
        
        # 9. 유사도 점수에 따른 품질 평가
        if sim >= 0.85:
            print("✅ [최고] 핵심 의미가 매우 잘 보존되었습니다.")
        elif sim >= 0.75:
            print("✅ [우수] 원문의 주요 내용을 충실히 담고 있습니다.")
        elif sim >= 0.65:
            print("⚠️ [보통] 일부 누락이나 의미 변화가 있을 수 있습니다.")
        else:
            print("❌ [경고] 원문의 의미가 많이 훼손되었습니다 (환각 현상 주의).")
        
        return sim

    except Exception as e:
        print(f"❌ 유사도 테스트 중 오류 발생: {e}")
    finally:
        if conn:
            conn.close()

if __name__ == "__main__":
    # 실행 시 ID 인자가 없으면 기본값 5090(테스트용) 사용
    rid = int(sys.argv[1]) if len(sys.argv) > 1 else 5090
    asyncio.run(test_similarity(rid))
