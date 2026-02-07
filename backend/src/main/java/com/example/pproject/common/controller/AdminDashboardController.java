package com.example.pproject.common.controller;

import com.example.pproject.application.repository.JobApplicationRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.repository.JobEntityRepository;
import com.example.pproject.report.repository.ReportRepository;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 관리자용 통합 대시보드 API
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
@Slf4j
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final JobEntityRepository jobEntityRepository;
    private final EmployerRepository employerRepository;
    private final com.example.pproject.payment.repository.PaymentRepository paymentRepository;
    private final ReportRepository reportRepository;
    private final JobApplicationRepository jobApplicationRepository;

    /**
     * GET /api/v1/admin/dashboard/stats
     * 전체 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        log.info("관리자 대시보드 통계 조회 요청");

        long totalMembers = userRepository.count();
        long totalJobs = jobEntityRepository.count();
        long totalCompanies = employerRepository.count();
        long totalRevenue = paymentRepository.sumTotalRevenue();

        // 대기중인 신고 건수 조회
        long pendingReports = 0;
        try {
            pendingReports = reportRepository.countByStatus("OPEN");
        } catch (Exception e) {
            log.warn("대기중 신고 건수 조회 실패: {}", e.getMessage());
        }

        // 오늘 지원 건수 조회
        long todayApplications = 0;
        try {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            todayApplications = jobApplicationRepository.countTodayApplications(startOfDay);
        } catch (Exception e) {
            log.warn("오늘 지원 건수 조회 실패: {}", e.getMessage());
        }

        // 주간 신규 회원 수 조회 (최근 7일)
        long weeklyNewMembers = 0;
        try {
            Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
            weeklyNewMembers = userRepository.countWeeklyNewMembers(weekAgo);
        } catch (Exception e) {
            log.warn("주간 신규 회원 수 조회 실패: {}", e.getMessage());
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMembers", totalMembers);
        stats.put("totalJobs", totalJobs);
        stats.put("totalCompanies", totalCompanies);
        stats.put("totalRevenue", totalRevenue);
        stats.put("pendingReports", pendingReports);
        stats.put("todayApplications", todayApplications);
        stats.put("weeklyNewMembers", weeklyNewMembers);

        return ResponseEntity.ok(stats);
    }
}
