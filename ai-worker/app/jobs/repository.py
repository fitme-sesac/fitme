from app.core.database import get_db_connection, init_db_extensions

class JobRepository:
    def update_job_data(self, job_id: int, summary: str, vector: list[float]):
        """
        채용공고의 요약 내용과 임베딩 벡터를 DB에 업데이트하는 함수입니다.

        Args:
            job_id (int): 채용공고 ID (Primary Key)
            summary (str): AI가 생성한 요약 텍스트
            vector (list[float]): 1536차원의 임베딩 벡터 리스트

        Note:
            - `pgvector` 확장이 미리 등록되어 있어야 벡터 저장이 가능합니다.
            - `init_db_extensions(conn)`을 호출하여 세션마다 확장을 등록합니다.
        """
        conn = get_db_connection()
        try:
            # pgvector 확장 등록
            init_db_extensions(conn)
            
            with conn.cursor() as cur:
                # SQL 실행: summary, embedding 컬럼 업데이트
                sql = """
                    UPDATE job_posting
                    SET summary = %s,
                        embedding = %s,
                        updated_at = NOW()
                    WHERE job_id = %s
                """
                cur.execute(sql, (summary, vector, job_id))
                
                # 업데이트된 행이 없다면 ID가 없는 것이므로 예외 발생
                if cur.rowcount == 0:
                    raise Exception(f"Update failed: Job ID {job_id} does not exist in database.")
            
            # 트랜잭션 커밋
            conn.commit()
        finally:
            # 연결 종료
            conn.close()

# 싱글톤처럼 사용
job_repo = JobRepository()
