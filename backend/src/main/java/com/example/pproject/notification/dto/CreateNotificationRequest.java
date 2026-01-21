package com.example.pproject.notification.dto;

import com.example.pproject.Constant.NotificationChannel;
import com.example.pproject.Constant.NotificationEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 알림 생성 요청 DTO (내부 서비스용)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {

    private Integer memberId;
    private NotificationEventType eventType;
    private NotificationChannel channel;
    private String title;
    private String message;
    private String linkUrl;
    private Map<String, Object> payload;

    /**
     * 인앱 푸시 알림 생성용 빌더 헬퍼
     */
    public static CreateNotificationRequest pushNotification(
            Integer memberId,
            NotificationEventType eventType,
            String title,
            String message,
            String linkUrl,
            Map<String, Object> payload
    ) {
        return CreateNotificationRequest.builder()
                .memberId(memberId)
                .eventType(eventType)
                .channel(NotificationChannel.PUSH)
                .title(title)
                .message(message)
                .linkUrl(linkUrl)
                .payload(payload)
                .build();
    }
}
