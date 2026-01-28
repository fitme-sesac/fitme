// src/main/java/com/example/pproject/report/controller/ReportController.java
package com.example.pproject.report.controller;

import com.example.pproject.report.service.ReportService;
import com.example.pproject.report.dto.request.CreateReportRequest;
import com.example.pproject.report.dto.request.ProcessReportRequest;
import com.example.pproject.report.dto.response.ReportResponse;
import com.example.pproject.report.dto.response.ModerationActionResponse;
import com.example.pproject.report.dto.response.MemberPenaltyPointResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * POST /api/v1/reports
     * 신고 생성
     */
    @PostMapping
    public ResponseEntity<ReportResponse> createReport(
            @Valid @RequestBody CreateReportRequest request) {
        log.info("신고 생성 요청: reporterMemberId={}", request.getReporterMemberId());
        ReportResponse response = reportService.createReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/reports/{reportId}
     * 신고 상세 조회
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportResponse> getReport(
            @PathVariable Long reportId) {
        log.info("신고 조회: reportId={}", reportId);
        ReportResponse response = reportService.getReport(reportId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/reports/reporter/{reporterMemberId}
     * 신고자별 신고 목록
     */
    @GetMapping("/reporter/{reporterMemberId}")
    public ResponseEntity<Page<ReportResponse>> getReportsByReporter(
            @PathVariable Long reporterMemberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("신고자별 신고 목록 조회: reporterMemberId={}", reporterMemberId);
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportResponse> response = reportService.getReportsByReporter(reporterMemberId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/reports/status/{status}
     * 상태별 신고 목록 (관리자용)
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<ReportResponse>> getReportsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("상태별 신고 목록 조회: status={}", status);
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportResponse> response = reportService.getReportsByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/reports/by-target-type/{targetType}
     * 신고 대상 타입별 조회
     */
    @GetMapping("/by-target-type/{targetType}")
    public ResponseEntity<Page<ReportResponse>> getReportsByTargetType(
            @PathVariable String targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("신고 대상 타입별 조회: targetType={}", targetType);
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportResponse> response = reportService.getReportsByTargetType(targetType, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/reports/{reportId}/process
     * 신고 처리 (중재 조치)
     */
    @PostMapping("/{reportId}/process")
    public ResponseEntity<ModerationActionResponse> processReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ProcessReportRequest request) {
        log.info("신고 처리: reportId={}", reportId);
        request.setReportId(reportId);
        ModerationActionResponse response = reportService.processReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/reports/target-member/{targetMemberId}/count
     * 회원별 신고 건수 조회
     */
    @GetMapping("/target-member/{targetMemberId}/count")
    public ResponseEntity<Map<String, Object>> getReportCountByTargetMember(
            @PathVariable Long targetMemberId) {
        log.info("회원별 신고 건수 조회: targetMemberId={}", targetMemberId);
        long count = reportService.getReportCountByTargetMember(targetMemberId);

        Map<String, Object> response = new HashMap<>();
        response.put("targetMemberId", targetMemberId);
        response.put("reportCount", count);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/reports/target-job/{targetJobId}/count
     * 채용공고별 신고 건수 조회
     */
    @GetMapping("/target-job/{targetJobId}/count")
    public ResponseEntity<Map<String, Object>> getReportCountByTargetJob(
            @PathVariable Long targetJobId) {
        log.info("채용공고별 신고 건수 조회: targetJobId={}", targetJobId);
        long count = reportService.getReportCountByTargetJob(targetJobId);

        Map<String, Object> response = new HashMap<>();
        response.put("targetJobId", targetJobId);
        response.put("reportCount", count);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/reports/member/{memberId}/penalty-points
     * 회원 경고 점수 조회
     */
    @GetMapping("/member/{memberId}/penalty-points")
    public ResponseEntity<Map<String, Object>> getMemberPenaltyPoints(
            @PathVariable Long memberId) {
        log.info("회원 경고 점수 조회: memberId={}", memberId);
        Integer totalPoints = reportService.getMemberPenaltyPoints(memberId);

        Map<String, Object> response = new HashMap<>();
        response.put("memberId", memberId);
        response.put("totalPenaltyPoints", totalPoints);
        response.put("isBanned", totalPoints >= 100);  // 100점 이상 시 계정 정지

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/reports/member/{memberId}/penalty-history
     * 회원 경고 이력 조회
     */
    @GetMapping("/member/{memberId}/penalty-history")
    public ResponseEntity<List<MemberPenaltyPointResponse>> getMemberPenaltyHistory(
            @PathVariable Long memberId) {
        log.info("회원 경고 이력 조회: memberId={}", memberId);
        List<MemberPenaltyPointResponse> response = reportService.getMemberPenaltyHistory(memberId);
        return ResponseEntity.ok(response);
    }
}