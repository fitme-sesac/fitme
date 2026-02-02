package com.example.pproject.job.controller;

import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 관리자용 채용공고 관리 API
 */
@RestController
@RequestMapping("/api/v1/admin/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SERVICEADMIN')")
@Slf4j
public class AdminJobController {

    private final JobEntityRepository jobEntityRepository;

    /**
     * GET /api/v1/admin/jobs
     * 채용공고 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<Page<Map<String, Object>>> getJobs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        log.info("관리자 채용공고 목록 조회: status={}, search={}", status, search);

        Page<JobEntity> jobs;
        if (search != null && !search.isEmpty()) {
            jobs = jobEntityRepository.searchPublicJobs(search, pageable);
        } else {
            jobs = jobEntityRepository.findAll(pageable);
        }

        Page<Map<String, Object>> result = jobs.map(j -> {
            Map<String, Object> map = new HashMap<>();
            map.put("jobId", j.getId());
            map.put("title", j.getTitle());
            map.put("employerId", j.getEmployerId());
            map.put("status", j.getStatus());
            map.put("viewCount", j.getViewCount());
            map.put("createdAt", j.getCreatedAt());
            map.put("location", j.getLocation());
            return map;
        });

        return ResponseEntity.ok(result);
    }

    /**
     * PATCH /api/v1/admin/jobs/{jobId}/status
     * 채용공고 상태 변경 (승인/반려/삭제)
     */
    @PatchMapping("/{jobId}/status")
    public ResponseEntity<Map<String, Object>> updateJobStatus(
            @PathVariable Long jobId,
            @RequestBody Map<String, String> request) {
        String newStatus = request.get("status");
        log.info("채용공고 상태 변경: jobId={}, status={}", jobId, newStatus);

        JobEntity job = jobEntityRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("채용공고를 찾을 수 없습니다: " + jobId));

        job.setStatus(newStatus);
        jobEntityRepository.save(job);

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("status", newStatus);
        response.put("message", "채용공고 상태가 변경되었습니다.");

        return ResponseEntity.ok(response);
    }
}
