import asyncio
import sys
import os
import io
import logging

# 배치 프로세스 -> 아이디 입력하면 resume에 대한 데이터 조회해서 요약 하고 임베딩하고 업데이트 하는 과정
# 제일 아래 매칭 실행하는 건 주석 처리로 해놓았음 -> 데이터 임베딩 한번에 업데이트 할 때는 주석하고 사용 테스트 해보고 싶은 건 주석 풀고 사용하기
# 터미널 한글 출력 설정
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.core.database import get_db_connection, init_db_extensions
from app.resumes.services.summary import summary_service
from app.resumes.services.vector import vector_service
from app.resumes.repository import resume_repo
from app.resumes.schemas import SummaryType

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ProcessExistingResume")

async def process_resume_by_id(resume_id: int):
    """
    DB에서 Resume, Career, Project 정보를 조회하여
    요약(Summary) 및 임베딩(Vector)을 생성하고 업데이트합니다.
    기존 데이터들도 새 로직을 태워서 요약문이랑 임베딩을 다시 만들어주고 싶다 일때 사용
    """
    conn = get_db_connection()
    raw_data = {}
    
    try:
        with conn.cursor() as cur:
            # 1. Fetch Resume Basic Info
            cur.execute("""
                SELECT title, re_stack, content, preference_location, preference_salary, employment_type, school_state, school_class
                FROM resume 
                WHERE resume_id = %s
            """, (resume_id,))
            resume_row = cur.fetchone()
            
            if not resume_row:
                logger.error(f"❌ Resume ID {resume_id} not found.")
                return

            # Education Mapping
            education_data = None
            if resume_row[6] or resume_row[7]:
                education_data = {
                    "status": resume_row[6] or "",
                    "major": resume_row[7] or ""
                }


            raw_data = {
                "resume_id": resume_id,  # [NEW] Log용 ID 추가
                "content": resume_row[2],
                "file_links": [], # DB doesn't store file links in a simple column yet, assuming empty or need separate table
                "basic_info": {
                    "title": resume_row[0],
                    "re_stack": resume_row[1] or [],
                    "education": education_data,
                    "preference": {
                        "location": resume_row[3],
                        "salary": resume_row[4],
                        "employment_type": resume_row[5]
                    }
                },
                "summary_type": "STRUCTURED",
                "include_reasoning": False
            }

            # 2. Fetch Projects
            cur.execute("""
                SELECT title, start_date, end_date, tech_stack, description
                FROM resume_project
                WHERE resume_id = %s
            """, (resume_id,))
            projects = []
            for row in cur.fetchall():
                projects.append({
                    "project_name": row[0],
                    "start_date": str(row[1]),
                    "end_date": str(row[2]) if row[2] else "",
                    "total_tech_stack": row[3].split(",") if row[3] else [],
                    "contribution": "", # Merged into description usually
                    "description": row[4]
                })
            raw_data["projects"] = projects

            # 3. Fetch Careers
            cur.execute("""
                SELECT company_name, role, start_date, end_date
                FROM resume_career
                WHERE resume_id = %s
            """, (resume_id,))
            careers = []
            for row in cur.fetchall():
                careers.append({
                    "company_name": row[0],
                    "role": row[1],
                    "start_date": str(row[2]),
                    "end_date": str(row[3]) if row[3] else "",
                    "description": "" # DB doesn't have description column in previous script
                })
            raw_data["careers"] = careers
            
        conn.close()
        
        logger.info(f"✅ Fetched Resume Data for ID {resume_id}")
        logger.info(f"   - Projects: {len(projects)}")
        logger.info(f"   - Careers: {len(careers)}")
        
        # 4. Process AI Summary
        # 4. Process AI Summary
        logger.info("🔄 Generating Summary & Embedding...")
        # [Refactor] generate_summary returns (result, eval_info)
        result_obj, eval_info = await summary_service.generate_summary(
            data=raw_data, 
            type="RESUME", 
            summary_type=SummaryType.STRUCTURED
        )
        
        logger.info(f"📊 Eval Info: {eval_info}")
        
        summary_text = result_obj.to_formatted_string(include_reasoning=False)
        
        meta_title = raw_data["basic_info"]["title"]
        meta_stack = ", ".join(raw_data["basic_info"]["re_stack"])
        
        # This will now use the Korean job_category if extracted
        embedding_text = result_obj.to_embedding_string(title=meta_title, tech_stack=meta_stack)
        
        # [DEBUG] Print full embedding text to verify format
        print("\n" + "="*40 + " [Generated Embedding Text (Current)] " + "="*40)
        print(embedding_text)
        print("="*106 + "\n")
        
        logger.info(f"📝 Generated Embedding Text Length: {len(embedding_text)}")

        # 5. Generate Vector
        vector = await vector_service.generate_vector(embedding_text)
        
        # 6. Update DB
        resume_repo.update_resume_data(resume_id, summary_text, vector, eval_info=eval_info)
        logger.info(f"✅ Resume {resume_id} updated successfully!")

    except Exception as e:
        logger.error(f"❌ Error processing resume {resume_id}: {e}")
        if conn: conn.close()

# python scripts/batch_process_resume.py 123 처럼 ID를 명시하면 -> ID 123번 이력서를 처리
# python scripts/batch_process_resume.py 처럼 그냥 실행하면 -> 자동으로 5002번 이력서(아마도 개발 중 자주 쓰던 테스트용 데이터)를 처리
if __name__ == "__main__":
    import sys
    
    # Usage: 
    # 1. python scripts/batch_process_resume.py 5099 (Single)
    # 2. python scripts/batch_process_resume.py 5090 5099 (Range)
    
    if len(sys.argv) == 3:
        try:
            start_id = int(sys.argv[1])
            end_id = int(sys.argv[2])
            print(f"🚀 Batch Processing IDs: {start_id} ~ {end_id}")
            
            for rid in range(start_id, end_id + 1):
                print(f"\n[{rid}] Processing...")
                asyncio.run(process_resume_by_id(rid))
                
        except ValueError:
            print("Error: IDs must be integers.")
            sys.exit(1)
            
    elif len(sys.argv) == 2:
        try:
            resume_id = int(sys.argv[1])
            print(f"🚀 Processing Single ID: {resume_id}")
            asyncio.run(process_resume_by_id(resume_id))
        except ValueError:
            print("Error: Resume ID must be an integer.")
            sys.exit(1)
    else:
        print("Usage: python scripts/batch_process_resume.py <start_id> [end_id]")
        print("Defaulting to ID 5002 for testing.")
        asyncio.run(process_resume_by_id(5002))

    # [Optional] 검증을 위해 매칭 결과 즉시 확인
    # 스크립트 실행 시 같은 폴더에 있는 verify_matching 모듈을 불러와서 실행
    # from verify_matching import verify_matching
    # print("\n" + "="*60)
    # logging.info("Verifying matching results for updated resume...")
    # verify_matching(resume_id)
