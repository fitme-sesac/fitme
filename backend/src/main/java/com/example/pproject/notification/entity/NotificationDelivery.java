package com.example.pproject.notification.entity;

import com.example.pproject.Constant.NotificationChannel;
import com.example.pproject.Constant.NotificationDeliveryStatus;
import com.example.pproject.Constant.NotificationEventType;
import com.example.pproject.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 알림 발송 이력 엔티티 - notification_delivery 테이블 매핑
 * ERD 기준 컬럼: delivery_id, template_id, member_id, event_type, channel, status, sent_at, payload, created_at
 */
@Entity
@Table(name = "notification_delivery")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private NotificationTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private UserEntity member;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private NotificationEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private NotificationDeliveryStatus status = NotificationDeliveryStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = NotificationDeliveryStatus.PENDING;
        }
        if (payload == null) {
            payload = new HashMap<>();
        }
    }

    /**
     * 발송 완료 처리
     */
    public void markAsSent() {
        this.status = NotificationDeliveryStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * 읽음 처리 (payload에 readAt 저장)
     * - JPA 변경 감지를 위해 새 Map으로 교체
     */
    public void markAsRead() {
        if (!isRead()) {
            Map<String, Object> newPayload = new HashMap<>();
            if (this.payload != null) {
                newPayload.putAll(this.payload);
            }
            newPayload.put("readAt", LocalDateTime.now().toString());
            this.payload = newPayload; // 새 Map으로 교체하여 JPA 변경 감지 유도
        }
    }

    /**
     * 읽음 여부 확인
     */
    public boolean isRead() {
        return this.payload != null && this.payload.containsKey("readAt");
    }

    /**
     * 읽은 시간 조회
     */
    public LocalDateTime getReadAt() {
        if (this.payload != null && this.payload.containsKey("readAt")) {
            Object readAt = this.payload.get("readAt");
            if (readAt instanceof String) {
                return LocalDateTime.parse((String) readAt);
            }
        }
        return null;
    }

    /**
     * 발송 실패 처리
     */
    public void markAsFailed() {
        this.status = NotificationDeliveryStatus.FAILED;
    }

    // ===== payload에서 추가 정보 조회 헬퍼 메서드 =====

    public String getTitle() {
        return payload != null ? (String) payload.get("title") : null;
    }

    public void setTitle(String title) {
        if (this.payload == null) this.payload = new HashMap<>();
        this.payload.put("title", title);
    }

    public String getMessage() {
        return payload != null ? (String) payload.get("message") : null;
    }

    public void setMessage(String message) {
        if (this.payload == null) this.payload = new HashMap<>();
        this.payload.put("message", message);
    }

    public String getLinkUrl() {
        return payload != null ? (String) payload.get("linkUrl") : null;
    }

    public void setLinkUrl(String linkUrl) {
        if (this.payload == null) this.payload = new HashMap<>();
        this.payload.put("linkUrl", linkUrl);
    }
}
