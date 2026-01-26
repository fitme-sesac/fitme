package com.example.pproject.job.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 채용공고 테이블 (ERD: job_posting 기준)
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

    // employer 테이블과의 FK
    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    // 채용공고 기본 정보
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    // 상태: DRAFT, OPEN, CLOSED
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    // ERD 컬럼명
    @Column(name = "location", length = 120)
    private String location;

    @Column(name = "salary_text")
    private Long salaryText;

    // JSONB 타입 - Hibernate 6 방식
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_questions", columnDefinition = "jsonb")
    private String requiredQuestions;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    // ERD: apply_count
    @Column(name = "apply_count", nullable = false)
    private Integer applicationCount;

    /**
     * 기술 스택 (TEXT[] 배열)
     * - ERD: stack TEXT[] NULL
     * - Hibernate 6: @JdbcTypeCode(SqlTypes.ARRAY)
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "stack", columnDefinition = "TEXT[]")
    private List<String> stack;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    // AI 벡터 임베딩 (이력서와의 유사도 계산용)
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    private java.util.List<Double> embedding;

    // 광고 입찰가
    @Column(name = "ad_bid_credit", nullable = false)
    private Integer adBidCredit;

    // 감사
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // ====== 앱에서 사용할 가상 필드 (DB에 없음) ======
    @Transient
    private UUID jobUid;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "DRAFT";
        }
        if (viewCount == null) viewCount = 0;
        if (applicationCount == null) applicationCount = 0;
        if (adBidCredit == null) adBidCredit = 0;

        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * jobUid 대신 id를 UUID 형태 문자열로 반환 (기존 API 호환용)
     */
    public UUID getJobUid() {
        return null;
    }
}
