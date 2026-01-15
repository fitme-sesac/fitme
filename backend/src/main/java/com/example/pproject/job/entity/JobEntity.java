package com.example.pproject.job.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채용공고 테이블
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "job_posting")
@Entity
public class JobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long id;

    @Column(name = "job_uid", nullable = false, unique = true)
    private UUID jobUid;

    // employer 테이블과의 FK
    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    // 채용공고 기본 정보
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "preferred_qualifications", columnDefinition = "TEXT")
    private String preferredQualifications;

    // 근무 조건
    @Column(name = "job_type", length = 50)
    private String jobType; // FULL_TIME, PART_TIME, CONTRACT, INTERN

    @Column(name = "experience_level", length = 50)
    private String experienceLevel; // ENTRY, JUNIOR, SENIOR, MANAGER

    @Column(name = "education_level", length = 50)
    private String educationLevel;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Column(name = "salary_negotiable")
    private Boolean salaryNegotiable;

    // 근무지
    @Column(name = "work_location", length = 500)
    private String workLocation;

    @Column(name = "remote_work_available")
    private Boolean remoteWorkAvailable;

    // 모집 기간
    @Column(name = "posting_start_date")
    private LocalDate postingStartDate;

    @Column(name = "posting_end_date")
    private LocalDate postingEndDate;

    @Column(name = "is_always_recruiting")
    private Boolean isAlwaysRecruiting;

    // 상태
    @Column(name = "status", nullable = false, length = 20)
    private String status; // DRAFT, ACTIVE, CLOSED, EXPIRED

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "application_count", nullable = false)
    private Integer applicationCount;

    // 감사
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    void prePersist() {
        if (jobUid == null) {
            jobUid = UUID.randomUUID();
        }
        if (status == null || status.isBlank()) {
            status = "DRAFT";
        }
        if (viewCount == null) viewCount = 0;
        if (applicationCount == null) applicationCount = 0;
        if (salaryNegotiable == null) salaryNegotiable = false;
        if (remoteWorkAvailable == null) remoteWorkAvailable = false;
        if (isAlwaysRecruiting == null) isAlwaysRecruiting = false;

        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
