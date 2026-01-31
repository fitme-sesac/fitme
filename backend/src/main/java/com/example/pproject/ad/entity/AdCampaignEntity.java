package com.example.pproject.ad.entity;

import com.example.pproject.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 광고 캠페인 테이블 (ERD: ad_campaign)
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "ad_campaign")
@Entity
public class AdCampaignEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campaign_id")
    private Long id;

    // employer 테이블과의 FK
    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    // job_posting 테이블과의 FK
    @Column(name = "job_id", nullable = false)
    private Long jobId;

    // CPC 입찰가
    @Column(name = "cpc_bid", nullable = false)
    private Integer cpcBid;

    // 일일 예산
    @Column(name = "daily_budget", nullable = false)
    private Integer dailyBudget;

    // 상태: ACTIVE, PAUSED, ENDED
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }
}
