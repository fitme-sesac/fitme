package com.example.pproject.notification.dto;

import com.example.pproject.notification.entity.NotificationDelivery;
import com.example.pproject.notification.entity.NotificationTemplate.NotificationChannel;
import lombok.*;

import java.time.Instant;

/**
 * 알림 응답 DTO
 * 
 * ========================================
 * ERD 테이블: notification_delivery (38번)
 * ========================================
 * 
 * 필드 매핑:
 * - id          <- delivery_id (PK)
 * - eventType   <- event_type (VARCHAR(40))
 * - channel     <- channel (VARCHAR(20))
 * - status      <- status (VARCHAR(20))
 * - isRead      <- is_read (추가 필드)
 * - linkUrl     <- link_url (추가 필드)
 * - payload     <- payload (JSONB)
 * - sentAt      <- sent_at (TIMESTAMPTZ)
 * - createdAt   <- created_at (TIMESTAMPTZ)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {

    /**
     * 알림 ID (delivery_id)
     */
    private Long id;

    /**
     * 이벤트 타입 (event_type)
     * - 예: APPLICATION_SUBMITTED, INTERVIEW_SCHEDULED 등
     */
    private String eventType;

    /**
     * 알림 제목 (템플릿 또는 동적 생성)
     */
    private String title;

    /**
     * 알림 내용 (body, 플레이스홀더 치환 후)
     */
    private String message;

    /**
     * 발송 채널 (channel)
     * - EMAIL, SMS, PUSH
     */
    private NotificationChannel channel;

    /**
     * 발송 상태 (status)
     * - PENDING, SENT, FAILED
     */
    private String status;

    /**
     * 읽음 여부 (is_read)
     */
    private Boolean isRead;

    /**
     * 링크 URL (link_url)
     * - 클릭 시 이동할 페이지
     */
    private String linkUrl;

    /**
     * 발송 일시 (sent_at)
     */
    private Instant sentAt;

    /**
     * 생성 일시 (created_at)
     */
    private Instant createdAt;

    /**
     * Entity -> DTO 변환
     */
    public static NotificationDTO from(NotificationDelivery entity) {
        return NotificationDTO.builder()
                .id(entity.getId())
                .eventType(entity.getEventType())
                .title(getTitle(entity))
                .message(getMessage(entity))
                .channel(entity.getChannel())
                .status(entity.getStatus().name())
                .isRead(entity.getIsRead())
                .linkUrl(entity.getLinkUrl())
                .sentAt(entity.getSentAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * 알림 제목 생성 (템플릿 또는 이벤트 타입 기반)
     */
    private static String getTitle(NotificationDelivery entity) {
        if (entity.getTemplate() != null && entity.getTemplate().getSubject() != null) {
            return entity.getTemplate().getSubject();
        }
        // 이벤트 타입에 따른 기본 제목
        return switch (entity.getEventType()) {
            case "APPLICATION_SUBMITTED" -> "지원 완료";
            case "APPLICATION_VIEWED" -> "이력서 열람";
            case "APPLICATION_STATUS_CHANGED" -> "지원 상태 변경";
            case "INTERVIEW_SCHEDULED" -> "면접 일정 안내";
            case "INTERVIEW_REMINDER" -> "면접 일정 리마인더";
            case "INTERVIEW_CANCELED" -> "면접 취소";
            case "HIRED" -> "합격 안내";
            case "REJECTED" -> "불합격 안내";
            case "PAYMENT_COMPLETED" -> "결제 완료";
            case "PAYMENT_FAILED" -> "결제 실패";
            case "NEW_APPLICATION_RECEIVED" -> "새 지원자";
            case "SYSTEM_NOTICE" -> "시스템 공지";
            default -> "알림";
        };
    }

    /**
     * 알림 메시지 생성 (템플릿 또는 페이로드 기반)
     */
    private static String getMessage(NotificationDelivery entity) {
        if (entity.getTemplate() != null) {
            // TODO: 플레이스홀더 치환 로직 (payload 파싱 후 치환)
            return entity.getTemplate().getBody();
        }
        return entity.getPayload();
    }
}
