package com.example.pproject.common.controller;

import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.repository.JobEntityRepository;
// import com.example.pproject.report.repository.ReportRepository;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 관리자용 통합 대시보드 API
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SERVICEADMIN')")
@Slf4j
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final JobEntityRepository jobEntityRepository;
    private final EmployerRepository employerRepository;
    private final com.example.pproject.payment.repository.PaymentRepository paymentRepository;

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

        // 신고 Repository가 countByStatus 같은 메소드를 가지고 있다고 가정
        // 없으면 findAll().stream().filter...로 할 수도 있으나 성능상 count 쿼리 권장
        // 일단 기본 count만 반환하거나 0으로 mock 처리
        long pendingReports = 0;
        try {
            // pendingReports = reportRepository.countByStatus("PENDING");
        } catch (Exception e) {
            // ignore
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMembers", totalMembers);
        stats.put("totalJobs", totalJobs);
        stats.put("totalCompanies", totalCompanies);
        stats.put("totalRevenue", totalRevenue);
        stats.put("pendingReports", pendingReports);

        // Mock data for trends
        stats.put("todayApplications", 15);
        stats.put("weeklyNewMembers", 42);

        return ResponseEntity.ok(stats);
    }
}
