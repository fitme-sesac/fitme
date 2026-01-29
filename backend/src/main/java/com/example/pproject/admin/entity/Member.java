package com.example.pproject.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "member_uid", nullable = false, unique = true)
    private UUID memberUid;

    @Column(length = 320, nullable = false, unique = true)
    private String email;

    @Column(name = "login_id", length = 50)
    private String loginId;

    // ✅ [추가 1] 비밀번호 (DB 컬럼명: password_hash)
    @Column(name = "password_hash")
    private String passwordHash;

    // ✅ [추가 2] 인증 제공자 (DB 컬럼명: auth_provider, NOT NULL)
    @Column(name = "auth_provider", length = 20, nullable = false)
    private String authProvider;

    // ✅ [추가 3] 로그인 실패 횟수 (DB 컬럼명: failed_login_count, NOT NULL)
    @Column(name = "failed_login_count", nullable = false)
    @Builder.Default // 빌더 사용 시 기본값 0 적용
    private Integer failedLoginCount = 0;

    // ✅ [추가 4] 동의 IP/UserAgent (DB에 있어서 추가함, 필수는 아닐 수 있음)
    @Column(name = "consent_ip", length = 45)
    private String consentIp;

    @Column(name = "consent_user_agent", columnDefinition = "TEXT")
    private String consentUserAgent;

    @Column(nullable = false)
    private String role;

    @Column(length = 80, nullable = false)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(nullable = false)
    private String status;

    @Column(length = 20)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "marketing_opt_in")
    private Boolean marketingOptIn;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "member_grade", length = 50)
    private String memberGrade;

    @Column(name = "grade_updated_at")
    private LocalDateTime gradeUpdatedAt;

    public void updateGrade(String newGrade) {
        this.memberGrade = newGrade;
        this.gradeUpdatedAt = LocalDateTime.now();
    }
}