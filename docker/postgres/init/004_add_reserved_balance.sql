-- ================================
-- Migration: Add reserved_balance column to wallet table
-- Date: 2026-02-03
-- Description: Phase 2 광고 예산 예약 기능을 위한 reserved_balance 컬럼 추가
-- Database: fitme_project
-- ================================

-- wallet 테이블에 reserved_balance 컬럼 추가
ALTER TABLE wallet 
ADD COLUMN IF NOT EXISTS reserved_balance BIGINT NOT NULL DEFAULT 0;

-- reserved_balance는 0 이상이어야 함
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'ck_wallet_reserved_balance_non_negative'
    ) THEN
        ALTER TABLE wallet 
        ADD CONSTRAINT ck_wallet_reserved_balance_non_negative 
        CHECK (reserved_balance >= 0);
    END IF;
END $$;

-- 주석 추가
COMMENT ON COLUMN wallet.reserved_balance IS '예약된 잔액 (광고 예산 등)';
