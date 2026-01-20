package com.example.pproject.employer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 기업회원 상세 정보 테이블
 * - member 테이블과 1:1 관계 (member_id FK)
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "employer")
@Entity
public class EmployerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employer_id")
    private Long id;

    @Column(name = "employer_uid", nullable = false, unique = true)
    private UUID employerUid;

    // member 테이블과의 FK
    @Column(name = "member_id", nullable = false)
    private Integer memberId;

    // 회사 정보
    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "business_registration_number", length = 20)
    private String businessRegistrationNumber;

    @Column(name = "representative_name", length = 80)
    private String representativeName;

    @Column(name = "company_address", length = 500)
    private String companyAddress;

    @Column(name = "company_phone", length = 20)
    private String companyPhone;

    @Column(name = "company_website", length = 500)
    private String companyWebsite;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Column(name = "company_description", columnDefinition = "TEXT")
    private String companyDescription;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    // 상태
    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // 감사
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (employerUid == null) {
            employerUid = UUID.randomUUID();
        }
        if (status == null || status.isBlank()) {
            status = "PENDING";
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
