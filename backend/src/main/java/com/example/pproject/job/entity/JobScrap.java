package com.example.pproject.job.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 공고 스크랩 Entity
 * ERD: job_scrap 테이블
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "job_scrap", uniqueConstraints = {
        @UniqueConstraint(name = "uq_job_scrap", columnNames = {"member_id", "job_id"})
})
public class JobScrap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scrap_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public JobScrap(Long memberId, JobEntity job) {
        this.memberId = memberId;
        this.job = job;
        this.createdAt = LocalDateTime.now();
    }
}
