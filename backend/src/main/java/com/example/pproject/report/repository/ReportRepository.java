package com.example.pproject.report.repository;

import com.example.pproject.report.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    Page<Report> findByReporterMemberId(Long reporterMemberId, Pageable pageable);
    Page<Report> findByStatus(String status, Pageable pageable);
    Page<Report> findByTargetType(String targetType, Pageable pageable);

    long countByTargetMemberId(Long targetMemberId);
    long countByTargetJobId(Long targetJobId);
}