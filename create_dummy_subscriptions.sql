-- 더미 구독 데이터 생성 스크립트
-- 관리자 패널에서 구독 통계를 확인하기 위한 테스트 데이터

BEGIN;

-- 1. 더미 기업 데이터 생성 (구독을 위해 필요)
INSERT INTO employer (employer_uid, name, industry, location, status) VALUES
(gen_random_uuid(), '테스트 기업 A', 'IT', '서울', 'ACTIVE'),
(gen_random_uuid(), '테스트 기업 B', 'IT', '부산', 'ACTIVE'),
(gen_random_uuid(), '테스트 기업 C', 'IT', '대구', 'ACTIVE'),
(gen_random_uuid(), '테스트 기업 D', 'IT', '인천', 'ACTIVE'),
(gen_random_uuid(), '테스트 기업 E', 'IT', '광주', 'ACTIVE')
ON CONFLICT DO NOTHING;

-- 2. 더미 구독 데이터 생성
-- 활성 구독 3개, 취소된 구독 1개, 결제 실패 구독 1개
INSERT INTO subscription (
    employer_id, 
    product_id, 
    status, 
    started_at, 
    next_billing_at,
    customer_key,
    billing_key
) VALUES
-- 활성 구독들
(
    (SELECT employer_id FROM employer WHERE name = '테스트 기업 A' LIMIT 1),
    (SELECT product_id FROM product WHERE product_code = 'SUB_BASIC' LIMIT 1),
    'ACTIVE',
    now() - interval '30 days',
    now() + interval '30 days',
    'test_customer_key_1',
    'test_billing_key_1'
),
(
    (SELECT employer_id FROM employer WHERE name = '테스트 기업 B' LIMIT 1),
    (SELECT product_id FROM product WHERE product_code = 'SUB_STANDARD' LIMIT 1),
    'ACTIVE',
    now() - interval '15 days',
    now() + interval '15 days',
    'test_customer_key_2',
    'test_billing_key_2'
),
(
    (SELECT employer_id FROM employer WHERE name = '테스트 기업 C' LIMIT 1),
    (SELECT product_id FROM product WHERE product_code = 'SUB_PRO' LIMIT 1),
    'ACTIVE',
    now() - interval '7 days',
    now() + interval '23 days',
    'test_customer_key_3',
    'test_billing_key_3'
),
-- 취소된 구독
(
    (SELECT employer_id FROM employer WHERE name = '테스트 기업 D' LIMIT 1),
    (SELECT product_id FROM product WHERE product_code = 'SUB_BASIC' LIMIT 1),
    'CANCELED',
    now() - interval '60 days',
    NULL,
    'test_customer_key_4',
    'test_billing_key_4'
),
-- 결제 실패 구독
(
    (SELECT employer_id FROM employer WHERE name = '테스트 기업 E' LIMIT 1),
    (SELECT product_id FROM product WHERE product_code = 'SUB_STANDARD' LIMIT 1),
    'PAYMENT_FAILED',
    now() - interval '45 days',
    now() - interval '15 days',
    'test_customer_key_5',
    'test_billing_key_5'
);

-- 3. 결과 확인
SELECT 
    '전체 구독' as category,
    COUNT(*) as count
FROM subscription
UNION ALL
SELECT 
    '활성 구독' as category,
    COUNT(*) as count
FROM subscription 
WHERE status = 'ACTIVE'
UNION ALL
SELECT 
    '결제 실패' as category,
    COUNT(*) as count
FROM subscription 
WHERE status = 'PAYMENT_FAILED';

COMMIT;