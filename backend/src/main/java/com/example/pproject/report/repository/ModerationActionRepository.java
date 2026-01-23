package com.example.pproject.report.repository;

import com.example.pproject.report.entity.ModerationAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ModerationActionRepository extends JpaRepository<ModerationAction, Long> {

    /**
     * 신고 ID로 중재 조치 조회
     */
    Optional<ModerationAction> findByReportId(Long reportId);

    /**
     * 관리자 ID로 처리 내역 조회
     */
    long countByAdminMemberId(Long adminMemberId);
}