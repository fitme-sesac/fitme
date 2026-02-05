BEGIN;

-- ✅ 기존 상품 데이터 삭제
-- 주의: orders/subscription 등이 product를 FK로 참조하므로 CASCADE면 연관 데이터도 삭제될 수 있음
TRUNCATE TABLE product RESTART IDENTITY CASCADE;

-- ✅ 더미 상품 데이터 생성 (ONE_TIME 6개 + SUBSCRIPTION 3개)
-- ONE_TIME: 고가 상품일수록 보너스 크레딧 제공(단가 개선)
INSERT INTO product (
  product_code,
  product_type,
  name,
  sale_status,
  price_amount,
  currency,
  credit_amount,
  plan_tier
) VALUES
  /* =========================
   * ONE_TIME (크레딧 충전)
   * - 저가: 1:10 유지
   * - 고가: 보너스 크레딧으로 실질 단가 개선
   * ========================= */
  ('OT_CREDIT_1000',  'ONE_TIME', '크레딧 1,000 충전',                 'ON_SALE',  10000.00, 'KRW',  1000,  NULL), -- 1:10
  ('OT_CREDIT_3000',  'ONE_TIME', '크레딧 3,000 충전',                 'ON_SALE',  30000.00, 'KRW',  3000,  NULL), -- 1:10

  ('OT_CREDIT_5500',  'ONE_TIME', '크레딧 5,500 충전 (10% 보너스)',     'ON_SALE',  50000.00, 'KRW',  5500,  NULL), -- 5,000 + 10%
  ('OT_CREDIT_11500', 'ONE_TIME', '크레딧 11,500 충전 (15% 보너스)',    'ON_SALE', 100000.00, 'KRW', 11500,  NULL), -- 10,000 + 15%
  ('OT_CREDIT_24000', 'ONE_TIME', '크레딧 24,000 충전 (20% 보너스)',    'ON_SALE', 200000.00, 'KRW', 24000,  NULL), -- 20,000 + 20%
  ('OT_CREDIT_65000', 'ONE_TIME', '크레딧 65,000 충전 (30% 보너스)',    'ON_SALE', 500000.00, 'KRW', 65000,  NULL), -- 50,000 + 30%

  /* =========================
   * SUBSCRIPTION (구독)
   * - STANDARD/PRO는 가격대를 더 높게 상향
   * - (가치감 유지 위해 포함 크레딧도 함께 상향)
   * ========================= */
  ('SUB_BASIC',    'SUBSCRIPTION', '구독 BASIC (월 10,000 크레딧)',     'ON_SALE',  99000.00, 'KRW',  10000, 'BASIC'),
  ('SUB_STANDARD', 'SUBSCRIPTION', '구독 STANDARD (월 60,000 크레딧)',  'ON_SALE', 399000.00, 'KRW',  60000, 'STANDARD'),
  ('SUB_PRO',      'SUBSCRIPTION', '구독 PRO (월 140,000 크레딧)',      'ON_SALE', 799000.00, 'KRW', 140000, 'PRO');

COMMIT;
