import sys
import os
import io
import logging

# 터미널 한글 출력 설정
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

# 프로젝트 루트 경로 추가
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.core.database import get_db_connection, init_db_extensions

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("VerifyMatching")

def verify_matching(resume_id: int, top_k: int = 5):
    """
    특정 이력서와 가장 유사한 채용 공고를 검색합니다.
    """
    conn = get_db_connection()
    try:
        init_db_extensions(conn)
        
        with conn.cursor() as cur:
            # 1. 이력서 임베딩 및 메타데이터 가져오기
            logger.info(f"🔍 Fetching embedding & metadata for Resume ID: {resume_id}")
            cur.execute("""
                SELECT embedding, title, re_stack, preference_location, career_years 
                FROM resume 
                WHERE resume_id = %s
            """, (resume_id,))
            resume_row = cur.fetchone()
            
            if not resume_row or resume_row[0] is None:
                logger.error(f"❌ Resume ID {resume_id} not found or has no embedding.")
                return

            resume_vector = resume_row[0]
            resume_title = resume_row[1]
            resume_stack_raw = resume_row[2] or []
            resume_location = resume_row[3] or ""   
            resume_career = resume_row[4] or 0 # Default to 0 (Newcomer)

            logger.info(f"✅ Resume Found: '{resume_title}'")
            logger.info(f"   - Stack: {resume_stack_raw}")
            logger.info(f"   - Location: {resume_location}")
            logger.info(f"   - Career Years: {resume_career}")

            # 2. 필터링 조건 생성
            # 2.1 Location Filter: "서울 전체" -> "서울"로 검색
            location_filter = ""
            params = [resume_vector]
            
            if resume_location:
                # "서울 전체" -> "서울" (단순화된 파싱 로직)
                target_loc = resume_location.split(" ")[0] 
                location_filter = "AND jp.location LIKE %s"
                params.append(f"%{target_loc}%")
                logger.info(f"   👉 Applying Location Filter: LIKE '{target_loc}%'")

            # 2.2 Stack Filter: Overlap >= 2
            # Use Postgres Array Intersection: cardinality(resume_stack & job_stack) >= 2
            # Since resume_stack is constant for this query, easiest is to pass it as param.
            # But wait, resume_stack in DB is text[]. In Python it is list.
            # We can use '&&' operator for overlap check, but specifically for count >= 2.
            
            # Using array_length(ARRAY(SELECT unnest(jp.stack) INTERSECT SELECT unnest(%s::text[])), 1) >= 2
            # Or simpler if pgvector/postgres has array functions.
            # Standard Postgres: 
            # (SELECT count(*) FROM (SELECT unnest(jp.stack) INTERSECT SELECT unnest(%s::text[])) t) >= 2
            
            stack_filter = ""
            if resume_stack_raw and isinstance(resume_stack_raw, list):
                # Pass resume stack as array for comparison
                stack_filter = """
                    AND (
                        SELECT count(*) 
                        FROM (
                            SELECT unnest(jp.stack) 
                            INTERSECT 
                            SELECT unnest(%s::text[])
                        ) t
                    ) >= 2
                """
                params.append(resume_stack_raw) # psycopg2 handles list -> array
                logger.info(f"   👉 Applying Stack Filter: Overlap >= 2 with {resume_stack_raw}")

            # 2.3 Career Filter: resume.career >= job.required_experience (handle NULL as 0)
            career_filter = """
                AND %s >= COALESCE(jp.required_experience, 0)
            """
            params.append(resume_career)
            logger.info(f"   👉 Applying Career Filter: Resume({resume_career}) >= Job(Req)")

            # 3. 유사도 검색 (Cosine Similarity + Filtering)
            logger.info(f"🏃 Running hybrid search (Filter + Vector) Top {top_k}...")
            
            params.append(top_k) # Limit Param
            
            sql = f"""
                SELECT 
                    jp.job_id,
                    jp.title,
                    e.name as company_name,
                    jp.location,
                    jp.stack,
                    1 - (jp.embedding <=> %s) as similarity,
                    jp.required_experience
                FROM job_posting jp
                JOIN employer e ON jp.employer_id = e.employer_id
                WHERE jp.embedding IS NOT NULL 
                  AND jp.deleted_at IS NULL
                  {location_filter}
                  {stack_filter}
                  {career_filter}
                ORDER BY similarity DESC
                LIMIT %s
            """
            
            cur.execute(sql, tuple(params))
            results = cur.fetchall()
            
            print("\n" + "="*100)
            print(f"🎯 Hybrid Matching Results (Top {top_k})")
            print("="*100)
            print(f"{'Rank':<5} | {'Score':<8} | {'Job ID':<8} | {'Location':<10} | {'Job Stack':<30}")
            print("-" * 100)
            
            if not results:
                print("   ❌ 조건에 맞는 매칭 결과가 없습니다.")
            
            for rank, row in enumerate(results, 1):
                job_id = row[0]
                title = row[1]
                # company = row[2]
                location = row[3]
                stack = row[4]
                score = row[5]
                
                # Stack Truncate for display
                stack_disp = (stack[:27] + '...') if stack and len(stack) > 27 else stack
                
                print(f"{rank:<5} | {score:.4f}   | {job_id:<8} | {location[:10]:<10} | {str(stack_disp)}")
                
            print("="*100 + "\n")

    except Exception as e:
        logger.error(f"❌ Error during matching verification: {e}")
    finally:
        conn.close()

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    # Positional argument로 변경 (nargs='?'로 선택적 인자로 설정)
    parser.add_argument("resume_id", type=int, nargs='?', default=2, help="Target Resume ID")
    
    args = parser.parse_args()
    
    verify_matching(args.resume_id, top_k=5)