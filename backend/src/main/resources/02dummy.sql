-- 관리자 계정 upsert (재실행 시 기존 행 갱신)
INSERT INTO member (
    member_uid, email, login_id, auth_provider, password_hash,
    name, gender, birth_date, phone, role, status,
    phone_verified_at,
    terms_notice_id, terms_agreed_at,
    privacy_notice_id, privacy_agreed_at,
    policy_notice_id, policy_agreed_at,
    marketing_opt_in, marketing_agreed_at,
    created_at, updated_at
) VALUES (
    gen_random_uuid(),
    'admin@fitme.com',
    'admin',
    'OTHER',
    '$2a$10$6v8Q1XxVMqG1CxS/U1MLr.Kmlwtj.0bRqW038QQIYSWDXgdZ7n5t2',
    '관리자',
    'MALE',
    '2024-01-01',
    '01000000000',
    'SERVICEADMIN',
    'ACTIVE',
    now(),
    1, now(),
    2, now(),
    3, now(),
    false, null,
    now(),
    now()
)
ON CONFLICT (email) DO UPDATE SET
    login_id = EXCLUDED.login_id,
    password_hash = EXCLUDED.password_hash,
    name = EXCLUDED.name,
    role = EXCLUDED.role,
    status = EXCLUDED.status,
    phone_verified_at = EXCLUDED.phone_verified_at,
    terms_notice_id = EXCLUDED.terms_notice_id,
    terms_agreed_at = EXCLUDED.terms_agreed_at,
    privacy_notice_id = EXCLUDED.privacy_notice_id,
    privacy_agreed_at = EXCLUDED.privacy_agreed_at,
    policy_notice_id = EXCLUDED.policy_notice_id,
    policy_agreed_at = EXCLUDED.policy_agreed_at,
    updated_at = now();