package com.example.pproject.report.repository;

import com.example.pproject.report.entity.ModerationAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ModerationActionRepository extends JpaRepository<ModerationAction, Long> {
    boolean existsByReportId(Long reportId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ModerationAction m WHERE m.reportId = :reportId")
    void deleteByReportId(@Param("reportId") Long reportId);
}