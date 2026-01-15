from app.core.database import get_db_connection, init_db_extensions

class ResumeRepository:
    def update_resume_data(self, id: int, summary: str, vector: list[float]):
        """
        Update a resume record with an AI-generated summary and its embedding vector.
        
        Updates the resume row identified by `id` setting `summary`, `embedding`, `summary_status` to 'COMPLETED', and `updated_at` to the current timestamp. Requires the `pgvector` extension to be initialized for the session before storing the embedding.
        
        Parameters:
            id (int): Resume primary key.
            summary (str): AI-generated summary text (approximately eight lines).
            vector (list[float]): Embedding vector (expected 1536 dimensions); a Python list is adapted to the database vector type when `pgvector` is registered.
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
            
            # 트랜잭션 커밋 (영구 저장)
            conn.commit()
        finally:
            # 연결 종료 (리소스 반환)
            conn.close()

# 싱글톤처럼 사용하거나, 필요할 때 인스턴스화
resume_repo = ResumeRepository()