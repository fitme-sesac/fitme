package com.example.pproject.notification.dto;

import com.example.pproject.Constant.NotificationDeliveryStatus;
import com.example.pproject.Constant.NotificationEventType;
import com.example.pproject.notification.entity.NotificationDelivery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 알림 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private NotificationEventType eventType;
    private String title;
    private String message;
    private String linkUrl;
    private NotificationDeliveryStatus status;
    private boolean isRead;
    private Map<String, Object> payload;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public static NotificationResponse from(NotificationDelivery entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .eventType(entity.getEventType())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .linkUrl(entity.getLinkUrl())
                .status(entity.getStatus())
                .isRead(entity.isRead())
                .payload(entity.getPayload())
                .createdAt(entity.getCreatedAt())
                .readAt(entity.getReadAt())
                .build();
    }
}
