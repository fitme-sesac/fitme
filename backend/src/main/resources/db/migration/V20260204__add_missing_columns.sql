-- Wallet 테이블에 reserved_balance 추가
ALTER TABLE wallet ADD COLUMN reserved_balance BIGINT NOT NULL DEFAULT 0;

-- AdClickEvent 테이블에 click_cost 추가
ALTER TABLE ad_click_event ADD COLUMN click_cost INT NOT NULL DEFAULT 0;
