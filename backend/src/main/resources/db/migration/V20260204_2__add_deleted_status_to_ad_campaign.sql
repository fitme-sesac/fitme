-- 광고 캠페인 상태 체크 제약조건에 'DELETED' 추가 (idempotent)
DO $$ 
BEGIN 
    -- 기존 제약조건 삭제
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name='ck_ad_campaign_status' AND table_name='ad_campaign') THEN
        ALTER TABLE ad_campaign DROP CONSTRAINT ck_ad_campaign_status;
    END IF;

    -- 새로운 제약조건 추가
    ALTER TABLE ad_campaign ADD CONSTRAINT ck_ad_campaign_status 
        CHECK (status IN ('ACTIVE', 'PAUSED', 'ENDED', 'DELETED'));
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'Constraint update failed or already applied: %', SQLERRM;
END $$;
