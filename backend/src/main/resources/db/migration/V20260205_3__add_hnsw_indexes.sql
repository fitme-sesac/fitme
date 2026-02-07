-- job_posting 테이블의 embedding 컬럼에 HNSW 인덱스 추가 (Cosine Distance)
-- m=16, ef_construction=64는 일반적인 성능/인덱싱 속도 균형값입니다.
CREATE INDEX IF NOT EXISTS idx_job_posting_embedding_hnsw 
ON job_posting USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- resume 테이블의 embedding 컬럼에 HNSW 인덱스 추가 (Cosine Distance)
CREATE INDEX IF NOT EXISTS idx_resume_embedding_hnsw 
ON resume USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);
