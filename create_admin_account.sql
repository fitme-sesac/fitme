-- 관리자 계정 생성 스크립트
-- 비밀번호: test1234!

BEGIN;

-- 기존 관리자 계정 삭제 (있다면)
DELETE FROM member WHERE email = 'admin@fitme.com' OR login_id = 'admin';

-- 새 관리자 계정 생성
INSERT INTO member (
    member_uid, 
    email, 
    login_id, 
    auth_provider, 
    password_hash,
    name, 
    role, 
    status,
    terms_notice_id, 
    terms_agreed_at, 
    privacy_notice_id, 
    privacy_agreed_at, 
    policy_notice_id, 
    policy_agreed_at,
    created_at, 
    updated_at
) VALUES (
    gen_random_uuid(), 
    'admin@fitme.com', 
    'admin', 
    'OTHER', 
    '$2a$10$6v8Q1XxVMqG1CxS/U1MLr.Kmlwtj.0bRqW038QQIYSWDXgdZ7n5t2',  -- test1234!
    '시스템 관리자', 
    'SERVICEADMIN', 
    'ACTIVE',
    1, 
    now(), 
    2, 
    now(), 
    3, 
    now(),
    now(), 
    now()
);

-- 결과 확인
SELECT 
    member_id,
    email,
    login_id,
    name,
    role,
    status,
    created_at
FROM member 
WHERE email = 'admin@fitme.com' OR login_id = 'admin';

COMMIT;