package com.example.pproject.outbox.service;

import com.example.pproject.outbox.entity.OutboxEvent;
import com.example.pproject.outbox.entity.OutboxEvent.OutboxStatus;
import com.example.pproject.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 아웃박스 서비스
 * 
 * ========================================
 * ERD 테이블: outbox_event (39번)
 * ========================================
 * 
 * 주요 기능:
 * - 이벤트 발행 (트랜잭션 내에서 Outbox 테이블에 저장)
 * - 이벤트 조회 (Consumer가 처리할 이벤트)
 * - 이벤트 상태 관리 (락 획득, 성공/실패 처리)
 * 
 * Outbox 패턴:
 * 1. 비즈니스 로직과 같은 트랜잭션에서 Outbox 테이블에 이벤트 저장
 * 2. 별도의 Consumer(Worker)가 Outbox 테이블을 폴링하여 이벤트 처리
 * 3. 처리 완료 후 상태 업데이트 (SUCCEEDED/FAILED)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    // 설정 값 (나중에 @ConfigurationProperties로 외부화 가능)
    private static final int MAX_RETRIES = 5;
    private static final int LOCK_TIMEOUT_MINUTES = 10;

    // ==================== 이벤트 발행 ====================

    /**
     * 이벤트 발행 (Outbox 테이블에 저장)
     * 
     * ERD 매핑:
     * - outbox_id    <- 자동 생성 (IDENTITY)
     * - event_type   <- eventType
     * - aggregate_type <- aggregateType
     * - aggregate_id <- aggregateId
     * - payload      <- payload (JSONB)
     * - status       <- 'PENDING' (DEFAULT)
     * - next_run_at  <- now() (DEFAULT)
     * - created_at   <- now() (DEFAULT)
     * - updated_at   <- now() (DEFAULT)
     * 
     * @param eventType 이벤트 타입 (예: APPLICATION_SUBMITTED)
     * @param aggregateType 집합체 타입 (예: APPLICATION)
     * @param aggregateId 집합체 ID (예: application_id)
     * @param payload 페이로드 Map
     * @return 저장된 이벤트
     */
    @Transactional
    public OutboxEvent publishEvent(String eventType, String aggregateType, 
                                     Long aggregateId, Map<String, Object> payload) {
        log.info("[Outbox] 이벤트 발행 - type: {}, aggregate: {}#{}", 
                eventType, aggregateType, aggregateId);

        String payloadJson = toJson(payload);

        OutboxEvent event = OutboxEvent.builder()
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payloadJson)
                .build();

        OutboxEvent saved = outboxRepository.save(event);
        log.debug("[Outbox] 이벤트 저장 완료 - outboxId: {}", saved.getId());

        return saved;
    }

    /**
     * 이벤트 발행 (JSON 문자열 직접 전달)
     */
    @Transactional
    public OutboxEvent publishEvent(String eventType, String aggregateType, 
                                     Long aggregateId, String payloadJson) {
        log.info("[Outbox] 이벤트 발행 - type: {}, aggregate: {}#{}", 
                eventType, aggregateType, aggregateId);

        OutboxEvent event = OutboxEvent.builder()
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payloadJson)
                .build();

        return outboxRepository.save(event);
    }

    // ==================== 이벤트 조회 (Consumer용) ====================

    /**
     * 처리 대기 이벤트 조회
     * - idx_outbox_pick 인덱스 활용: (status, next_run_at, outbox_id)
     * 
     * @param limit 조회 개수
     * @return 처리할 이벤트 목록
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> getPendingEvents(int limit) {
        return outboxRepository.findPendingEvents(limit);
    }

    /**
     * 이벤트 락 획득
     * - 원자적 업데이트로 동시성 제어
     * 
     * @param eventId 이벤트 ID (outbox_id)
     * @param workerId Worker 식별자
     * @return 락 획득 성공 여부
     */
    @Transactional
    public boolean acquireLock(Long eventId, String workerId) {
        int updated = outboxRepository.acquireLock(eventId, workerId, Instant.now());
        if (updated > 0) {
            log.debug("[Outbox] 락 획득 성공 - outboxId: {}, worker: {}", eventId, workerId);
            return true;
        }
        log.debug("[Outbox] 락 획득 실패 - outboxId: {}", eventId);
        return false;
    }

    // ==================== 이벤트 상태 변경 ====================

    /**
     * 이벤트 처리 성공
     * 
     * @param eventId 이벤트 ID
     */
    @Transactional
    public void markAsSucceeded(Long eventId) {
        OutboxEvent event = outboxRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다: " + eventId));
        
        event.markAsSucceeded();
        outboxRepository.save(event);
        
        log.info("[Outbox] 이벤트 처리 성공 - outboxId: {}, type: {}", 
                eventId, event.getEventType());
    }

    /**
     * 이벤트 처리 실패
     * 
     * @param eventId 이벤트 ID
     * @param error 에러 메시지
     */
    @Transactional
    public void markAsFailed(Long eventId, String error) {
        OutboxEvent event = outboxRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다: " + eventId));
        
        event.markAsFailed(error, MAX_RETRIES);
        outboxRepository.save(event);
        
        log.warn("[Outbox] 이벤트 처리 실패 - outboxId: {}, retry: {}/{}, error: {}", 
                eventId, event.getRetryCount(), MAX_RETRIES, error);
    }

    // ==================== 락 관리 ====================

    /**
     * 만료된 락 해제 (스케줄러에서 호출)
     * - Worker 장애 복구용
     * 
     * @return 해제된 이벤트 수
     */
    @Transactional
    public int releaseExpiredLocks() {
        Instant timeout = Instant.now().minusSeconds(LOCK_TIMEOUT_MINUTES * 60L);
        int released = outboxRepository.releaseExpiredLocks(timeout);
        
        if (released > 0) {
            log.info("[Outbox] 만료된 락 해제 - count: {}", released);
        }
        return released;
    }

    // ==================== 정리 (Cleanup) ====================

    /**
     * 오래된 성공 이벤트 삭제
     * - 스케줄러에서 호출
     * 
     * @param retentionDays 보관 기간 (일)
     * @return 삭제된 이벤트 수
     */
    @Transactional
    public int cleanupOldEvents(int retentionDays) {
        int deleted = outboxRepository.cleanupSucceededEvents(retentionDays);
        
        if (deleted > 0) {
            log.info("[Outbox] 오래된 이벤트 정리 - count: {}, retention: {}days", 
                    deleted, retentionDays);
        }
        return deleted;
    }

    // ==================== 통계 ====================

    /**
     * 상태별 이벤트 수 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getEventCounts() {
        return Map.of(
                "PENDING", outboxRepository.countByStatus(OutboxStatus.PENDING),
                "PROCESSING", outboxRepository.countByStatus(OutboxStatus.PROCESSING),
                "SUCCEEDED", outboxRepository.countByStatus(OutboxStatus.SUCCEEDED),
                "FAILED", outboxRepository.countByStatus(OutboxStatus.FAILED)
        );
    }

    // ==================== Helper ====================

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("[Outbox] JSON 변환 실패", e);
            return "{}";
        }
    }
}
