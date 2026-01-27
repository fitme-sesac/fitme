package com.example.pproject.outbox.consumer;

import com.example.pproject.notification.dto.NotificationCreateRequest;
import com.example.pproject.notification.entity.NotificationTemplate.NotificationChannel;
import com.example.pproject.notification.service.NotificationService;
import com.example.pproject.outbox.entity.OutboxEvent;
import com.example.pproject.outbox.service.OutboxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

/**
 * 아웃박스 이벤트 컨슈머 (Worker)
 * 
 * ========================================
 * ERD 테이블: outbox_event (39번)
 * ========================================
 * 
 * 역할:
 * - Outbox 테이블을 폴링하여 PENDING 상태의 이벤트 처리
 * - 이벤트 타입에 따라 적절한 핸들러 호출
 * - 처리 결과에 따라 상태 업데이트 (SUCCEEDED/FAILED)
 * 
 * 인덱스 활용:
 * - idx_outbox_pick: (status, next_run_at, outbox_id)
 * 
 * 이벤트 타입별 처리:
 * - JOB_POSTING_CREATED: 공고 등록 알림 (기업용)
 * - APPLICATION_SUBMITTED: 지원 완료 알림 (지원자) + 새 지원자 알림 (기업)
 * - APPLICATION_STATUS_CHANGED: 지원 상태 변경 알림 (지원자)
 * - INTERVIEW_SCHEDULED: 면접 일정 알림 (지원자)
 * - INTERVIEW_CANCELED: 면접 취소 알림 (지원자)
 * - PAYMENT_COMPLETED: 결제 완료 알림
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventConsumer {

    private final OutboxService outboxService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    // Worker 식별자 (호스트명 + 스레드)
    private final String workerId = getWorkerId();

    // 설정 값
    private static final int BATCH_SIZE = 50;
    private static final int POLL_INTERVAL_MS = 5000; // 5초
    private static final int LOCK_CLEANUP_INTERVAL_MS = 60000; // 1분
    private static final int EVENT_CLEANUP_RETENTION_DAYS = 30; // 30일

    // ==================== 스케줄러 ====================

    /**
     * 이벤트 처리 스케줄러 (5초마다 실행)
     * - PENDING 상태의 이벤트를 조회하여 처리
     */
    @Scheduled(fixedDelay = POLL_INTERVAL_MS)
    public void processEvents() {
        List<OutboxEvent> events = outboxService.getPendingEvents(BATCH_SIZE);
        
        if (events.isEmpty()) {
            return;
        }

        log.debug("[OutboxConsumer] 처리할 이벤트 수: {}", events.size());

        for (OutboxEvent event : events) {
            processEvent(event);
        }
    }

    /**
     * 만료된 락 해제 스케줄러 (1분마다 실행)
     * - Worker 장애 복구용
     */
    @Scheduled(fixedDelay = LOCK_CLEANUP_INTERVAL_MS)
    public void releaseExpiredLocks() {
        outboxService.releaseExpiredLocks();
    }

    /**
     * 오래된 이벤트 정리 스케줄러 (매일 새벽 3시)
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldEvents() {
        int deleted = outboxService.cleanupOldEvents(EVENT_CLEANUP_RETENTION_DAYS);
        log.info("[OutboxConsumer] 이벤트 정리 완료 - 삭제: {}건", deleted);
    }

    // ==================== 이벤트 처리 ====================

    /**
     * 개별 이벤트 처리
     */
    private void processEvent(OutboxEvent event) {
        Long eventId = event.getId();
        String eventType = event.getEventType();

        // 1. 락 획득 시도
        if (!outboxService.acquireLock(eventId, workerId)) {
            return; // 다른 Worker가 처리 중
        }

        log.info("[OutboxConsumer] 이벤트 처리 시작 - outboxId: {}, type: {}", eventId, eventType);

        try {
            // 2. 이벤트 타입별 처리
            Map<String, Object> payload = parsePayload(event.getPayload());
            handleEvent(eventType, event.getAggregateType(), event.getAggregateId(), payload);

            // 3. 성공 처리
            outboxService.markAsSucceeded(eventId);
            log.info("[OutboxConsumer] 이벤트 처리 완료 - outboxId: {}", eventId);

        } catch (Exception e) {
            // 4. 실패 처리 (재시도 스케줄링)
            log.error("[OutboxConsumer] 이벤트 처리 실패 - outboxId: {}, error: {}", 
                    eventId, e.getMessage(), e);
            outboxService.markAsFailed(eventId, e.getMessage());
        }
    }

    /**
     * 이벤트 타입별 핸들러 라우팅
     */
    private void handleEvent(String eventType, String aggregateType, 
                              Long aggregateId, Map<String, Object> payload) {
        switch (eventType) {
            // ========== 채용공고 이벤트 ==========
            case "JOB_POSTING_CREATED" -> handleJobPostingCreated(aggregateId, payload);
            case "JOB_POSTING_UPDATED" -> handleJobPostingUpdated(aggregateId, payload);
            case "JOB_POSTING_STATUS_CHANGED" -> handleJobPostingStatusChanged(aggregateId, payload);
            case "JOB_POSTING_DELETED" -> handleJobPostingDeleted(aggregateId, payload);

            // ========== 지원 이벤트 ==========
            case "APPLICATION_SUBMITTED" -> handleApplicationSubmitted(aggregateId, payload);
            case "APPLICATION_STATUS_CHANGED" -> handleApplicationStatusChanged(aggregateId, payload);
            case "APPLICATION_VIEWED" -> handleApplicationViewed(aggregateId, payload);

            // ========== 면접 이벤트 ==========
            case "INTERVIEW_CREATED" -> handleInterviewCreated(aggregateId, payload);
            case "INTERVIEW_UPDATED" -> handleInterviewUpdated(aggregateId, payload);
            case "INTERVIEW_CANCELED" -> handleInterviewCanceled(aggregateId, payload);

            // ========== 결제 이벤트 ==========
            case "PAYMENT_COMPLETED" -> handlePaymentCompleted(aggregateId, payload);
            case "PAYMENT_FAILED" -> handlePaymentFailed(aggregateId, payload);

            // ========== 기업 이벤트 ==========
            case "EMPLOYER_CREATED" -> handleEmployerCreated(aggregateId, payload);
            case "EMPLOYER_UPDATED" -> handleEmployerUpdated(aggregateId, payload);

            default -> log.warn("[OutboxConsumer] 알 수 없는 이벤트 타입: {}", eventType);
        }
    }

    // ==================== 채용공고 이벤트 핸들러 ====================

    /**
     * 채용공고 등록 이벤트
     * 
     * payload 구조:
     * - jobId: job_posting.job_id
     * - userId: 등록한 사용자 (member.member_id)
     * - title: 공고 제목
     * - companyName: 기업명
     * - status: 공고 상태
     */
    private void handleJobPostingCreated(Long jobId, Map<String, Object> payload) {
        Long userId = getLong(payload, "userId");
        String title = getString(payload, "title");
        String companyName = getString(payload, "companyName");

        log.info("[OutboxConsumer] 채용공고 등록 처리 - jobId: {}, title: {}", jobId, title);

        // 기업 담당자에게 알림 (선택적)
        if (userId != null) {
            createNotification(userId, "JOB_POSTING_CREATED",
                    String.format("{\"jobId\":%d,\"title\":\"%s\",\"companyName\":\"%s\"}", 
                            jobId, title, companyName),
                    "/employer/jobs/" + jobId);
        }
    }

    private void handleJobPostingUpdated(Long jobId, Map<String, Object> payload) {
        log.info("[OutboxConsumer] 채용공고 수정 처리 - jobId: {}", jobId);
        // 필요 시 구현
    }

    private void handleJobPostingStatusChanged(Long jobId, Map<String, Object> payload) {
        String oldStatus = getString(payload, "oldStatus");
        String newStatus = getString(payload, "newStatus");
        log.info("[OutboxConsumer] 채용공고 상태 변경 처리 - jobId: {}, {} -> {}", 
                jobId, oldStatus, newStatus);
        // 필요 시 구현 (예: 마감 알림)
    }

    private void handleJobPostingDeleted(Long jobId, Map<String, Object> payload) {
        log.info("[OutboxConsumer] 채용공고 삭제 처리 - jobId: {}", jobId);
        // 필요 시 구현
    }

    // ==================== 지원 이벤트 핸들러 ====================

    /**
     * 지원서 제출 이벤트
     * 
     * payload 구조:
     * - applicationId: job_application.application_id
     * - jobId: job_posting.job_id
     * - userId: 지원자 (member.member_id)
     * - jobTitle: 공고 제목
     * - companyName: 기업명
     * - employerMemberId: 기업 담당자 (member.member_id) - 선택적
     */
    private void handleApplicationSubmitted(Long applicationId, Map<String, Object> payload) {
        Long userId = getLong(payload, "userId");
        Long jobId = getLong(payload, "jobId");
        String jobTitle = getString(payload, "jobTitle");
        String companyName = getString(payload, "companyName");
        Long employerMemberId = getLong(payload, "employerMemberId");

        log.info("[OutboxConsumer] 지원서 제출 처리 - applicationId: {}, userId: {}", 
                applicationId, userId);

        // 1. 지원자에게 알림
        if (userId != null) {
            createNotification(userId, "APPLICATION_SUBMITTED",
                    String.format("{\"applicationId\":%d,\"jobId\":%d,\"jobTitle\":\"%s\",\"companyName\":\"%s\"}", 
                            applicationId, jobId, jobTitle, companyName),
                    "/applications");
        }

        // 2. 기업 담당자에게 알림 (선택적)
        if (employerMemberId != null) {
            createNotification(employerMemberId, "NEW_APPLICATION_RECEIVED",
                    String.format("{\"applicationId\":%d,\"jobId\":%d,\"jobTitle\":\"%s\"}", 
                            applicationId, jobId, jobTitle),
                    "/employer/applications/" + applicationId);
        }
    }

    /**
     * 지원서 상태 변경 이벤트
     * 
     * payload 구조:
     * - applicationId: job_application.application_id
     * - candidateMemberId: 지원자 (member.member_id)
     * - jobTitle: 공고 제목
     * - companyName: 기업명
     * - newStatus: 새 상태 (VIEWED/INTERVIEW/HIRED/REJECTED)
     */
    private void handleApplicationStatusChanged(Long applicationId, Map<String, Object> payload) {
        Long candidateMemberId = getLong(payload, "candidateMemberId");
        String jobTitle = getString(payload, "jobTitle");
        String companyName = getString(payload, "companyName");
        String newStatus = getString(payload, "newStatus");

        log.info("[OutboxConsumer] 지원 상태 변경 처리 - applicationId: {}, status: {}", 
                applicationId, newStatus);

        // 지원자에게 알림
        if (candidateMemberId != null) {
            String eventType = switch (newStatus) {
                case "HIRED" -> "HIRED";
                case "REJECTED" -> "REJECTED";
                default -> "APPLICATION_STATUS_CHANGED";
            };

            createNotification(candidateMemberId, eventType,
                    String.format("{\"applicationId\":%d,\"jobTitle\":\"%s\",\"companyName\":\"%s\",\"status\":\"%s\"}", 
                            applicationId, jobTitle, companyName, newStatus),
                    "/applications");
        }
    }

    private void handleApplicationViewed(Long applicationId, Map<String, Object> payload) {
        Long candidateMemberId = getLong(payload, "candidateMemberId");
        String jobTitle = getString(payload, "jobTitle");
        String companyName = getString(payload, "companyName");

        log.info("[OutboxConsumer] 이력서 열람 처리 - applicationId: {}", applicationId);

        // 지원자에게 알림
        if (candidateMemberId != null) {
            createNotification(candidateMemberId, "APPLICATION_VIEWED",
                    String.format("{\"applicationId\":%d,\"jobTitle\":\"%s\",\"companyName\":\"%s\"}", 
                            applicationId, jobTitle, companyName),
                    "/applications");
        }
    }

    // ==================== 면접 이벤트 핸들러 ====================

    /**
     * 면접 일정 등록 이벤트
     * 
     * payload 구조:
     * - interviewId: interview_schedule.interview_id
     * - applicationId: job_application.application_id
     * - candidateMemberId: 지원자 (member.member_id)
     * - jobTitle: 공고 제목
     * - companyName: 기업명
     * - startAt: 면접 시작 시간
     * - location: 면접 장소
     */
    private void handleInterviewCreated(Long interviewId, Map<String, Object> payload) {
        Long candidateMemberId = getLong(payload, "candidateMemberId");
        String jobTitle = getString(payload, "jobTitle");
        String companyName = getString(payload, "companyName");
        String startAt = getString(payload, "startAt");
        String location = getString(payload, "location");

        log.info("[OutboxConsumer] 면접 일정 등록 처리 - interviewId: {}", interviewId);

        // 지원자에게 알림
        if (candidateMemberId != null) {
            createNotification(candidateMemberId, "INTERVIEW_SCHEDULED",
                    String.format("{\"interviewId\":%d,\"jobTitle\":\"%s\",\"companyName\":\"%s\",\"startAt\":\"%s\",\"location\":\"%s\"}", 
                            interviewId, jobTitle, companyName, startAt, location != null ? location : ""),
                    "/interviews/" + interviewId);
        }
    }

    private void handleInterviewUpdated(Long interviewId, Map<String, Object> payload) {
        Long candidateMemberId = getLong(payload, "candidateMemberId");
        String jobTitle = getString(payload, "jobTitle");
        String startAt = getString(payload, "startAt");

        log.info("[OutboxConsumer] 면접 일정 수정 처리 - interviewId: {}", interviewId);

        if (candidateMemberId != null) {
            createNotification(candidateMemberId, "INTERVIEW_SCHEDULED",
                    String.format("{\"interviewId\":%d,\"jobTitle\":\"%s\",\"startAt\":\"%s\",\"updated\":true}", 
                            interviewId, jobTitle, startAt),
                    "/interviews/" + interviewId);
        }
    }

    private void handleInterviewCanceled(Long interviewId, Map<String, Object> payload) {
        Long candidateMemberId = getLong(payload, "candidateMemberId");
        String jobTitle = getString(payload, "jobTitle");
        String companyName = getString(payload, "companyName");

        log.info("[OutboxConsumer] 면접 일정 취소 처리 - interviewId: {}", interviewId);

        if (candidateMemberId != null) {
            createNotification(candidateMemberId, "INTERVIEW_CANCELED",
                    String.format("{\"interviewId\":%d,\"jobTitle\":\"%s\",\"companyName\":\"%s\"}", 
                            interviewId, jobTitle, companyName),
                    "/applications");
        }
    }

    // ==================== 결제 이벤트 핸들러 ====================

    private void handlePaymentCompleted(Long paymentId, Map<String, Object> payload) {
        Long userId = getLong(payload, "userId");
        String productName = getString(payload, "productName");
        String amount = getString(payload, "amount");

        log.info("[OutboxConsumer] 결제 완료 처리 - paymentId: {}", paymentId);

        if (userId != null) {
            createNotification(userId, "PAYMENT_COMPLETED",
                    String.format("{\"paymentId\":%d,\"productName\":\"%s\",\"amount\":\"%s\"}", 
                            paymentId, productName, amount),
                    "/payments/" + paymentId);
        }
    }

    private void handlePaymentFailed(Long paymentId, Map<String, Object> payload) {
        Long userId = getLong(payload, "userId");
        String reason = getString(payload, "reason");

        log.info("[OutboxConsumer] 결제 실패 처리 - paymentId: {}", paymentId);

        if (userId != null) {
            createNotification(userId, "PAYMENT_FAILED",
                    String.format("{\"paymentId\":%d,\"reason\":\"%s\"}", paymentId, reason),
                    "/payments");
        }
    }

    // ==================== 기업 이벤트 핸들러 ====================

    private void handleEmployerCreated(Long employerId, Map<String, Object> payload) {
        log.info("[OutboxConsumer] 기업 등록 처리 - employerId: {}", employerId);
        // 필요 시 구현
    }

    private void handleEmployerUpdated(Long employerId, Map<String, Object> payload) {
        log.info("[OutboxConsumer] 기업 수정 처리 - employerId: {}", employerId);
        // 필요 시 구현
    }

    // ==================== Helper Methods ====================

    /**
     * 알림 생성 (NotificationService 호출)
     */
    private void createNotification(Long memberId, String eventType, 
                                     String payload, String linkUrl) {
        try {
            NotificationCreateRequest request = NotificationCreateRequest.builder()
                    .memberId(memberId)
                    .eventType(eventType)
                    .channel(NotificationChannel.PUSH)
                    .payload(payload)
                    .linkUrl(linkUrl)
                    .build();

            notificationService.createNotification(request);
        } catch (Exception e) {
            log.error("[OutboxConsumer] 알림 생성 실패 - memberId: {}, eventType: {}", 
                    memberId, eventType, e);
            // 알림 생성 실패는 이벤트 처리 실패로 간주하지 않음
        }
    }

    private Map<String, Object> parsePayload(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("[OutboxConsumer] Payload 파싱 실패: {}", json, e);
            return Map.of();
        }
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private static String getWorkerId() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            return hostname + "-" + Thread.currentThread().getId();
        } catch (Exception e) {
            return "worker-" + Thread.currentThread().getId();
        }
    }
}
