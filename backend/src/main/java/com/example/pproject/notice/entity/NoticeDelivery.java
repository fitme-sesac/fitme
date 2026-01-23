package com.example.pproject.notice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice_delivery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NoticeDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    // Member 패키지 의존성을 끊기 위해 ID로 참조
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DeliveryChannel channel; // EMAIL, SMS, LMS, PUSH

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status; // PENDING, SENT, FAILED

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "fail_reason", length = 200)
    private String failReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Enums
    public enum DeliveryChannel { EMAIL, SMS, LMS, PUSH }
    public enum DeliveryStatus { PENDING, SENT, FAILED }
}