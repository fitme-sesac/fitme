-- 상품 데이터 upsert (재실행·배포 시 오류 없음, TRUNCATE 없이 product_code 기준 갱신)
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
  ('OT_CREDIT_1000',  'ONE_TIME', '크레딧 1,000 충전',                 'ON_SALE',  10000.00, 'KRW',  1000,  NULL),
  ('OT_CREDIT_3000',  'ONE_TIME', '크레딧 3,000 충전',                 'ON_SALE',  30000.00, 'KRW',  3000,  NULL),
  ('OT_CREDIT_5500',  'ONE_TIME', '크레딧 5,500 충전 (10% 보너스)',     'ON_SALE',  50000.00, 'KRW',  5500,  NULL),
  ('OT_CREDIT_11500', 'ONE_TIME', '크레딧 11,500 충전 (15% 보너스)',    'ON_SALE', 100000.00, 'KRW', 11500,  NULL),
  ('OT_CREDIT_24000', 'ONE_TIME', '크레딧 24,000 충전 (20% 보너스)',    'ON_SALE', 200000.00, 'KRW', 24000,  NULL),
  ('OT_CREDIT_65000', 'ONE_TIME', '크레딧 65,000 충전 (30% 보너스)',    'ON_SALE', 500000.00, 'KRW', 65000,  NULL),
  ('SUB_BASIC',    'SUBSCRIPTION', '구독 BASIC (월 10,000 크레딧)',     'ON_SALE',  99000.00, 'KRW',  10000, 'BASIC'),
  ('SUB_STANDARD', 'SUBSCRIPTION', '구독 STANDARD (월 60,000 크레딧)',  'ON_SALE', 399000.00, 'KRW',  60000, 'STANDARD'),
  ('SUB_PRO',      'SUBSCRIPTION', '구독 PRO (월 140,000 크레딧)',      'ON_SALE', 799000.00, 'KRW', 140000, 'PRO')
ON CONFLICT (product_code) DO UPDATE SET
  product_type = EXCLUDED.product_type,
  name = EXCLUDED.name,
  sale_status = EXCLUDED.sale_status,
  price_amount = EXCLUDED.price_amount,
  currency = EXCLUDED.currency,
  credit_amount = EXCLUDED.credit_amount,
  plan_tier = EXCLUDED.plan_tier,
  updated_at = now();
