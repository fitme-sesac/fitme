package com.example.pproject.notification.dto;

import com.example.pproject.notification.entity.NotificationTemplate.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * 알림 생성 요청 DTO
 * 
 * ========================================
 * ERD 테이블: notification_delivery (38번)
 * ========================================
 * 
 * 필드 매핑:
 * - memberId    -> member_id (FK, NOT NULL)
 * - eventType   -> event_type (VARCHAR(40), NOT NULL)
 * - channel     -> channel (VARCHAR(20), NOT NULL)
 * - templateCode -> 템플릿 조회용 (template_id FK)
 * - payload     -> payload (JSONB)
 * - linkUrl     -> link_url (추가 필드)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCreateRequest {

    /**
     * 수신자 회원 ID (member_id FK)
     * - member(member_id) 참조
     */
    @NotNull(message = "회원 ID는 필수입니다")
    private Long memberId;

    /**
     * 이벤트 타입 (event_type)
     * - 예: APPLICATION_SUBMITTED, INTERVIEW_SCHEDULED 등
     */
    @NotBlank(message = "이벤트 타입은 필수입니다")
    private String eventType;

    /**
     * 발송 채널 (channel)
     * - EMAIL, SMS, PUSH
     */
    @NotNull(message = "발송 채널은 필수입니다")
    private NotificationChannel channel;

    /**
     * 템플릿 코드 (optional)
     * - notification_template.template_code 조회용
     */
    private String templateCode;

    /**
     * 페이로드 (payload, JSONB)
     * - 템플릿 플레이스홀더 치환용 데이터
     * - 예: {"jobTitle": "백엔드 개발자", "companyName": "ABC Company"}
     */
    private String payload;

    /**
     * 링크 URL (link_url)
     * - 클릭 시 이동할 페이지
     * - 예: /jobs/123, /applications
     */
    private String linkUrl;
}
