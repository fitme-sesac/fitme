package com.example.pproject.notification.dto;

import com.example.pproject.notification.entity.NotificationDelivery;
import com.example.pproject.notification.entity.NotificationTemplate.NotificationChannel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
@Slf4j
public class NotificationDTO {

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
            case "INTERVIEW_ACCEPTED" -> "면접 일정 확정";
            case "INTERVIEW_DECLINED" -> "면접 일정 거절";
            case "INTERVIEW_RESCHEDULE_REQUEST" -> "면접 일정 변경 요청";
            case "INTERVIEW_CANCELLED" -> "면접 취소";
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
     * - 템플릿이 있으면 플레이스홀더를 payload 값으로 치환
     * - 템플릿이 없으면 이벤트 타입별 기본 메시지 생성
     */
    private static String getMessage(NotificationDelivery entity) {
        // payload JSON 파싱
        Map<String, Object> payloadMap = parsePayload(entity.getPayload());
        
        if (entity.getTemplate() != null && entity.getTemplate().getBody() != null) {
            // 템플릿의 플레이스홀더 치환 ({{key}} 형식)
            return replacePlaceholders(entity.getTemplate().getBody(), payloadMap);
        }
        
        // 템플릿이 없으면 이벤트 타입별 기본 메시지 생성
        return generateDefaultMessage(entity.getEventType(), payloadMap);
    }
    
