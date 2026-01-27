package com.example.pproject.job.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.job.dto.JobDTO;
import com.example.pproject.job.service.JobScrapService;
import com.example.pproject.job.service.JobViewLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 공고 스크랩 및 열람 기록 Controller
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobScrapController {

    private final JobScrapService jobScrapService;
    private final JobViewLogService jobViewLogService;

    /**
     * 스크랩 토글 (추가/삭제)
     */
    @PostMapping("/{jobId}/scrap")
    public ResponseEntity<Map<String, Object>> toggleScrap(
            @PathVariable Long jobId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        boolean isScraped = jobScrapService.toggleScrap(jobId, principal.getId());
        return ResponseEntity.ok(Map.of(
                "scraped", isScraped,
                "message", isScraped ? "스크랩되었습니다." : "스크랩이 취소되었습니다."
        ));
    }

    /**
     * 스크랩 여부 확인
     */
    @GetMapping("/{jobId}/scrap-status")
    public ResponseEntity<Map<String, Boolean>> getScrapStatus(
            @PathVariable Long jobId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        boolean isScraped = jobScrapService.isScraped(jobId, principal.getId());
        return ResponseEntity.ok(Map.of("scraped", isScraped));
    }

    /**
     * 내 스크랩 목록 조회 (페이징)
     */
    @GetMapping("/scraps")
    public ResponseEntity<Page<JobDTO>> getMyScrapList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Page<JobDTO> scraps = jobScrapService.getMyScrapList(principal.getId(), page, size);
        return ResponseEntity.ok(scraps);
    }

    /**
     * 내 스크랩 수 조회
     */
    @GetMapping("/scraps/count")
    public ResponseEntity<Map<String, Long>> getScrapCount(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        long count = jobScrapService.getScrapCount(principal.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 최근 본 공고 목록 조회
     */
    @GetMapping("/recent-views")
    public ResponseEntity<List<JobDTO>> getRecentViewedJobs(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        List<JobDTO> recentJobs = jobViewLogService.getRecentViewedJobs(principal.getId(), limit);
        return ResponseEntity.ok(recentJobs);
    }
}
