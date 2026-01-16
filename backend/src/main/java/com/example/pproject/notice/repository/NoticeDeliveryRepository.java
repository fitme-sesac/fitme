

package com.example.pproject.notice.repository;

import com.example.pproject.notice.entity.NoticeDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeDeliveryRepository extends JpaRepository<NoticeDelivery, Long> {

    /**
     * 공지사항별 배송 기록 조회
     */
    @Query("SELECT nd FROM NoticeDelivery nd WHERE nd.notice.id = :noticeId ORDER BY nd.createdAt DESC")
    Page<NoticeDelivery> findByNoticeId(@Param("noticeId") Long noticeId, Pageable pageable);

    /**
     * 특정 회원의 배송 기록 조회
     */
    List<NoticeDelivery> findByMemberId(Long memberId);

    /**
     * 배송 상태별 조회
     */
    @Query("SELECT nd FROM NoticeDelivery nd WHERE nd.notice.id = :noticeId AND nd.status = :status")
    List<NoticeDelivery> findByNoticeIdAndStatus(
            @Param("noticeId") Long noticeId,
            @Param("status") NoticeDelivery.DeliveryStatus status);

    /**
     * 채널별 배송 기록
     */
    @Query("SELECT nd FROM NoticeDelivery nd WHERE nd.notice.id = :noticeId AND nd.channel = :channel")
    List<NoticeDelivery> findByNoticeIdAndChannel(
            @Param("noticeId") Long noticeId,
            @Param("channel") NoticeDelivery.DeliveryChannel channel);
}