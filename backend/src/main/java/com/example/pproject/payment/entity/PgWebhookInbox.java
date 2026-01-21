package com.example.pproject.payment.entity;

import com.example.pproject.Constant.WebhookProcessStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * PG사(토스) 웹훅 수신함 엔티티.
 * <p>
 * 토스 페이먼츠에서 보내는 웹훅(결제 상태 변경, 입금 통보 등) 데이터를 원본 그대로 저장합니다.
 * 추후 재처리나 디버깅을 위해 사용되며, 처리 상태(RECEIVED, PROCESSED, FAILED)를 관리합니다.
 * </p>
 */
@Entity
@Table(
        name = "pg_webhook_inbox",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pg_event_id", columnNames = "pg_event_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PgWebhookInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inbox_id")
    private Long inboxId;   // 인박스id

    @Column(name = "pg_event_id", nullable = false, length = 100)
    private String pgEventId;   // PG 이벤트

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType; // PaymentStatusChanged 등

    // Payment와 관계는 맺되, 웹훅 수신 시점엔 PaymentId를 모를 수도 있으므로 Nullable 허용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = true)
    private Payment payment;    // 결제id

    // 웹훅 전체 Payload
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;    // 원문

    @Enumerated(EnumType.STRING)
    @Column(name = "process_status", nullable = false, length = 20)
    private WebhookProcessStatus processStatus; // 처리 상태

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount; // 재시도 횟수

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;   // 마지막 오류

    @CreatedDate
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;   // 수신 시간

    @Column(name = "processed_at")
    private LocalDateTime processedAt;  // 처리 시간

    @Builder
    public PgWebhookInbox(String pgEventId, String eventType, Payment payment, Map<String, Object> payload) {
        this.pgEventId = pgEventId;
        this.eventType = eventType;
        this.payment = payment;
        this.payload = payload;
        this.processStatus = WebhookProcessStatus.RECEIVED; // 초기 상태: RECEIVED
        this.retryCount = 0;
    }

    // === 팩토리 메서드 ===

    /**
     * 웹훅 수신함 엔티티 생성
     */
    public static PgWebhookInbox create(String pgEventId, String eventType, Payment payment, Map<String, Object> payload) {
        return PgWebhookInbox.builder()
                .pgEventId(pgEventId)
                .eventType(eventType)
                .payment(payment)
                .payload(payload)
                .build();
    }

    // === 비즈니스 로직 ===

    /**
     * 웹훅 처리가 성공적으로 완료되었음을 표시합니다.
     */
    public void markAsProcessed() {
        this.processStatus = WebhookProcessStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 웹훅 처리가 실패했음을 표시하고, 에러 메시지를 기록합니다.
     * 재시도 횟수를 증가시킵니다.
     *
     * @param errorMessage 발생한 에러 메시지
     */
    public void markAsFailed(String errorMessage) {
        this.processStatus = WebhookProcessStatus.FAILED;
        this.lastError = errorMessage;
        this.retryCount++;
    }
}
