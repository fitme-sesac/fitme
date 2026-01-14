package com.example.pproject.notice.repository;

import com.example.pproject.notice.model.NoticeDelivery;
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
     * 특정 공지사항의 발송 결과 조회
     */
    @Query("SELECT nd FROM NoticeDelivery nd WHERE nd.noticeId = :noticeId ORDER BY nd.createdAt DESC")
    Page<NoticeDelivery> findByNoticeId(@Param("noticeId") Long noticeId, Pageable pageable);

    /**
     * 대기 중인 발송 건 조회 (배치 처리용)
     */
    @Query("SELECT nd FROM NoticeDelivery nd WHERE nd.status = 'PENDING' ORDER BY nd.createdAt ASC")
    List<NoticeDelivery> findPending();

    /**
     * 동일한 발송 기록이 있는지 확인
     */
    @Query("SELECT COUNT(nd) FROM NoticeDelivery nd WHERE nd.noticeId = :noticeId AND nd.memberId = :memberId AND nd.channel = :channel")
    Long countByNoticeAndMemberAndChannel(@Param("noticeId") Long noticeId, @Param("memberId") Long memberId, @Param("channel") String channel);
}
