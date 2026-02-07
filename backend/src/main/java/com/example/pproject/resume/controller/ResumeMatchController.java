package com.example.pproject.resume.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.job.dto.JobListResponseDTO;
import com.example.pproject.resume.dto.JobRecommendationDTO;
import com.example.pproject.resume.service.ResumeMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 이력서 기반 채용공고 추천 및 매칭 API (병합본)
 *
 * ✅ 기존 경로 유지:
 *  - GET /api/v1/resume/recommend
 *  - GET /api/v1/resume/recommend/by-resume/{resumeId}
 *
 * ✅ 신규 경로 유지:
 *  - GET /api/v1/resume/match/recommend
 *  - GET /api/v1/resume/match/jobs
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resume")
@RequiredArgsConstructor
public class ResumeMatchController {

    private final ResumeMatchService resumeMatchService;

    // ---------------------------
    // 1) (기존) JWT 기반 추천
    // GET /api/v1/resume/recommend
    // ---------------------------
    @GetMapping("/recommend")
    public ResponseEntity<List<JobRecommendationDTO>> recommendJobs(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) List<String> skills
    ) {
        Long memberId = resolveMemberId(principal, null);

        // 비로그인 사용자는 빈 리스트 반환 (기존 동작 유지)
        if (memberId == null) {
            log.warn("Job recommendation request without authentication (legacy path)");
            return ResponseEntity.ok(Collections.emptyList());
        }

        int safeLimit = clampLimit(limit);

        List<JobRecommendationDTO> result =
                resumeMatchService.recommendJobs(memberId, safeLimit, location, skills);

        log.info("Job recommendation (legacy). MemberId: {}, Returned {} jobs", memberId, result.size());
        return ResponseEntity.ok(result);
    }

    // -----------------------------------------
    // 2) (기존) 특정 이력서 기반 추천
    // GET /api/v1/resume/recommend/by-resume/{resumeId}
    // -----------------------------------------
    @GetMapping("/recommend/by-resume/{resumeId}")
    public ResponseEntity<List<JobRecommendationDTO>> recommendJobsByResume(
            @PathVariable Long resumeId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) List<String> skills
    ) {
        int safeLimit = clampLimit(limit);

        List<JobRecommendationDTO> result =
                resumeMatchService.recommendJobsByResume(resumeId, safeLimit, location, skills);

        log.info("Job recommendation by resume. ResumeId: {}, Returned {} jobs", resumeId, result.size());
        return ResponseEntity.ok(result);
    }

    // -----------------------------------------
    // 3) (신규) memberId 파라미터 기반 추천
    // GET /api/v1/resume/match/recommend
    // - principal 있으면 principal 우선 (호환/보안)
    // - principal 없으면 memberId 사용
    // -----------------------------------------
    @GetMapping("/match/recommend")
    public ResponseEntity<List<JobRecommendationDTO>> recommendJobsMatch(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(required = false) Long memberId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) List<String> skills
    ) {
        Long effectiveMemberId = resolveMemberId(principal, memberId);

        // 둘 다 없으면 빈 리스트 (기존 스타일에 맞춰 200 + empty)
        if (effectiveMemberId == null) {
            log.warn("Job recommendation request without memberId/principal (match path)");
            return ResponseEntity.ok(Collections.emptyList());
        }

        // principal이 있는데 memberId가 다르면 파라미터는 무시(보안/호환)
        if (principal != null && memberId != null && !effectiveMemberId.equals(memberId)) {
            log.warn("memberId param ignored due to principal. param={}, principal={}", memberId, effectiveMemberId);
        }

        int safeLimit = clampLimit(limit);
        List<JobRecommendationDTO> result =
                resumeMatchService.recommendJobs(effectiveMemberId, safeLimit, location, skills);

        log.info("Job recommendation (match). MemberId: {}, Returned {} jobs", effectiveMemberId, result.size());
        return ResponseEntity.ok(result);
    }

    // -----------------------------------------
    // 4) (신규) 매칭률 포함 공고 검색
    // GET /api/v1/resume/match/jobs
    // - principal 있으면 principal 우선
    // - principal 없으면 memberId 필요
    // -----------------------------------------
    @GetMapping("/match/jobs")
    public ResponseEntity<?> getPublicJobsWithMatch(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stack,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) Integer maxExperience,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String industry,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(required = false) Long memberId
    ) {
        Long effectiveMemberId = resolveMemberId(principal, memberId);
        if (effectiveMemberId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
        }

        try {
            log.info("Matching job search request. MemberId: {}, Keyword: {}, Stack: {}",
                    effectiveMemberId, keyword, stack);

            JobListResponseDTO response = resumeMatchService.getPublicJobsWithMatch(
                    page, size, keyword, stack, location,
                    minExperience, maxExperience, position, industry,
                    effectiveMemberId
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in matching job search", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }

    // ---------------------------
    // helpers
    // ---------------------------
    private static int clampLimit(int limit) {
        if (limit <= 0) return 10;
        return Math.min(limit, 50);
    }

    private static Long resolveMemberId(JwtUserPrincipal principal, Long memberIdParam) {
        if (principal != null) {
            try {
                return Long.valueOf(principal.getUserid());
            } catch (Exception ignored) {
                return null;
            }
        }
        return memberIdParam;
    }
}
