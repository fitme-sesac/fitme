package com.example.pproject.user.entity;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.Constant.SocialType;
import com.example.pproject.user.entity.convert.RoleTypeConverter;
import com.example.pproject.user.entity.convert.SocialTypeConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ✅ PostgreSQL DDL의 member 테이블 구조를 기준으로 JPA 매핑을 정리.
 * - 기존 서비스/레포지토리 호환을 위해 필드명(userid/username/birthday/password)은 유지
 * - 실제 컬럼명은 DDL 컬럼명(login_id/name/birth_date/password_hash 등)에 매핑
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"password"})
@Table(name = "member")
@Entity
public class UserEntity {

    // =========================================================
    // PK / UID
    // =========================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "member_uid", nullable = false, unique = true)
    private UUID memberUid;

    // =========================================================
    // 인증 / 식별
    // =========================================================
    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    /** 일반 가입 아이디(login_id). 소셜 가입은 NULL 허용 */
    @Column(name = "login_id", length = 50)
    private String userid;

    /** 비밀번호 해시(password_hash). 소셜 전용은 NULL */
    @Column(name = "password_hash", length = 255)
    private String password;

    /** DDL의 auth_provider에 매핑(NAVER/GOOGLE/KAKAO/OTHER) */
    @Convert(converter = SocialTypeConverter.class)
    @Column(name = "auth_provider", nullable = false, length = 20)
    private SocialType socialType;

    // =========================================================
    // 개인정보(SSOT: member)
    // =========================================================
    @Column(name = "name", nullable = false, length = 80)
    private String username;

    @Column(name = "gender", length = 20)
    private String gender;

    /** DDL의 birth_date (DATE). 기존 코드 호환을 위해 String 유지(YYYY-MM-DD 권장) */
    @Column(name = "birth_date")
    private String birthday;

    /** 소셜 가입자는 NULL 허용(추후 연결). */
    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    // =========================================================
    // 권한/상태
    // =========================================================
    /** DDL의 role에 매핑(CANDIDATE/EMPLOYER/SERVICEADMIN/APPROVEADMIN/MASTER) */
    @Convert(converter = RoleTypeConverter.class)
    @Column(name = "role", nullable = false, length = 20)
    private RoleType roleType;

    /** DDL의 status(ACTIVE/SUSPENDED/WITHDRAWN). 기존 enum 도입 전까지 String 유지 */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    // =========================================================
    // 보안/로그인 상태
    // =========================================================
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "failed_login_count", nullable = false)
    private Integer failedLoginCount;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    // =========================================================
    // 정책 동의(테이블 추가 없이 member 컬럼에 저장)
    // =========================================================
    @Column(name = "terms_notice_id")
    private Long termsNoticeId;
    @Column(name = "terms_agreed_at")
    private LocalDateTime termsAgreedAt;

    @Column(name = "privacy_notice_id")
    private Long privacyNoticeId;
    @Column(name = "privacy_agreed_at")
    private LocalDateTime privacyAgreedAt;

    @Column(name = "policy_notice_id")
    private Long policyNoticeId;
    @Column(name = "policy_agreed_at")
    private LocalDateTime policyAgreedAt;

    @Column(name = "marketing_opt_in", nullable = false)
    private Boolean marketingOptIn;
    @Column(name = "marketing_agreed_at")
    private LocalDateTime marketingAgreedAt;

    @Column(name = "consent_ip", length = 45)
    private String consentIp;

    @Column(name = "consent_user_agent", columnDefinition = "TEXT")
    private String consentUserAgent;

    // =========================================================
    // 감사/삭제
    // =========================================================
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // =========================================================
    // 레거시(기존 프론트/DTO 호환) - DDL에 없는 컬럼은 제거
    // =========================================================
    // postcode/address/detailAddress/extraAddress는 추후 Profile 테이블로 분리하는 것을 권장.

    @PrePersist
    void prePersist() {
        if (memberUid == null) {
            memberUid = UUID.randomUUID();
        }
        if (socialType == null) {
            // DDL 기본값은 OTHER에 해당. 현 enum에서는 OTHER를 기본으로 둔다.
            socialType = SocialType.OTHER;
        }
        if (roleType == null) {
            roleType = RoleType.CANDIDATE;
        }
        if (failedLoginCount == null) {
            failedLoginCount = 0;
        }
        if (marketingOptIn == null) {
            marketingOptIn = Boolean.FALSE;
        }
        if (status == null || status.isBlank()) {
            // 요구 DDL 기본값(예: ACTIVE). 실제 가입 플로우에서 정책 동의 후 ACTIVE 세팅 권장.
            status = "ACTIVE";
        }
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
