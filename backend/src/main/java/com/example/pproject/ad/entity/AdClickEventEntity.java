package com.example.pproject.ad.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 광고 클릭 이벤트 테이블 (ERD: ad_click_event)
 * - BaseTimeEntity 상속 안 함 (updated_at 없음)
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "ad_click_event")
@Entity
public class AdClickEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "click_id")
    private Long id;

    // ad_campaign 테이블과의 FK
    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    // member 테이블과의 FK (Nullable - 비로그인 클릭 가능성 고려?) -> DDL상 Nullable
    @Column(name = "member_id")
    private Long memberId;

    // 중복 클릭 방지 키 (Unique)
    @Column(name = "click_key", nullable = false, length = 120, unique = true)
    private String clickKey;

    // wallet_ledger 테이블과의 FK (과금 기록 연결)
    @Column(name = "ledger_id")
    private Long ledgerId;

    // 발생 시간
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    // 클릭 당시의 과금액 (CPC) - 정산용 Source of Truth
    @Column(name = "click_cost", nullable = false)
    private Integer cost;

    @PrePersist
    void prePersist() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
