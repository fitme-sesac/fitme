package com.example.pproject.report.controller;

import com.example.pproject.Config.JwtUserPrincipal;
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

    /**
     * [U-REP-001] 회원: 신고 접수 (최종 수정 버전)
     * POST /api/v1/reports
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CANDIDATE', 'EMPLOYER')") // 일반 회원만 가능
    public ResponseEntity<ReportResponse> createReport(
            @Valid @RequestBody MemberReportCreateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) { // JWT 토큰에서 인증된 사용자 정보 추출

        Long reporterId = principal.getId();
        log.info("신고 접수: reporterId={}, targetType={}, targetId={}",
                reporterId, request.getTargetType(), request.getTargetId());

        // target_id를 타입에 따라 분배 로직
        Long targetJobId = null;
        Long targetMemberId = null;

        if ("JOB_POSTING".equals(request.getTargetType())) {
            targetJobId = request.getTargetId();
        } else if ("MEMBER".equals(request.getTargetType())) {
            targetMemberId = request.getTargetId();
        } else {
            throw new IllegalArgumentException("지원하지 않는 신고 대상 타입입니다: " + request.getTargetType());
        }

        // Service용 DTO로 변환
        CreateReportRequest serviceRequest = CreateReportRequest.builder()
                .reporterMemberId(reporterId) // JWT에서 추출한 로그인 회원 ID
                .targetType(request.getTargetType())
                .targetJobId(targetJobId) // 분배된 ID
                .targetMemberId(targetMemberId) // 분배된 ID
                .reasonCode(request.getReasonCode())
                .reasonDetail(request.getDescription()) // description -> reasonDetail
                .build();

        ReportResponse response = reportService.createReport(serviceRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/reports/{reportId}
     * 신고 상세 조회 (신고자 본인 또는 관리자만 조회 가능)
     */
    @GetMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'EMPLOYER', 'SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<ReportResponse> getReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        log.info("신고 조회: reportId={}, requesterId={}", reportId, principal.getId());

        ReportResponse response = reportService.getReport(reportId);

        // 관리자가 아닌 경우, 본인의 신고만 조회 가능
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority() != null && (auth.getAuthority().equals("ROLE_SERVICEADMIN")
                        || auth.getAuthority().equals("ROLE_APPROVEADMIN")
                        || auth.getAuthority().equals("ROLE_MASTER")));

        if (!isAdmin && !response.getReporterMemberId().equals(principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("본인의 신고만 조회할 수 있습니다.");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/reports/reporter/{reporterMemberId}
     * 신고자별 신고 목록 (본인 또는 관리자만 조회 가능)
     */
    @GetMapping("/reporter/{reporterMemberId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'EMPLOYER', 'SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Page<ReportResponse>> getReportsByReporter(
            @PathVariable Long reporterMemberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        // 관리자가 아닌 경우, 본인의 신고만 조회 가능
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority() != null && (auth.getAuthority().equals("ROLE_SERVICEADMIN")
                        || auth.getAuthority().equals("ROLE_APPROVEADMIN")
                        || auth.getAuthority().equals("ROLE_MASTER")));

        if (!isAdmin && !reporterMemberId.equals(principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("본인의 신고 목록만 조회할 수 있습니다.");
        }

        log.info("신고자별 신고 목록 조회: reporterMemberId={}, requesterId={}", reporterMemberId, principal.getId());
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportResponse> response = reportService.getReportsByReporter(reporterMemberId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/reports/status/{status}
     * 상태별 신고 목록 (관리자용)
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
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
     * 신고 대상 타입별 조회 (관리자용)
     */
    @GetMapping("/by-target-type/{targetType}")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
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
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<ModerationActionResponse> processReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ProcessReportRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        try {
            log.info("=== 신고 처리 요청 시작 ===");
            log.info("reportId={}, adminId={}, decision={}, violationType={}", 
                    reportId, principal.getId(), request.getDecision(), request.getViolationType());
            log.info("요청 데이터: {}", request);

            // 경로 변수의 ID를 DTO에 설정하여 일치시킴
            request.setReportId(reportId);
            request.setAdminMemberId(principal.getId()); // 관리자 ID도 JWT에서 추출

            ModerationActionResponse response = reportService.processReport(request);
            
            log.info("=== 신고 처리 완료 ===");
            log.info("응답 데이터: {}", response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("=== 신고 처리 중 오류 발생 ===");
            log.error("reportId={}, error={}", reportId, e.getMessage(), e);
            
            // 구체적인 에러 응답 반환
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("code", "500");
            errorResponse.put("message", "신고 처리 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            errorResponse.put("reportId", reportId);
            
            log.error("에러 응답: {}", errorResponse);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ModerationActionResponse.builder()
                            .actionId(-1L)
                            .reportId(reportId)
                            .decision("ERROR")
                            .reason("처리 중 오류 발생: " + e.getMessage())
                            .build());
        }
    }

    /**
     * GET /api/v1/reports/target-member/{targetMemberId}/count
     * 회원별 신고 건수 조회 (관리자용)
     */
    @GetMapping("/target-member/{targetMemberId}/count")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
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
     * 채용공고별 신고 건수 조회 (관리자용)
     */
    @GetMapping("/target-job/{targetJobId}/count")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
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
     * 회원 경고 점수 및 상태 조회 (관리자용)
     */
    @GetMapping("/member/{memberId}/penalty-points")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
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
     * 회원 경고 이력 조회 (관리자용)
     */
    @GetMapping("/member/{memberId}/penalty-history")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<List<MemberPenaltyPointResponse>> getMemberPenaltyHistory(
            @PathVariable Long memberId) {
        log.info("회원 경고 이력 조회: memberId={}", memberId);
        List<MemberPenaltyPointResponse> response = reportService.getMemberPenaltyHistory(memberId);
        return ResponseEntity.ok(response);
    }
}