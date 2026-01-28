package com.example.pproject.application.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.application.dto.JobApplicationRequest;
import com.example.pproject.application.dto.JobApplicationResponse;
import com.example.pproject.application.service.JobApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 프론트 fitme-2 연동용 지원 API.
 * - prefix: /api/applications (경로 통일)
 * - GET /my, GET /my/counts, GET /:id, POST, POST /:id/cancel
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Tag(name = "Application (Fitme2)", description = "지원 API (프론트 /api/applications 연동)")
public class ApplicationApiController {

    private final JobApplicationService jobApplicationService;

    private static Long requireMemberId(JwtUserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        return principal.getId();
    }

    @Operation(summary = "내 지원 현황 조회")
    @GetMapping("/my")
    public ResponseEntity<List<JobApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = requireMemberId(principal);
        List<JobApplicationResponse> list = jobApplicationService.getMyApplications(memberId);
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "지원 상태별 건수 조회")
    @GetMapping("/my/counts")
    public ResponseEntity<Map<String, Long>> getMyCounts(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = requireMemberId(principal);
        Map<String, Long> counts = jobApplicationService.getMyApplicationCounts(memberId);
        return ResponseEntity.ok(counts);
    }

    @Operation(summary = "지원 상세 조회")
    @GetMapping("/{applicationId}")
    public ResponseEntity<JobApplicationResponse> getApplication(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = requireMemberId(principal);
        JobApplicationResponse res = jobApplicationService.getApplication(applicationId, memberId);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "입사 지원")
    @PostMapping
    public ResponseEntity<Long> apply(
            @RequestBody @Valid JobApplicationRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = requireMemberId(principal);
        Long applicationId = jobApplicationService.apply(request, memberId);
        return ResponseEntity.ok(applicationId);
    }

    @Operation(summary = "지원 취소 (POST)")
    @PostMapping("/{applicationId}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = requireMemberId(principal);
        jobApplicationService.cancel(applicationId, memberId);
        return ResponseEntity.ok().build();
    }
}
