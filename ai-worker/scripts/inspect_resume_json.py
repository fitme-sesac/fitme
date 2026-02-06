import json
import sys
import os
import io

# 터미널 한글 출력 설정
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.core.database import get_db_connection

def get_resume_json(resume_id: int):
    conn = get_db_connection()
    try:
        with conn.cursor() as cur:
            # 1. Basic Info
            cur.execute("""
                SELECT title, re_stack, content, preference_location, preference_salary, employment_type, school_state, school_class
                FROM resume 
                WHERE resume_id = %s
            """, (resume_id,))
            resume_row = cur.fetchone()
            
            if not resume_row:
                print(f"Resume ID {resume_id} not found.")
                return

            education_data = None
            if resume_row[6] or resume_row[7]:
                education_data = {
                    "status": resume_row[6] or "",
                    "major": resume_row[7] or ""
                }

            full_data = {
                "resume_id": resume_id,
                "content": resume_row[2],
                "file_links": [],
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

            # 2. Projects
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
                    "contribution": "",
                    "description": row[4]
                })
            full_data["projects"] = projects

            # 3. Careers
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
                    "description": ""
                })
            full_data["careers"] = careers

            print(json.dumps(full_data, indent=2, ensure_ascii=False))

    except Exception as e:
        print(f"Error: {e}")
    finally:
        conn.close()

if __name__ == "__main__":
    rid = int(sys.argv[1]) if len(sys.argv) > 1 else 5002
    get_resume_json(rid)
