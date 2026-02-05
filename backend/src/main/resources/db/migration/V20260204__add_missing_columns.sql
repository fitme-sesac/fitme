-- Wallet 테이블에 reserved_balance 추가 (idempotent)
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='wallet' AND column_name='reserved_balance') THEN
        ALTER TABLE wallet ADD COLUMN reserved_balance BIGINT NOT NULL DEFAULT 0;
    END IF;
END $$;

-- AdClickEvent 테이블에 click_cost 추가 (idempotent)
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='ad_click_event' AND column_name='click_cost') THEN
        ALTER TABLE ad_click_event ADD COLUMN click_cost INT NOT NULL DEFAULT 0;
    END IF;
END $$;
