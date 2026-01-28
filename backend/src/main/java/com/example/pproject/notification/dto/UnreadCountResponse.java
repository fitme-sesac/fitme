package com.example.pproject.notification.dto;

import lombok.*;

/**
 * 읽지 않은 알림 수 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnreadCountResponse {

    /**
     * 읽지 않은 알림 수
     */
    private long unreadCount;

    public static UnreadCountResponse of(long count) {
        return new UnreadCountResponse(count);
    }
}
