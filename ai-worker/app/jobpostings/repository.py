from app.core.database import get_db_connection, init_db_extensions
import logging

class JobRepository:
    def __init__(self):
        self.logger = logging.getLogger(__name__)

    def get_job_posting(self, job_id: int) -> dict | None:
        """
        [채용 공고 상세 조회]
        임베딩 생성을 위해 필요한 채용 공고의 핵심 정보를 조회합니다.
        Employer 테이블과 조인하여 산업군(industry) 정보도 함께 가져옵니다.
        
        Target Table: job_posting (jp), employer (e)
        Columns: jp.job_id, jp.title, jp.description, jp.stack, jp.summary, jp.location, jp.status, e.industry
        """
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                sql = """
                    SELECT 
                        jp.job_id, 
                        jp.title, 
                        jp.description, 
                        jp.stack, 
                        jp.summary, 
                        jp.location, 
                        jp.status,
                        e.industry
                    FROM job_posting jp
                    JOIN employer e ON jp.employer_id = e.employer_id
                    WHERE jp.job_id = %s AND jp.deleted_at IS NULL
                """
                cur.execute(sql, (job_id,))
                row = cur.fetchone()
                
                if row:
                    # 컬럼명과 값을 매핑하여 딕셔너리로 반환
                    columns = [desc[0] for desc in cur.description]
                    return dict(zip(columns, row))
                return None
        except Exception as e:
            self.logger.error(f"Error fetching job posting {job_id}: {e}")
            raise e
        finally:
            conn.close()

    def update_job_embedding(self, job_id: int, vector: list[float]) -> bool:
        """
        [Job Posting 테이블 업데이트]
        생성된 임베딩 벡터를 'job_posting' 테이블에 저장합니다.
        
        Target Table: job_posting
        Target Columns: embedding, updated_at
        
        Returns:
            bool: 업데이트 성공(대상 행 존재) 여부
        """
        conn = get_db_connection()
        try:
            # pgvector 확장 등록 (벡터 타입 인식을 위해 필수)
            init_db_extensions(conn)
            
            with conn.cursor() as cur:
                sql = """
                    UPDATE job_posting
                    SET embedding = %s,
                        updated_at = NOW()
                    WHERE job_id = %s
                """
                
                cur.execute(sql, (vector, job_id))
                
                # Check if update confirmed
                if cur.rowcount == 0:
                    self.logger.warning(f"Update failed: Job ID {job_id} does not exist.")
                    update_success = False
                else:
                    self.logger.info(f"Successfully updated embedding for Job ID {job_id}")
                    update_success = True
            
            conn.commit()
            return update_success
            
        except Exception as e:
            conn.rollback()
            self.logger.error(f"DB Error during job embedding update: {e}")
            raise e
        finally:
            conn.close()

    def get_target_job_ids(self, only_null: bool = True) -> list[int]:
        """
        [배치 처리용] 임베딩 생성 대상 Job ID 목록 조회
        
        Args:
            only_null (bool): True면 embedding이 NULL인 것만 조회 (이어하기)
                              False면 모든 활성 공고 조회 (전체 갱신)
        """
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                # 기본 조건: 삭제되지 않은(Active) 공고
                conditions = ["deleted_at IS NULL"]
                
                if only_null:
                    conditions.append("embedding IS NULL")
                
                where_clause = " AND ".join(conditions)
                
                sql = f"""
                    SELECT job_id 
                    FROM job_posting 
                    WHERE {where_clause}
                    ORDER BY job_id ASC
                """
                
                cur.execute(sql)
                rows = cur.fetchall()
                
                return [row[0] for row in rows]
                
        except Exception as e:
            self.logger.error(f"Error fetching target job ids: {e}")
            return []
        finally:
            conn.close()

job_repo = JobRepository()
