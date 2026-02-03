-- Resume 테이블에 임베딩 생성용 원본 텍스트 컬럼 추가
ALTER TABLE resume ADD COLUMN IF NOT EXISTS embedding_text TEXT;

-- 기존 데이터가 있다면 (선택사항) summary를 복사해둘 수도 있지만, 
-- 새로 요약을 생성하는 것이 검색 품질에 더 좋습니다.
COMMENT ON COLUMN resume.embedding_text IS 'AI 임베딩 생성 시 사용된 원본 텍스트 (검색/매칭용)';
