package com.example.pproject.outbox.entity;

import com.example.pproject.Constant.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 아웃박스 이벤트 엔티티 - outbox_event 테이블 매핑
 * Transactional Outbox Pattern 구현을 위한 이벤트 저장소
 */
@Entity
@Table(name = "outbox_event", indexes = {
    @Index(name = "idx_outbox_pick", columnList = "status, next_run_at, outbox_id"),
    @Index(name = "idx_outbox_agg", columnList = "aggregate_type, aggregate_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long id;

    /**
     * 이벤트 타입 (예: APPLICATION_SUBMITTED, INTERVIEW_SCHEDULED 등)
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /**
     * 집계 타입 (예: JOB_APPLICATION, INTERVIEW, PAYMENT 등)
     */
    @Column(name = "aggregate_type", nullable = false, length = 30)
    private String aggregateType;

    /**
     * 집계 ID (예: application_id, interview_id, payment_id 등)
     */
    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    /**
     * 이벤트 페이로드 (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    /**
     * 처리 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    /**
     * 재시도 횟수
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * 다음 실행 예정 시간
     */
    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    /**
     * 락 획득 시간 (동시 처리 방지)
     */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    /**
     * 락을 획득한 워커 ID
     */
    @Column(name = "locked_by", length = 80)
    private String lockedBy;

    /**
     * 마지막 에러 메시지
     */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 처리 완료 시간
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (nextRunAt == null) nextRunAt = now;
        if (status == null) status = OutboxStatus.PENDING;
        if (retryCount == null) retryCount = 0;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 처리 시작 - 락 획득
     */
    public void startProcessing(String workerId) {
        this.status = OutboxStatus.PROCESSING;
        this.lockedAt = LocalDateTime.now();
        this.lockedBy = workerId;
    }

    /**
     * 처리 성공
     */
    public void markAsSucceeded() {
        this.status = OutboxStatus.SUCCEEDED;
        this.processedAt = LocalDateTime.now();
        this.lockedAt = null;
        this.lockedBy = null;
    }

    /**
     * 처리 실패 - 재시도 스케줄링
     */
    public void markAsFailed(String errorMessage, int maxRetries) {
        this.retryCount++;
        this.lastError = errorMessage;
        this.lockedAt = null;
        this.lockedBy = null;

        if (this.retryCount >= maxRetries) {
            this.status = OutboxStatus.FAILED;
        } else {
            this.status = OutboxStatus.PENDING;
            // 지수 백오프: 2^retryCount 분 후 재시도
            this.nextRunAt = LocalDateTime.now().plusMinutes((long) Math.pow(2, retryCount));
        }
    }

    /**
     * 락 해제 (타임아웃 등)
     */
    public void releaseLock() {
        this.lockedAt = null;
        this.lockedBy = null;
        this.status = OutboxStatus.PENDING;
    }
}
