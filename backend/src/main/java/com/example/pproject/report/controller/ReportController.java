package com.example.pproject.report.controller;

import com.example.pproject.report.dto.request.MemberReportCreateRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    // ❌ [삭제됨] 기존의 createReport (CreateReportRequest 사용) 메서드는 삭제하여 중복 매핑 방지

    /**
     * [U-REP-001] 회원: 신고 접수 (최종 수정 버전)
     * POST /api/v1/reports
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CANDIDATE', 'EMPLOYER')") // 일반 회원만 가능
    public ResponseEntity<ReportResponse> createReport(
            @Valid @RequestBody MemberReportCreateRequest request,
            //@AuthenticationPrincipal Long reporterId) { // 토큰에서 신고자 ID 추출
            @RequestParam Long reporterId) { // 토큰 대신 파라미터로 ID 직접 받기

        log.info("회원 신고 접수 요청: reporterId={}, targetType={}, targetId={}",
                reporterId, request.getTargetType(), request.getTargetId());

        // target_id를 타입에 따라 분배 로직
        Long targetJobId = null;
        Long targetMemberId = null;

        if ("JOB_POSTING".equals(request.getTargetType())) {
            targetJobId = request.getTargetId();
        } else if ("MEMBER".equals(request.getTargetType())) {
            targetMemberId = request.getTargetId();
        }

        // Service용 DTO로 변환
        CreateReportRequest serviceRequest = CreateReportRequest.builder()
                .reporterMemberId(reporterId) // 로그인한 회원 ID
                .targetType(request.getTargetType())
                .targetJobId(targetJobId)       // 분배된 ID
                .targetMemberId(targetMemberId) // 분배된 ID
                .reasonCode(request.getReasonCode())
                .reasonDetail(request.getDescription()) // description -> reasonDetail
                .build();

        ReportResponse response = reportService.createReport(serviceRequest);
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
     * 신고 처리 (중재 조치) - 관리자 전용
     */
    @PostMapping("/{reportId}/process")
    public ResponseEntity<ModerationActionResponse> processReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ProcessReportRequest request) {
        log.info("신고 처리: reportId={}", reportId);

        // 경로 변수의 ID를 DTO에 설정하여 일치시킴
        request.setReportId(reportId);

        // 실제로는 보안 컨텍스트에서 관리자 ID를 가져와야 합니다.
        // request.setAdminMemberId(currentAdminId);

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
     * 회원 경고 점수 및 상태 조회
     */
    @GetMapping("/member/{memberId}/penalty-points")
    public ResponseEntity<Map<String, Object>> getMemberPenaltyPoints(
            @PathVariable Long memberId) {
        log.info("회원 경고 점수 조회: memberId={}", memberId);

        Integer totalPoints = reportService.getMemberPenaltyPoints(memberId);
        boolean isBanned = reportService.isMemberBanned(memberId);

        Map<String, Object> response = new HashMap<>();
        response.put("memberId", memberId);
        response.put("totalPenaltyPoints", totalPoints);
        response.put("isBanned", isBanned);

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