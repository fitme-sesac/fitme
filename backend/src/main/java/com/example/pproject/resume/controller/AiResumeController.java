package com.example.pproject.resume.controller;

import com.example.pproject.Constant.SummaryStatus;
import com.example.pproject.global.response.ApiResponse;
import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.resume.dto.AiResumeResponse;
import com.example.pproject.resume.service.AiResumeService;
import com.example.pproject.resume.service.ResumeSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
public class AiResumeController {

    private final AiResumeService aiResumeService;
    private final ResumeSummaryService resumeSummaryService;

    /**
     * 이력서 AI 첨삭 및 요약 요청 (기본)
     * - 프론트엔드에서 "이력서 ID"와 "요약 타입"을 보내면,
     * - 백엔드가 이력서 데이터를 조회해서 AiResumeRequest로 변환 후 Python 서버로 전송
     */
    @PostMapping("/resumes/analyze")
    public ApiResponse<String> analyzeResume(
            @RequestBody AiResumeAnalyzeRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long userId = getUserId(principal);

        // 비동기 요청 시작
        CompletableFuture<AiResumeResponse> future = aiResumeService.requestAnalysis(
                request.getResumeId(),
                request.getSummaryType(),
                userId
        );

        // 비동기이므로 바로 응답
        return ApiResponse.success(
                "AI 분석 요청이 성공적으로 접수되었습니다. 완료 시 알림을 보내드립니다.",
                request.getResumeId().toString()
        );
    }

    /**
     * 채용공고 맞춤 이력서 AI 첨삭 요청
     * - 특정 채용공고에 맞춰 이력서를 어떻게 수정하면 좋을지 AI가 분석
     */
    @PostMapping("/resumes/analyze/job-match")
    public ApiResponse<String> analyzeResumeForJob(
            @RequestBody AiResumeJobMatchRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long userId = getUserId(principal);

        // 비동기 요청 시작
        CompletableFuture<AiResumeResponse> future = aiResumeService.requestAnalysisWithJob(
                request.getResumeId(),
                request.getSummaryType(),
                request.getJobId(),
                userId
        );

        log.info("채용공고 맞춤 AI 분석 요청 - resumeId: {}, jobId: {}, userId: {}",
                request.getResumeId(), request.getJobId(), userId);

        return ApiResponse.success(
                "채용공고 맞춤 AI 분석 요청이 접수되었습니다. 완료 시 알림을 보내드립니다.",
                request.getResumeId().toString());
    }

    /**
     * [NEW] 이력서 요약 생성 요청 (Stateless Worker 사용)
     * POST /api/v1/ai/resumes/{resumeId}/summarize
     */
    @PostMapping("/resumes/{resumeId}/summarize")
    public ApiResponse<SummaryRequestResponse> requestSummary(
            @PathVariable Long resumeId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long userId = getUserId(principal);

        // 비동기 처리 시작 (fire-and-forget)
        resumeSummaryService.requestSummary(resumeId, userId);

        log.info("[요약 요청] resumeId: {}, userId: {}", resumeId, userId);

        return ApiResponse.success(
                "AI 요약 요청이 접수되었습니다.",
                new SummaryRequestResponse(resumeId, "PENDING", "요약이 진행 중입니다. 잠시 후 확인해주세요."));
    }

    /**
     * [NEW] 요약 상태 조회
     * GET /api/v1/ai/resumes/{resumeId}/summary-status
     */
    @GetMapping("/resumes/{resumeId}/summary-status")
    public ApiResponse<SummaryStatusResponse> getSummaryStatus(
            @PathVariable Long resumeId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long userId = getUserId(principal);
        SummaryStatus status = resumeSummaryService.getStatus(resumeId, userId);

        return ApiResponse.success(
                "조회 성공",
                new SummaryStatusResponse(resumeId, status.name(), getStatusMessage(status)));
    }

    /**
     * AI 분석 상태 조회 (기존)
     */
    @GetMapping("/resumes/{resumeId}/status")
    public ApiResponse<AiAnalysisStatusResponse> getAnalysisStatus(
            @PathVariable Long resumeId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long userId = getUserId(principal);
        SummaryStatus status = aiResumeService.getAnalysisStatus(resumeId, userId);

        AiAnalysisStatusResponse response = new AiAnalysisStatusResponse(
                resumeId,
                status.name(),
                getStatusMessage(status)
        );

        return ApiResponse.success("조회 성공", response);
    }

    // ========== Request/Response DTOs ==========

    @lombok.Data
    public static class AiResumeAnalyzeRequest {
        private Long resumeId;
        private String summaryType; // "brief", "detailed", "professional"
    }

    @lombok.Data
    public static class AiResumeJobMatchRequest {
        private Long resumeId;
        private Long jobId; // 맞춤 첨삭할 채용공고 ID
        private String summaryType;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AiAnalysisStatusResponse {
        private Long resumeId;
        private String status;
        private String message;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SummaryRequestResponse {
        private Long resumeId;
        private String status;
        private String message;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SummaryStatusResponse {
        private Long resumeId;
        private String status;
        private String message;
    }

    // ========== Helper Methods ==========

    private Long getUserId(JwtUserPrincipal principal) {
        if (principal == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }
        // JwtUserPrincipal에서 userId 추출 (구현에 따라 다를 수 있음)
        return Long.valueOf(principal.getUserid());
    }

    private String getStatusMessage(SummaryStatus status) {
        return switch (status) {
            case NONE -> "AI 분석을 요청하지 않았습니다.";
            case PENDING -> "AI 분석/요약 요청이 대기 중입니다.";
            case PROCESSING -> "AI가 이력서를 분석/요약하고 있습니다.";
            case COMPLETED -> "AI 작업이 완료되었습니다.";
            case FAILED -> "AI 작업이 실패했습니다. 다시 시도해주세요.";
        };
    }
}
