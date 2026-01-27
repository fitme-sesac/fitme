package com.example.pproject.outbox.repository;

import com.example.pproject.outbox.entity.OutboxEvent;
import com.example.pproject.outbox.entity.OutboxEvent.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 아웃박스 이벤트 Repository
 * 
 * ========================================
 * ERD 테이블: outbox_event (39번)
 * ========================================
 * 
 * 인덱스 활용:
 * - idx_outbox_pick: (status, next_run_at, outbox_id) - 처리할 이벤트 조회
 * - idx_outbox_agg: (aggregate_type, aggregate_id) - 특정 엔티티 이벤트 조회
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    // ==================== 이벤트 조회 (Consumer용) ====================

    /**
     * 처리 대기 중인 이벤트 조회 (PENDING + next_run_at <= now)
     * - idx_outbox_pick 인덱스 활용
     * - Worker가 처리할 이벤트 목록
     * 
     * @param status 상태 (PENDING)
     * @param now 현재 시각
     * @param pageable 페이징 (limit)
     * @return 처리할 이벤트 목록
     */
    @Query("""
        SELECT o FROM OutboxEvent o 
        WHERE o.status = :status 
          AND o.nextRunAt <= :now 
        ORDER BY o.nextRunAt ASC, o.id ASC
        """)
    List<OutboxEvent> findPendingEvents(
            @Param("status") OutboxStatus status, 
            @Param("now") Instant now,
            Pageable pageable);

    /**
     * 처리 대기 이벤트 조회 (간편 메서드)
     */
    default List<OutboxEvent> findPendingEvents(int limit) {
        return findPendingEvents(OutboxStatus.PENDING, Instant.now(), Pageable.ofSize(limit));
    }

    /**
     * 락이 만료된 이벤트 조회 (PROCESSING + lockedAt < timeout)
     * - 장애 복구용: Worker 장애 시 락 해제
     * 
     * @param status 상태 (PROCESSING)
     * @param timeout 락 타임아웃 시각
     * @return 락 만료 이벤트 목록
     */
    @Query("""
        SELECT o FROM OutboxEvent o 
        WHERE o.status = :status 
          AND o.lockedAt < :timeout
        """)
    List<OutboxEvent> findExpiredLockedEvents(
            @Param("status") OutboxStatus status, 
            @Param("timeout") Instant timeout);

    /**
     * 락 만료 이벤트 조회 (간편 메서드)
     * 
     * @param timeoutMinutes 타임아웃 (분)
     */
    default List<OutboxEvent> findExpiredLockedEvents(int timeoutMinutes) {
        return findExpiredLockedEvents(
                OutboxStatus.PROCESSING, 
                Instant.now().minusSeconds(timeoutMinutes * 60L)
        );
    }

    // ==================== 집합체별 조회 ====================

    /**
     * 특정 엔티티의 이벤트 조회
     * - idx_outbox_agg 인덱스 활용
     * 
     * @param aggregateType 집합체 타입 (예: JOB_POSTING)
     * @param aggregateId 집합체 ID (예: job_id)
     * @return 이벤트 목록
     */
    @Query("""
        SELECT o FROM OutboxEvent o 
        WHERE o.aggregateType = :aggregateType 
          AND o.aggregateId = :aggregateId 
        ORDER BY o.createdAt DESC
        """)
    List<OutboxEvent> findByAggregate(
            @Param("aggregateType") String aggregateType, 
            @Param("aggregateId") Long aggregateId);

    /**
     * 특정 이벤트 타입 조회
     * 
     * @param eventType 이벤트 타입 (예: APPLICATION_SUBMITTED)
     * @param status 상태
     * @return 이벤트 목록
     */
    List<OutboxEvent> findByEventTypeAndStatus(String eventType, OutboxStatus status);

    // ==================== 상태 업데이트 ====================

    /**
     * 이벤트 락 획득 (비관적 락 + 원자적 업데이트)
     * - Worker가 이벤트 처리 시작 시 호출
     * 
     * @param id 이벤트 ID
     * @param workerId Worker 식별자
     * @param now 현재 시각
     * @return 업데이트된 행 수 (1이면 성공)
     */
    @Modifying
    @Query("""
        UPDATE OutboxEvent o 
        SET o.status = 'PROCESSING', 
            o.lockedAt = :now, 
            o.lockedBy = :workerId,
            o.updatedAt = :now
        WHERE o.id = :id 
          AND o.status = 'PENDING'
        """)
    int acquireLock(
            @Param("id") Long id, 
            @Param("workerId") String workerId,
            @Param("now") Instant now);

    /**
     * 만료된 락 해제 (배치)
     * 
     * @param timeout 락 타임아웃 시각
     * @return 해제된 행 수
     */
    @Modifying
    @Query("""
        UPDATE OutboxEvent o 
        SET o.status = 'PENDING', 
            o.lockedAt = NULL, 
            o.lockedBy = NULL,
            o.updatedAt = CURRENT_TIMESTAMP
        WHERE o.status = 'PROCESSING' 
          AND o.lockedAt < :timeout
        """)
    int releaseExpiredLocks(@Param("timeout") Instant timeout);

    // ==================== 정리 (Cleanup) ====================

    /**
     * 오래된 성공 이벤트 삭제 (배치용)
     * - 디스크 공간 확보
     * 
     * @param status 상태 (SUCCEEDED)
     * @param before 기준 일시
     * @return 삭제된 행 수
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.status = :status AND o.processedAt < :before")
    int deleteByStatusAndProcessedAtBefore(
            @Param("status") OutboxStatus status, 
            @Param("before") Instant before);

    /**
     * 성공 이벤트 정리 (간편 메서드)
     * 
     * @param retentionDays 보관 기간 (일)
     */
    default int cleanupSucceededEvents(int retentionDays) {
        return deleteByStatusAndProcessedAtBefore(
                OutboxStatus.SUCCEEDED, 
                Instant.now().minusSeconds(retentionDays * 24 * 60 * 60L)
        );
    }

    // ==================== 통계 ====================

    /**
     * 상태별 이벤트 수 조회
     */
    long countByStatus(OutboxStatus status);

    /**
     * 이벤트 타입별 개수 조회
     */
    @Query("SELECT o.eventType, COUNT(o) FROM OutboxEvent o WHERE o.status = :status GROUP BY o.eventType")
    List<Object[]> countByEventTypeAndStatus(@Param("status") OutboxStatus status);
}
