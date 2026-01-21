package com.example.pproject.outbox.consumer;

import com.example.pproject.Constant.NotificationEventType;
import com.example.pproject.Constant.OutboxStatus;
import com.example.pproject.notification.service.NotificationService;
import com.example.pproject.outbox.entity.OutboxEvent;
import com.example.pproject.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 아웃박스 이벤트 소비자
 * - 주기적으로 대기 중인 이벤트를 폴링하여 처리
 * - 실패 시 재시도 로직 포함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventConsumer {

    private final OutboxEventRepository outboxEventRepository;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;
    private static final int LOCK_TIMEOUT_MINUTES = 5;

    private final String workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);

    /**
     * 주기적으로 대기 중인 이벤트 처리 (5초마다)
     */
    @Scheduled(fixedDelay = 5000)
    public void processPendingEvents() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lockTimeout = now.minusMinutes(LOCK_TIMEOUT_MINUTES);

            // 타임아웃된 락 해제 (트랜잭션 내에서 실행)
            transactionTemplate.executeWithoutResult(status -> {
                int releasedLocks = outboxEventRepository.releaseTimedOutLocks(
                        OutboxStatus.PENDING, OutboxStatus.PROCESSING, lockTimeout);
                if (releasedLocks > 0) {
                    log.info("타임아웃된 락 해제: {} 건", releasedLocks);
                }
            });

            // 처리할 이벤트 조회
            List<OutboxEvent> events = outboxEventRepository.findPendingEvents(OutboxStatus.PENDING, now);

            if (!events.isEmpty()) {
                log.info("처리 대기 중인 이벤트: {} 건", events.size());

                for (OutboxEvent event : events) {
                    // 각 이벤트를 별도의 트랜잭션에서 처리
                    processEventInTransaction(event.getId());
                }
            }
        } catch (Exception e) {
            log.error("아웃박스 이벤트 처리 중 오류 발생", e);
        }
    }

    /**
     * 트랜잭션 내에서 이벤트 처리 (TransactionTemplate 사용)
     */
    private void processEventInTransaction(Long eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                OutboxEvent event = outboxEventRepository.findById(eventId)
                        .orElseThrow(() -> new IllegalStateException("이벤트를 찾을 수 없습니다: " + eventId));
                
                // 이미 처리 중이거나 완료된 경우 스킵
                if (event.getStatus() != OutboxStatus.PENDING) {
                    log.debug("이벤트가 이미 처리 중이거나 완료됨: outboxId={}, status={}", eventId, event.getStatus());
                    return;
                }
                
                processEvent(event);
            } catch (Exception e) {
                log.error("이벤트 처리 실패: eventId={}, error={}", eventId, e.getMessage(), e);
                status.setRollbackOnly();
            }
        });
    }

    /**
     * 개별 이벤트 처리 (트랜잭션 내에서 호출됨)
     */
    private void processEvent(OutboxEvent event) {
        try {
            // 락 획득
            event.startProcessing(workerId);
            outboxEventRepository.save(event);

            log.debug("이벤트 처리 시작: outboxId={}, eventType={}", event.getId(), event.getEventType());

            // 이벤트 타입에 따라 처리
            processEventByType(event);

            // 성공 처리
            event.markAsSucceeded();
            outboxEventRepository.save(event);

            log.info("이벤트 처리 완료: outboxId={}, eventType={}", event.getId(), event.getEventType());

        } catch (Exception e) {
            log.error("이벤트 처리 실패: outboxId={}, eventType={}, error={}",
                    event.getId(), event.getEventType(), e.getMessage(), e);

            // 실패 처리 (재시도 스케줄링)
            event.markAsFailed(e.getMessage(), MAX_RETRIES);
            outboxEventRepository.save(event);
        }
    }

    /**
     * 이벤트 타입별 처리 분기
     */
    private void processEventByType(OutboxEvent event) {
        String eventType = event.getEventType();
        Map<String, Object> payload = event.getPayload();

        // NOTIFICATION_ 접두사가 있는 이벤트는 알림 처리
        if (eventType.startsWith("NOTIFICATION_")) {
            processNotificationEvent(event);
            return;
        }

        // 기타 이벤트 타입 처리
        switch (eventType) {
            case "EMAIL_SEND" -> processEmailEvent(event);
            case "SMS_SEND" -> processSmsEvent(event);
            case "WEBHOOK_CALL" -> processWebhookEvent(event);
            default -> log.warn("알 수 없는 이벤트 타입: {}", eventType);
        }
    }

    /**
     * 알림 이벤트 처리
     */
    private void processNotificationEvent(OutboxEvent event) {
        Map<String, Object> payload = event.getPayload();

        Integer targetMemberId = (Integer) payload.get("targetMemberId");
        String eventTypeName = (String) payload.get("notificationEventType");
        String title = (String) payload.get("title");
        String message = (String) payload.get("message");
        String linkUrl = (String) payload.get("linkUrl");

        if (targetMemberId == null) {
            throw new IllegalArgumentException("targetMemberId가 없습니다.");
        }

        NotificationEventType notificationEventType;
        try {
            notificationEventType = NotificationEventType.valueOf(eventTypeName);
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 알림 이벤트 타입: {}", eventTypeName);
            notificationEventType = NotificationEventType.SYSTEM_NOTICE;
        }

        // 알림 생성
        notificationService.createNotification(
                targetMemberId,
                notificationEventType,
                title,
                message,
                linkUrl,
                payload
        );

        log.info("알림 생성 완료: memberId={}, eventType={}", targetMemberId, notificationEventType);
    }

    /**
     * 이메일 발송 이벤트 처리 (추후 구현)
     */
    private void processEmailEvent(OutboxEvent event) {
        log.info("이메일 발송 이벤트 처리: outboxId={}", event.getId());
        // TODO: 이메일 발송 로직 구현
    }

    /**
     * SMS 발송 이벤트 처리 (추후 구현)
     */
    private void processSmsEvent(OutboxEvent event) {
        log.info("SMS 발송 이벤트 처리: outboxId={}", event.getId());
        // TODO: SMS 발송 로직 구현
    }

    /**
     * 웹훅 호출 이벤트 처리 (추후 구현)
     */
    private void processWebhookEvent(OutboxEvent event) {
        log.info("웹훅 호출 이벤트 처리: outboxId={}", event.getId());
        // TODO: 웹훅 호출 로직 구현
    }

    /**
     * 실패한 이벤트 수동 재처리
     */
    @Transactional
    public int retryFailedEvents() {
        List<OutboxEvent> failedEvents = outboxEventRepository.findByStatusOrderByCreatedAtDesc(OutboxStatus.FAILED);

        int retriedCount = 0;
        for (OutboxEvent event : failedEvents) {
            event.setStatus(OutboxStatus.PENDING);
            event.setRetryCount(0);
            event.setNextRunAt(LocalDateTime.now());
            event.setLastError(null);
            outboxEventRepository.save(event);
            retriedCount++;
        }

        log.info("실패한 이벤트 재처리 대기열 등록: {} 건", retriedCount);
        return retriedCount;
    }

    /**
     * 오래된 성공 이벤트 정리 (30일 이전)
     */
    @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime before = LocalDateTime.now().minusDays(30);
        int deleted = outboxEventRepository.deleteOldSucceededEvents(before);
        log.info("오래된 성공 이벤트 정리: {} 건 삭제", deleted);
    }
}
