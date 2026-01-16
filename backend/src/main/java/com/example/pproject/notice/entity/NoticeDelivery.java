package com.example.pproject.notice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * 공지사항 배송 기록 엔티티
 * ADM-NTC-004, ADM-NTC-005 관련
 */
@Entity
@Table(name = "notice_deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "channel", nullable = false)
    @Enumerated(EnumType.STRING)
    private DeliveryChannel channel;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failed_reason")
    private String failedReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * 배송 채널 열거형
     */
    public enum DeliveryChannel {
        EMAIL,  // 이메일
        SMS     // SMS
    }

    /**
     * 배송 상태 열거형
     */
    public enum DeliveryStatus {
        PENDING,   // 대기중
        SUCCESS,   // 성공
        FAILED     // 실패
    }
}