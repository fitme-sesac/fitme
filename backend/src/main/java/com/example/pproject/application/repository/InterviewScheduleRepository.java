package com.example.pproject.application.repository;

import com.example.pproject.Constant.InterviewStatus;
import com.example.pproject.application.entity.InterviewSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewScheduleRepository extends JpaRepository<InterviewSchedule, Long> {

    /**
     * 지원 ID로 면접 일정 조회 (최신순)
     */
    List<InterviewSchedule> findByApplicationIdOrderByStartAtDesc(Long applicationId);

    /**
     * 지원자(회원)의 모든 면접 일정 조회
     */
    @Query("SELECT i FROM InterviewSchedule i " +
            "JOIN i.application a " +
            "WHERE a.member.id = :memberId " +
            "ORDER BY i.startAt DESC")
    List<InterviewSchedule> findByMemberId(@Param("memberId") Long memberId);

    /**
     * 지원자의 다가오는 면접 일정 조회 (확정된 것만)
     */
    @Query("SELECT i FROM InterviewSchedule i " +
            "JOIN i.application a " +
            "WHERE a.member.id = :memberId " +
            "AND i.status = :status " +
            "AND i.startAt > :now " +
            "ORDER BY i.startAt ASC")
    List<InterviewSchedule> findUpcomingByMemberIdAndStatus(
            @Param("memberId") Long memberId,
            @Param("status") InterviewStatus status,
            @Param("now") LocalDateTime now);

    /**
     * 기업(고용주)의 모든 면접 일정 조회
     */
    @Query("SELECT i FROM InterviewSchedule i " +
            "JOIN i.application a " +
            "JOIN a.job j " +
            "WHERE j.employerId = :employerId " +
            "ORDER BY i.startAt DESC")
    List<InterviewSchedule> findByEmployerId(@Param("employerId") Long employerId);

    /**
     * 기업의 다가오는 면접 일정 조회
     */
    @Query("SELECT i FROM InterviewSchedule i " +
            "JOIN i.application a " +
            "JOIN a.job j " +
            "WHERE j.employerId = :employerId " +
            "AND i.status IN :statuses " +
            "AND i.startAt > :now " +
            "ORDER BY i.startAt ASC")
    List<InterviewSchedule> findUpcomingByEmployerId(
            @Param("employerId") Long employerId,
            @Param("statuses") List<InterviewStatus> statuses,
            @Param("now") LocalDateTime now);

    /**
     * 특정 기간 내 면접 일정 조회 (캘린더용)
     */
    @Query("SELECT i FROM InterviewSchedule i " +
            "JOIN i.application a " +
            "WHERE a.member.id = :memberId " +
            "AND i.startAt BETWEEN :startDate AND :endDate " +
            "ORDER BY i.startAt ASC")
    List<InterviewSchedule> findByMemberIdAndDateRange(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
