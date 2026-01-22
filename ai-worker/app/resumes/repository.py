from app.core.database import get_db_connection, init_db_extensions

class ResumeRepository:
    def update_resume_data(self, id: int, summary: str, vector: list[float]):
        """
        이력서의 요약 내용과 임베딩 벡터를 DB에 업데이트하는 함수입니다.

        Args:
            id (int): 이력서 ID (Primary Key)
            summary (str): AI가 생성한 8줄 요약 텍스트
            vector (list[float]): 1536차원의 임베딩 벡터 리스트

        Note:
            - `pgvector` 확장이 미리 등록되어 있어야 벡터 저장이 가능합니다.
            - `init_db_extensions(conn)`을 호출하여 세션마다 확장을 등록합니다.
            - `vector` 타입 컬럼에 리스트를 파이썬 리스트 그대로 넘기면 adapter가 자동으로 변환해줍니다.
        """
        conn = get_db_connection()
        try:
            # pgvector 확장 등록 (벡터 타입 인식을 위해 필수)
            init_db_extensions(conn)
            
            with conn.cursor() as cur:
                # SQL 실행: summary, embedding 컬럼 업데이트
                # summary_status를 'COMPLETED'로 변경하여 처리가 끝났음을 표시합니다.
                sql = """
                    UPDATE resume
                    SET summary = %s,
                        embedding = %s,
                        summary_status = 'COMPLETED',
                        updated_at = NOW()
                    WHERE resume_id = %s
                """
                cur.execute(sql, (summary, vector, id))
                
                # [오류 처리 강화] 업데이트된 행이 없다면 ID가 없는 것이므로 예외 발생
                if cur.rowcount == 0:
                    raise Exception(f"Update failed: Resume ID {id} does not exist in database.")
            
            # 트랜잭션 커밋 (영구 저장)
            conn.commit()
        finally:
            # 연결 종료 (리소스 반환)
            conn.close()

    def get_resume_by_id(self, resume_id: int) -> dict:
        """
        이력서 ID로 Resume, Career, Project 정보를 모두 조회하여 Dictionary로 반환합니다.
        반환 구조는 ResumeRequest 스키마와 호환됩니다.
        """
        conn = get_db_connection()
        data = {}
        try:
            with conn.cursor() as cur:
                # 1. Fetch Resume Basic Info
                cur.execute("""
                    SELECT title, re_stack, content, preference_location, preference_salary, employment_type
                    FROM resume 
                    WHERE resume_id = %s
                """, (resume_id,))
                resume_row = cur.fetchone()
                
                if not resume_row:
                    return None

                data = {
                    "resume_id": resume_id,
                    "content": resume_row[2],
                    "file_links": [], 
                    "basic_info": {
                        "title": resume_row[0],
                        "re_stack": resume_row[1] or [],
                        "field": "RESUME", # Default value as it's from resume table
                        "preference": {
                            "location": resume_row[3] or "",
                            "salary": resume_row[4] or "",
                            "employment_type": resume_row[5] or ""
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
                        "contribution": "", 
                        "description": row[4]
                    })
                data["projects"] = projects

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
                        "description": ""
                    })
                data["careers"] = careers
            
            return data
        finally:
            conn.close()

# 싱글톤처럼 사용하거나, 필요할 때 인스턴스화
resume_repo = ResumeRepository()
