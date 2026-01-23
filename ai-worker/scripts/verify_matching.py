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

            # 2. 필터링 조건 (User Request: 필터 제거 확인)
            # 필터링 로직을 제거하고 벡터 유사도 검색만 수행합니다.
            
            # Params: [Embedding Vector, Limit]
            params = [resume_vector, top_k]
            
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