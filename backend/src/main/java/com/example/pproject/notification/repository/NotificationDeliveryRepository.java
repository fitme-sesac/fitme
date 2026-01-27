package com.example.pproject.notification.repository;

import com.example.pproject.notification.entity.NotificationDelivery;
import com.example.pproject.notification.entity.NotificationDelivery.DeliveryStatus;
import com.example.pproject.notification.entity.NotificationTemplate.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 알림 발송 이력 Repository
 * 
 * ========================================
 * ERD 테이블: notification_delivery (38번)
 * ========================================
 * 
 * FK 관계:
 * - template_id -> notification_template(template_id)
 * - member_id -> member(member_id)
 */
@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    // ==================== 회원별 알림 조회 ====================

    /**
     * 회원별 알림 목록 조회 (페이징)
     * - member_id FK 활용
     * 
     * @param memberId 회원 ID (member.member_id)
     * @param pageable 페이징 정보
     * @return 알림 목록 (Page)
     */
    @Query("SELECT nd FROM NotificationDelivery nd WHERE nd.member.id = :memberId ORDER BY nd.createdAt DESC")
    Page<NotificationDelivery> findByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * 회원별 최근 알림 조회 (limit)
     * - 드롭다운용 최근 알림
     * 
     * @param memberId 회원 ID
     * @param pageable 페이징 (size = limit)
     * @return 최근 알림 목록
     */
    @Query("SELECT nd FROM NotificationDelivery nd WHERE nd.member.id = :memberId ORDER BY nd.createdAt DESC")
    List<NotificationDelivery> findRecentByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * 회원별 읽지 않은 알림 수 조회
     * 
     * @param memberId 회원 ID
     * @return 읽지 않은 알림 수
     */
    @Query("SELECT COUNT(nd) FROM NotificationDelivery nd WHERE nd.member.id = :memberId AND nd.isRead = false")
    long countUnreadByMemberId(@Param("memberId") Long memberId);

    /**
     * 회원별 읽지 않은 알림 목록 조회
     * 
     * @param memberId 회원 ID
     * @return 읽지 않은 알림 목록
     */
    @Query("SELECT nd FROM NotificationDelivery nd WHERE nd.member.id = :memberId AND nd.isRead = false ORDER BY nd.createdAt DESC")
    List<NotificationDelivery> findUnreadByMemberId(@Param("memberId") Long memberId);

    // ==================== 상태별 조회 ====================

    /**
     * 발송 대기 중인 알림 조회
     * - Outbox Consumer에서 처리할 알림 목록
     * 
     * @param status 발송 상태 (PENDING)
     * @param pageable 페이징 정보
     * @return 발송 대기 알림 목록
     */
    @Query("SELECT nd FROM NotificationDelivery nd WHERE nd.status = :status ORDER BY nd.createdAt ASC")
    List<NotificationDelivery> findByStatus(@Param("status") DeliveryStatus status, Pageable pageable);

    /**
     * 특정 채널의 발송 대기 알림 조회
     * 
     * @param channel 발송 채널
     * @param status 발송 상태
     * @param limit 조회 개수
     * @return 발송 대기 알림 목록
     */
    @Query("SELECT nd FROM NotificationDelivery nd WHERE nd.channel = :channel AND nd.status = :status ORDER BY nd.createdAt ASC")
    List<NotificationDelivery> findByChannelAndStatus(
            @Param("channel") NotificationChannel channel, 
            @Param("status") DeliveryStatus status, 
            Pageable pageable);

    // ==================== 업데이트 ====================

    /**
     * 회원의 모든 알림 읽음 처리
     * 
     * @param memberId 회원 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE NotificationDelivery nd SET nd.isRead = true WHERE nd.member.id = :memberId AND nd.isRead = false")
    int markAllAsReadByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 기간 이전 알림 삭제 (배치용)
     * 
     * @param before 기준 일시
     * @return 삭제된 행 수
     */
    @Modifying
    @Query("DELETE FROM NotificationDelivery nd WHERE nd.createdAt < :before")
    int deleteByCreatedAtBefore(@Param("before") Instant before);

    // ==================== 이벤트 타입별 조회 ====================

    /**
     * 이벤트 타입별 회원 알림 조회
     * 
     * @param memberId 회원 ID
     * @param eventType 이벤트 타입
     * @return 알림 목록
     */
    @Query("SELECT nd FROM NotificationDelivery nd WHERE nd.member.id = :memberId AND nd.eventType = :eventType ORDER BY nd.createdAt DESC")
    List<NotificationDelivery> findByMemberIdAndEventType(
            @Param("memberId") Long memberId, 
            @Param("eventType") String eventType);

    /**
     * 회원 + 특정 알림 조회 (소유권 확인용)
     * 
     * @param id 알림 ID
     * @param memberId 회원 ID
     * @return 알림 Optional
     */
    @Query("SELECT nd FROM NotificationDelivery nd WHERE nd.id = :id AND nd.member.id = :memberId")
    java.util.Optional<NotificationDelivery> findByIdAndMemberId(@Param("id") Long id, @Param("memberId") Long memberId);
}