    /**
     * JSON payload 파싱
     */
    private static Map<String, Object> parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        
        try {
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Payload JSON 파싱 실패: {}", e.getMessage());
            return Map.of();
        }
    }
    
    /**
     * 플레이스홀더 치환 ({{key}} 형식)
     */
    private static String replacePlaceholders(String template, Map<String, Object> values) {
        if (template == null || values.isEmpty()) {
            return template;
        }
        
        String result = template;
        Pattern pattern = Pattern.compile("\\{\\{(\\w+)\\}\\}");
        Matcher matcher = pattern.matcher(template);
        
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = values.get(key);
            if (value != null) {
                result = result.replace("{{" + key + "}}", String.valueOf(value));
            }
        }
        
        return result;
    }
    
    /**
     * 이벤트 타입별 기본 메시지 생성
     */
    private static String generateDefaultMessage(String eventType, Map<String, Object> payload) {
        String jobTitle = getStringValue(payload, "jobTitle", "채용공고");
        String companyName = getStringValue(payload, "companyName", "기업");
        String candidateName = getStringValue(payload, "candidateName", "지원자");
        String status = getStringValue(payload, "status", "");
        // startAt 또는 interviewDate 필드 사용
        String interviewDate = getStringValue(payload, "startAt", 
                getStringValue(payload, "interviewDate", ""));
        String amount = getStringValue(payload, "amount", "");
        
        return switch (eventType) {
            case "APPLICATION_SUBMITTED" -> 
                String.format("'%s'에 지원이 완료되었습니다.", jobTitle);
            case "APPLICATION_VIEWED" -> 
                String.format("'%s'에서 이력서를 열람했습니다.", companyName);
            case "APPLICATION_STATUS_CHANGED" -> 
                String.format("'%s' 지원 상태가 '%s'(으)로 변경되었습니다.", jobTitle, translateStatus(status));
            case "INTERVIEW_SCHEDULED" ->
                String.format("'%s'에서 '%s' 면접을 요청했습니다. 일시: %s", companyName, jobTitle, formatDate(interviewDate));
            case "INTERVIEW_REMINDER" ->
                String.format("'%s' 면접이 곧 예정되어 있습니다. 일시: %s", jobTitle, formatDate(interviewDate));
            case "INTERVIEW_ACCEPTED" ->
                String.format("'%s'님이 '%s' 면접 일정을 수락했습니다.", candidateName, jobTitle);
            case "INTERVIEW_DECLINED" ->
                String.format("'%s'님이 '%s' 면접 일정을 거절했습니다.", candidateName, jobTitle);
            case "INTERVIEW_RESCHEDULE_REQUEST" ->
                String.format("'%s'님이 '%s' 면접 일정 변경을 요청했습니다.", candidateName, jobTitle);
            case "INTERVIEW_CANCELLED", "INTERVIEW_CANCELED" ->
                String.format("'%s' 면접이 취소되었습니다.", jobTitle);
            case "HIRED" -> 
                String.format("축하합니다! '%s'에 합격하셨습니다.", jobTitle);
            case "REJECTED" -> 
                String.format("'%s' 지원 결과를 확인해주세요.", jobTitle);
            case "PAYMENT_COMPLETED" -> 
                String.format("결제가 완료되었습니다. 금액: %s원", formatAmount(amount));
            case "PAYMENT_FAILED" -> 
                "결제 처리 중 문제가 발생했습니다. 다시 시도해주세요.";
            case "NEW_APPLICATION_RECEIVED" -> 
                String.format("'%s' 공고에 새로운 지원자(%s)가 있습니다.", jobTitle, candidateName);
            case "JOB_POSTING_CREATED" -> 
                String.format("'%s' 채용공고가 등록되었습니다.", jobTitle);
            case "JOB_POSTING_UPDATED" -> 
                String.format("'%s' 채용공고가 수정되었습니다.", jobTitle);
            case "JOB_POSTING_STATUS_CHANGED" -> 
                String.format("'%s' 채용공고 상태가 '%s'(으)로 변경되었습니다.", jobTitle, translateJobStatus(status));
            case "JOB_POSTING_DELETED" -> 
                String.format("'%s' 채용공고가 삭제되었습니다.", jobTitle);
            case "PROPOSAL_RECEIVED" -> 
                String.format("'%s'에서 입사 제안을 보냈습니다.", companyName);
            case "PROPOSAL_ACCEPTED" -> 
                String.format("'%s'님이 입사 제안을 수락했습니다.", candidateName);
            case "PROPOSAL_REJECTED" -> 
                String.format("'%s'님이 입사 제안을 거절했습니다.", candidateName);
            case "CREDIT_CHARGED" -> 
                String.format("크레딧 %s이 충전되었습니다.", formatAmount(amount));
            case "CREDIT_USED" -> 
                String.format("크레딧 %s이 사용되었습니다.", formatAmount(amount));
            case "SYSTEM_NOTICE" -> 
                getStringValue(payload, "message", "시스템 공지사항을 확인해주세요.");
            default -> 
                "새로운 알림이 있습니다.";
        };
    }
    
    /**
     * payload에서 String 값 추출
     */
    private static String getStringValue(Map<String, Object> payload, String key, String defaultValue) {
        Object value = payload.get(key);
        return value != null ? String.valueOf(value) : defaultValue;
    }
    
    /**
     * 지원 상태 한글 변환
     */
    private static String translateStatus(String status) {
        return switch (status.toUpperCase()) {
            case "SUBMITTED" -> "지원 완료";
            case "VIEWED" -> "이력서 열람";
            case "INTERVIEW" -> "면접 진행";
            case "HIRED" -> "합격";
            case "REJECTED" -> "불합격";
            case "CANCELED" -> "취소";
            default -> status;
        };
    }
    
    /**
     * 채용공고 상태 한글 변환
     */
    private static String translateJobStatus(String status) {
        return switch (status.toUpperCase()) {
            case "DRAFT" -> "임시저장";
            case "OPEN" -> "모집중";
            case "CLOSED" -> "마감";
            default -> status;
        };
    }
    
    /**
     * 날짜 포맷팅
     */
    private static String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return "미정";
        }
        // ISO 형식이면 한국어 형식으로 변환 시도
        try {
            if (dateStr.contains("T")) {
                Instant instant = Instant.parse(dateStr);
                java.time.ZonedDateTime zdt = instant.atZone(java.time.ZoneId.of("Asia/Seoul"));
                return String.format("%d년 %d월 %d일 %02d:%02d",
                        zdt.getYear(), zdt.getMonthValue(), zdt.getDayOfMonth(),
                        zdt.getHour(), zdt.getMinute());
            }
        } catch (Exception e) {
            // 변환 실패 시 원본 반환
        }
        return dateStr;
    }
    
    /**
     * 금액 포맷팅 (천단위 콤마)
     */
    private static String formatAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return "0";
        }
        try {
            long value = Long.parseLong(amount.replaceAll("[^0-9]", ""));
            return String.format("%,d", value);
        } catch (NumberFormatException e) {
            return amount;
        }
    }
}
