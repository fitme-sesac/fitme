package com.example.pproject.job.repository;

import com.example.pproject.job.entity.JobViewLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JobViewLogRepository extends JpaRepository<JobViewLog, Long> {

    /**
     * 회원의 열람 로그 조회 (최신순)
     */
    List<JobViewLog> findByMemberIdOrderByViewedAtDesc(Long memberId);

    /**
     * 최근 본 공고 목록 조회 (중복 제거, 최신순)
     * - 같은 공고를 여러 번 본 경우 가장 최근 기록만 조회
     */
    @Query("SELECT v FROM JobViewLog v " +
            "WHERE v.memberId = :memberId " +
            "AND v.viewedAt = (SELECT MAX(v2.viewedAt) FROM JobViewLog v2 WHERE v2.memberId = :memberId AND v2.job.id = v.job.id) " +
            "AND v.job.deletedAt IS NULL " +
            "ORDER BY v.viewedAt DESC")
    List<JobViewLog> findRecentViewedJobs(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * 특정 기간 내 열람 로그 조회
     */
    @Query("SELECT v FROM JobViewLog v " +
            "WHERE v.memberId = :memberId " +
            "AND v.viewedAt BETWEEN :startDate AND :endDate " +
            "ORDER BY v.viewedAt DESC")
    List<JobViewLog> findByMemberIdAndDateRange(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 공고의 열람 수 조회
     */
    long countByJobId(Long jobId);

    /**
     * 회원이 특정 공고를 최근에 본 적 있는지 확인 (중복 로그 방지용)
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END " +
            "FROM JobViewLog v " +
            "WHERE v.memberId = :memberId " +
            "AND v.job.id = :jobId " +
            "AND v.viewedAt > :since")
    boolean existsRecentView(
            @Param("memberId") Long memberId,
            @Param("jobId") Long jobId,
            @Param("since") LocalDateTime since);
}
