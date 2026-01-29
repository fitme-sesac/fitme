package com.example.pproject.inquiry.repository;

import com.example.pproject.inquiry.entity.InquiryMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InquiryMessageRepository extends JpaRepository<InquiryMessage, Long> {

    /**
     * 문의 ID로 메시지 조회 (작성일 오름차순 정렬)
     * 과거 메시지가 먼저, 최신 메시지가 나중에 나오도록 정렬합니다.
     */
    List<InquiryMessage> findByInquiryIdOrderByCreatedAtAsc(Long inquiryId);

    /**
     * 문의별 메시지 개수
     */
    long countByInquiryId(Long inquiryId);
}