package com.example.pproject.employer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 기업 프로필 테이블 (ERD: employer)
 * - employer_member 테이블을 통해 member와 연결
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

    // 기업 기본 정보 (ERD 기준)
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "industry", length = 80)
    private String industry;

    @Column(name = "founded_year")
    private Integer foundedYear;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Column(name = "location", length = 120)
    private String location;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "culture", columnDefinition = "TEXT")
    private String culture;

    @Column(name = "benefits", columnDefinition = "TEXT")
    private String benefits;

    @Column(name = "tech_stack", columnDefinition = "TEXT")
    private String techStack;

    @Column(name = "contact_email", length = 320)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "website_url")
    private String websiteUrl;

    // 상태
    @Column(name = "status", nullable = false, length = 20)
    private String status; // ACTIVE, SUSPENDED, CLOSED

    // 감사
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    void prePersist() {
        if (employerUid == null) {
            employerUid = UUID.randomUUID();
        }
        if (status == null || status.isBlank()) {
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
