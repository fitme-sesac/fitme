package com.example.pproject.ad.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 광고 노출 이벤트 테이블 (ERD: ad_impression_event)
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "ad_impression_event")
@Entity
public class AdImpressionEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "impression_id")
    private Long id;

    // ad_campaign 테이블과의 FK
    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    // member 테이블과의 FK (Nullable)
    @Column(name = "member_id")
    private Long memberId;

    // 발생 시간
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void prePersist() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
