package com.example.pproject.notification.repository;

import com.example.pproject.Constant.NotificationDeliveryStatus;
import com.example.pproject.notification.entity.NotificationDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    /**
     * 특정 회원의 알림 목록 조회 (최신순)
     */
    List<NotificationDelivery> findByMemberIdOrderByCreatedAtDesc(Integer memberId);

    /**
     * 특정 회원의 알림 목록 조회 (페이징)
     */
    Page<NotificationDelivery> findByMemberIdOrderByCreatedAtDesc(Integer memberId, Pageable pageable);

    /**
     * 특정 회원의 읽지 않은 알림 수 (payload에 readAt이 없는 것)
     * PostgreSQL JSONB 쿼리 사용
     */
    @Query(value = "SELECT COUNT(*) FROM notification_delivery WHERE member_id = :memberId AND (payload IS NULL OR payload->>'readAt' IS NULL)", nativeQuery = true)
    long countUnreadByMemberId(@Param("memberId") Integer memberId);

    /**
     * 특정 회원의 최근 N개 알림 조회
     */
    @Query("SELECT n FROM NotificationDelivery n WHERE n.member.id = :memberId ORDER BY n.createdAt DESC")
    List<NotificationDelivery> findRecentByMemberId(@Param("memberId") Integer memberId, Pageable pageable);

    /**
     * 발송 대기 중인 알림 조회
     */
    List<NotificationDelivery> findByStatus(NotificationDeliveryStatus status);
}
