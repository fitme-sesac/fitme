package com.example.pproject.notice.repository;

import com.example.pproject.notice.entity.NoticeDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeDeliveryRepository extends JpaRepository<NoticeDelivery, Long> {
    // 필요 시 특정 공지의 발송 이력 조회 등의 메서드 추가 가능
    // List<NoticeDelivery> findByNoticeId(Long noticeId);
}