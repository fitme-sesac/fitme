-- 광고 캠페인 상태 체크 제약조건에 'DELETED' 추가
-- 기존: ACTIVE, PAUSED, ENDED
-- 변경: ACTIVE, PAUSED, ENDED, DELETED

ALTER TABLE ad_campaign DROP CONSTRAINT ck_ad_campaign_status;

ALTER TABLE ad_campaign ADD CONSTRAINT ck_ad_campaign_status 
    CHECK (status IN ('ACTIVE', 'PAUSED', 'ENDED', 'DELETED'));
