package com.example.pproject.inquiry.repository;

import com.example.pproject.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    /**
     * 회원 ID로 문의 조회
     */
    Page<Inquiry> findByMemberId(Long memberId, Pageable pageable);

    /**
     * 상태별 문의 조회
     */
    Page<Inquiry> findByStatus(String status, Pageable pageable);

    /**
     * 회원 ID와 상태로 문의 조회
     */
    Page<Inquiry> findByMemberIdAndStatus(Long memberId, String status, Pageable pageable);

    /**
     * 회원 ID로 응답 대기 중인 문의 개수
     */
    long countByMemberIdAndStatus(Long memberId, String status);

    /**
     * 카테고리별 문의 조회
     */
    List<Inquiry> findByCategory(String category);
}
