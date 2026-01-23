package com.example.pproject.report.repository;

import com.example.pproject.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * 신고자 ID로 신고 조회
     */
    Page<Report> findByReporterMemberId(Long reporterMemberId, Pageable pageable);

    /**
     * 상태별 신고 조회
     */
    Page<Report> findByStatus(String status, Pageable pageable);

    /**
     * 신고 대상 타입별 조회
     */
    Page<Report> findByTargetType(String targetType, Pageable pageable);

    /**
     * 회원 ID로 신고된 건수
     */
    long countByTargetMemberId(Long targetMemberId);

    /**
     * 채용공고 ID로 신고된 건수
     */
    long countByTargetJobId(Long targetJobId);

    /**
     * 상태별 개수
     */
    long countByStatus(String status);
}
