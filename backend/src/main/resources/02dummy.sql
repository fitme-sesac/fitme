INSERT INTO member (
    member_uid, email, login_id, auth_provider, password_hash,
    name, gender, birth_date, phone, role, status,
    phone_verified_at,
    terms_notice_id, terms_agreed_at,      -- 이용약관 (필수)
    privacy_notice_id, privacy_agreed_at,  -- 개인정보 (필수)
    policy_notice_id, policy_agreed_at,    -- 위치기반 (선택이지만 값 채움)
    marketing_opt_in, marketing_agreed_at,
    created_at, updated_at
) VALUES (
    gen_random_uuid(),
    'admin@fitme.com',
    'admin',
    'OTHER',
    '$2a$10$6v8Q1XxVMqG1CxS/U1MLr.Kmlwtj.0bRqW038QQIYSWDXgdZ7n5t2', -- test1234!
    '관리자',
    'MALE',
    '2024-01-01',
    '01000000000',
    'SERVICEADMIN', -- 관리자 권한
    'ACTIVE',
    now(),
    1, now(),       -- terms_notice_id=1
    2, now(),       -- privacy_notice_id=2
    3, now(),       -- policy_notice_id=3
    false, null,    -- 마케팅 동의 미수신
    now(),
    now()
);