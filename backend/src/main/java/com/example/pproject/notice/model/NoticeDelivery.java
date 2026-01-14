package com.example.pproject.notice.model;

import com.example.pproject.Constant.DeliveryChannel;
import com.example.pproject.Constant.DeliveryStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 공지사항 발송 엔티티
 * - 개별 회원에 대한 공지사항 발송 추적
 * - EMAIL, SMS, LMS, PUSH 등 다양한 채널 지원
 */
@Entity
@Table(name = "notice_delivery")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Long deliveryId;

    @Column(name = "notice_id", nullable = false)
    private Long noticeId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private DeliveryChannel channel;  // EMAIL, SMS, LMS, PUSH

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;  // PENDING(대기), SENT(발송완료), FAILED(발송실패)

    @Column(name = "sent_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;

    @Column(name = "fail_reason", length = 200)
    private String failReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}