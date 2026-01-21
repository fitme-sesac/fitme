package com.example.pproject.outbox.repository;

import com.example.pproject.Constant.OutboxStatus;
import com.example.pproject.outbox.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * 처리 대기 중인 이벤트 조회 (락 없이, 다음 실행 시간이 현재 시간 이전인 것)
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = :status AND o.nextRunAt <= :now ORDER BY o.nextRunAt ASC, o.id ASC")
    List<OutboxEvent> findPendingEvents(@Param("status") OutboxStatus status, @Param("now") LocalDateTime now);

    /**
     * 처리 대기 중인 이벤트 N개 조회 및 락 획득 (비관적 락)
     */
    @Query(value = """
        SELECT * FROM outbox_event 
        WHERE status = 'PENDING' 
          AND next_run_at <= :now 
          AND (locked_at IS NULL OR locked_at < :lockTimeout)
        ORDER BY next_run_at ASC, outbox_id ASC 
        LIMIT :limit 
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEvent> findAndLockPendingEvents(
        @Param("now") LocalDateTime now,
        @Param("lockTimeout") LocalDateTime lockTimeout,
        @Param("limit") int limit
    );

    /**
     * 특정 집계의 이벤트 조회
     */
    List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(
        String aggregateType, Long aggregateId
    );

    /**
     * 타임아웃된 락 해제
     */
    @Modifying
    @Query("UPDATE OutboxEvent o SET o.status = :pendingStatus, o.lockedAt = NULL, o.lockedBy = NULL WHERE o.status = :processingStatus AND o.lockedAt < :timeout")
    int releaseTimedOutLocks(@Param("pendingStatus") OutboxStatus pendingStatus, @Param("processingStatus") OutboxStatus processingStatus, @Param("timeout") LocalDateTime timeout);

    /**
     * 특정 상태의 이벤트 수
     */
    long countByStatus(OutboxStatus status);

    /**
     * 실패한 이벤트 조회
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtDesc(OutboxStatus status);

    /**
     * 특정 이벤트 타입의 대기 중인 이벤트 조회
     */
    List<OutboxEvent> findByEventTypeAndStatus(String eventType, OutboxStatus status);

    /**
     * 오래된 성공 이벤트 삭제 (정리용)
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.status = 'SUCCEEDED' AND o.processedAt < :before")
    int deleteOldSucceededEvents(@Param("before") LocalDateTime before);
}
