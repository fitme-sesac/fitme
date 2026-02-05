package com.example.pproject.resume.controller;

import com.example.pproject.job.dto.JobListResponseDTO;
import com.example.pproject.resume.dto.JobRecommendationDTO;
import com.example.pproject.resume.service.ResumeMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 이력서 기반 채용공고 추천 및 매칭 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resume/match")
@RequiredArgsConstructor
public class ResumeMatchController {

    private final ResumeMatchService resumeMatchService;

    /**
     * [AI 추천] 사용자의 대표 이력서 기반 채용공고 추천 (Best 10)
     * - 벡터 유사도 순으로 가장 적합한 공고 상위 10개를 즉시 반환합니다.
     * 
     * @param memberId 지원자 회원 ID
     * @param limit    반환받을 공고 수 (기본 10, 최대 50)
     * @param location 지역 필터 (선택 사항)
     * @param skills   우선 기술 필터 (선택 사항)
     */
    @GetMapping("/recommend")
    public ResponseEntity<List<JobRecommendationDTO>> recommendJobs(
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) List<String> skills) {

        int safeLimit = Math.min(limit, 50);
        List<JobRecommendationDTO> result = resumeMatchService.recommendJobs(memberId, safeLimit, location, skills);
        log.info("Job recommendation request. MemberId: {}, Returned {} jobs", memberId, result.size());
        return ResponseEntity.ok(result);
    }

    /**
     * [매칭 검색] 필터링이 포함된 채용공고 매칭 검색 (매칭률 순 정렬)
     * - 키워드, 기술스택, 경력 등 다중 필터를 적용하면서
     * - AI 매칭률이 높은 순서대로 결과를 반환합니다.
     * 
     * @param memberId 지원자 회원 ID (매칭 계산의 기준)
     */
    @GetMapping("/jobs")
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
            @RequestParam Long memberId) {

        try {
            log.info("Matching job search request. MemberId: {}, Keyword: {}, Stack: {}", memberId, keyword, stack);
            JobListResponseDTO response = resumeMatchService.getPublicJobsWithMatch(
                    page, size, keyword, stack, location, minExperience, maxExperience, position, industry, memberId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in matching job search", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }
}
