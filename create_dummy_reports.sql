-- 더미 신고 데이터 생성 스크립트
-- 신고 관리 기능 테스트를 위한 데이터
-- 기존 시드 데이터의 회원들을 활용

BEGIN;

-- 1. 관리자 계정 생성 (신고 처리용)
INSERT INTO member (
    member_uid, email, login_id, auth_provider, password_hash,
    name, role, status,
    terms_notice_id, terms_agreed_at, privacy_notice_id, privacy_agreed_at, policy_notice_id, policy_agreed_at,
    created_at, updated_at
) VALUES (
    gen_random_uuid(), 'admin@fitme.com', 'admin', 'OTHER', '$2a$10$6v8Q1XxVMqG1CxS/U1MLr.Kmlwtj.0bRqW038QQIYSWDXgdZ7n5t2',
    '관리자', 'SERVICEADMIN', 'ACTIVE',
    1, now(), 2, now(), 3, now(),
    now(), now()
) ON CONFLICT (email) DO NOTHING;

-- 2. 다양한 상태의 신고 데이터 생성 (기존 회원 ID 100-109 활용)
INSERT INTO report (
    reporter_member_id,
    target_type,
    target_member_id,
    target_job_id,
    reason_code,
    reason_detail,
    status,
    created_at,
    updated_at
) VALUES
-- 1. 회원 신고 - 미처리 상태
(100, 'MEMBER', 101, NULL, 'INAPPROPRIATE', '부적절한 프로필 사진 및 내용', 'OPEN', now() - interval '3 days', now()),

-- 2. 회원 신고 - 미처리 상태 
(102, 'MEMBER', 103, NULL, 'HARASSMENT', '지속적인 괴롭힘 및 스팸 메시지', 'OPEN', now() - interval '2 days', now()),

-- 3. 회원 신고 - 미처리 상태
(104, 'MEMBER', 105, NULL, 'FAKE_PROFILE', '허위 프로필 정보 및 경력 조작', 'OPEN', now() - interval '1 day', now()),

-- 4. 채용공고 신고 - 미처리 상태 (job_id 1 사용)
(106, 'JOB_POSTING', NULL, 1, 'SPAM', '중복 게시 및 스팸성 채용공고', 'OPEN', now() - interval '4 hours', now()),

-- 5. 채용공고 신고 - 미처리 상태
(107, 'JOB_POSTING', NULL, 2, 'MISLEADING', '허위 채용 조건 및 기업 정보', 'OPEN', now() - interval '2 hours', now()),

-- 6. 회원 신고 - 승인된 상태
(108, 'MEMBER', 109, NULL, 'INAPPROPRIATE', '욕설 및 부적절한 언어 사용', 'ACCEPTED', now() - interval '7 days', now()),

-- 7. 회원 신고 - 거절된 상태
(100, 'MEMBER', 110, NULL, 'HARASSMENT', '단순 의견 차이로 인한 신고', 'REJECTED', now() - interval '5 days', now()),

-- 8. 기타 신고 - 미처리 상태
(101, 'ETC', NULL, NULL, 'SYSTEM_ABUSE', '시스템 악용 및 부정 사용', 'OPEN', now() - interval '6 hours', now()),

-- 9. 채용공고 신고 - 승인된 상태
(102, 'JOB_POSTING', NULL, 3, 'DISCRIMINATION', '성별/나이 차별적 채용 공고', 'ACCEPTED', now() - interval '10 days', now()),

-- 10. 회원 신고 - 거절된 상태
(103, 'MEMBER', 104, NULL, 'FAKE_PROFILE', '충분한 증거 없는 허위 신고', 'REJECTED', now() - interval '8 days', now());

-- 3. 승인/거절된 신고에 대한 조치 기록 생성
INSERT INTO moderation_action (
    report_id,
    admin_member_id,
    decision,
    sanction_level,
    restrict_days,
    reason,
    decided_at,
    created_at,
    updated_at
) VALUES
-- 승인된 신고 #6에 대한 조치
(6, (SELECT member_id FROM member WHERE email = 'admin@fitme.com'), 'ACCEPT', 2, 7, '부적절한 언어 사용으로 7일 제재', now() - interval '7 days', now() - interval '7 days', now()),

-- 거절된 신고 #7에 대한 조치
(7, (SELECT member_id FROM member WHERE email = 'admin@fitme.com'), 'REJECT', NULL, NULL, '단순 의견 차이로 신고 사유 부족', now() - interval '5 days', now() - interval '5 days', now()),

-- 승인된 신고 #9에 대한 조치
(9, (SELECT member_id FROM member WHERE email = 'admin@fitme.com'), 'ACCEPT', 3, 30, '차별적 채용공고로 30일 제재', now() - interval '10 days', now() - interval '10 days', now()),

-- 거절된 신고 #10에 대한 조치
(10, (SELECT member_id FROM member WHERE email = 'admin@fitme.com'), 'REJECT', NULL, NULL, '허위 신고로 판단됨', now() - interval '8 days', now() - interval '8 days', now());

-- 4. 결과 확인 쿼리
SELECT 
    r.report_id,
    r.target_type,
    r.reason_code,
    r.reason_detail,
    r.status,
    CASE 
        WHEN r.target_type = 'MEMBER' THEN (SELECT name FROM member WHERE member_id = r.target_member_id)
        WHEN r.target_type = 'JOB_POSTING' THEN (SELECT title FROM job_posting WHERE job_id = r.target_job_id)
        ELSE 'N/A'
    END as target_name,
    (SELECT name FROM member WHERE member_id = r.reporter_member_id) as reporter_name,
    r.created_at
FROM report r
ORDER BY r.report_id;

COMMIT;