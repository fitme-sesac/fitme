package com.example.pproject.job.controller;

import com.example.pproject.job.dto.JobDTO;
import com.example.pproject.job.dto.JobListResponseDTO;
import com.example.pproject.job.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 공개 채용공고 API (인증 없이 접근 가능)
 * - 일반 사용자(구직자)가 채용공고를 열람하는 용도
 * - /api/public/jobs/* 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/api/public/jobs")
@RequiredArgsConstructor
public class PublicJobController {

    private final JobService jobService;

    /**
     * 공개 채용공고 목록 조회
     * - status='OPEN'인 공고만 반환
     * - 검색, 필터링 지원
     */
    @GetMapping
    public ResponseEntity<?> getPublicJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stack,
            @RequestParam(required = false) String location) {
        try {
            log.info("공개 채용공고 조회 - page: {}, size: {}, keyword: {}, stack: {}, location: {}",
                    page, size, keyword, stack, location);
            
            JobListResponseDTO response = jobService.getPublicJobs(page, size, keyword, stack, location);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("공개 채용공고 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }

    /**
     * 공개 채용공고 상세 조회
     * - 조회수 자동 증가
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<?> getPublicJob(@PathVariable Long jobId) {
        try {
            log.info("공개 채용공고 상세 조회 - jobId: {}", jobId);
            
            JobDTO job = jobService.getPublicJob(jobId);
            return ResponseEntity.ok(job);
        } catch (IllegalStateException e) {
            log.warn("공개 채용공고 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("공개 채용공고 상세 조회 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }
}
