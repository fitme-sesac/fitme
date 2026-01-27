package com.example.pproject.job.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 공고 열람 로그 Entity
 * ERD: job_view_log 테이블
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "job_view_log")
public class JobViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_log_id")
    private Long id;

    @Column(name = "member_id")
    private Long memberId;  // 비회원도 조회 가능하므로 nullable

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @Builder
    public JobViewLog(Long memberId, JobEntity job) {
        this.memberId = memberId;
        this.job = job;
        this.viewedAt = LocalDateTime.now();
    }
}
