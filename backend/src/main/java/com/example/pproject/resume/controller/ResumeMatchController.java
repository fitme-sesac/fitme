package com.example.pproject.resume.controller;

import com.example.pproject.resume.dto.JobRecommendationDTO;
import com.example.pproject.resume.service.ResumeMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 이력서 기반 채용공고 추천 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resume/recommend")
@RequiredArgsConstructor
public class ResumeMatchController {

    private final ResumeMatchService resumeMatchService;

    /**
     * 사용자의 대표 이력서 기반 채용공고 추천
     * 
     * @param memberId 사용자 ID
     * @param limit    추천 개수 (기본값: 10)
     * @param location 지역 필터 (선택)
     * @param skills   스택 필터 (선택, 여러 개 가능)
     */
    @GetMapping
    public ResponseEntity<List<JobRecommendationDTO>> recommendJobs(
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) List<String> skills) {

        int safeLimit = Math.min(limit, 50);

        List<JobRecommendationDTO> result = resumeMatchService.recommendJobs(
                memberId, safeLimit, location, skills);

        log.info("Job recommendation request. MemberId: {}, Returned {} jobs", memberId, result.size());

        return ResponseEntity.ok(result);
    }

    /**
     * 특정 이력서 기반 채용공고 추천
     */
    @GetMapping("/by-resume/{resumeId}")
    public ResponseEntity<List<JobRecommendationDTO>> recommendJobsByResume(
            @PathVariable Long resumeId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) List<String> skills) {

        int safeLimit = Math.min(limit, 50);

        List<JobRecommendationDTO> result = resumeMatchService.recommendJobsByResume(
                resumeId, safeLimit, location, skills);

        log.info("Job recommendation by resume. ResumeId: {}, Returned {} jobs", resumeId, result.size());

        return ResponseEntity.ok(result);
    }
}
