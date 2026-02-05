package com.example.pproject.job.controller;

import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.repository.EmployerRepository;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 관리자용 채용공고 관리 API
 */
@RestController
@RequestMapping("/api/v1/admin/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
@Slf4j
public class AdminJobController {

    private final JobEntityRepository jobEntityRepository;
    private final EmployerRepository employerRepository;

    /**
     * GET /api/v1/admin/jobs
     * 채용공고 목록 조회 (페이징, 상태 필터: null=전체, OPEN=승인, CLOSED=마감, DELETED=삭제탭)
     * 전체/승인/마감은 deletedAt IS NULL 만, 삭제탭은 deletedAt IS NOT NULL
     */
    @GetMapping
    public ResponseEntity<Page<Map<String, Object>>> getJobs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        log.info("관리자 채용공고 목록 조회: status={}, search={}", status, search);

        String statusFilter = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : null;
        String keyword = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<JobEntity> jobs;
        if ("DELETED".equals(statusFilter)) {
            // 삭제 탭: soft-deleted 만
            jobs = keyword != null
                    ? jobEntityRepository.findByDeletedAtIsNotNullAndTitleContainingIgnoreCase(keyword, pageable)
                    : jobEntityRepository.findByDeletedAtIsNotNull(pageable);
        } else {
            // 전체/승인/마감: 삭제되지 않은 것만
            if (keyword != null && statusFilter != null) {
                jobs = jobEntityRepository.findByStatusAndDeletedAtIsNullAndTitleContainingIgnoreCase(statusFilter, keyword, pageable);
            } else if (statusFilter != null) {
                jobs = jobEntityRepository.findByStatusAndDeletedAtIsNull(statusFilter, pageable);
            } else if (keyword != null) {
                jobs = jobEntityRepository.findByTitleContainingIgnoreCaseAndDeletedAtIsNull(keyword, pageable);
            } else {
                jobs = jobEntityRepository.findByDeletedAtIsNull(pageable);
            }
        }

        List<Long> employerIds = jobs.getContent().stream().map(JobEntity::getEmployerId).distinct().toList();
        Map<Long, String> employerNames = employerIds.isEmpty() ? Map.of()
                : employerRepository.findAllById(employerIds).stream()
                        .collect(Collectors.toMap(EmployerEntity::getId, EmployerEntity::getName, (a, b) -> a));

        Page<Map<String, Object>> result = jobs.map(j -> {
            Map<String, Object> map = new HashMap<>();
            map.put("jobId", j.getId());
            map.put("title", j.getTitle());
            map.put("employerId", j.getEmployerId());
            map.put("companyName", employerNames.getOrDefault(j.getEmployerId(), ""));
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
     * 채용공고 상태 변경 (승인/마감/삭제). 삭제는 soft delete(deletedAt 설정).
     */
    @PatchMapping("/{jobId}/status")
    public ResponseEntity<Map<String, Object>> updateJobStatus(
            @PathVariable Long jobId,
            @RequestBody Map<String, String> request) {
        String newStatus = request.get("status");
        log.info("채용공고 상태 변경: jobId={}, status={}", jobId, newStatus);

        JobEntity job = jobEntityRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("채용공고를 찾을 수 없습니다: " + jobId));

        if ("DELETED".equalsIgnoreCase(newStatus)) {
            job.setDeletedAt(Instant.now());
        } else {
            job.setDeletedAt(null);
            job.setStatus(newStatus != null ? newStatus.toUpperCase() : job.getStatus());
        }
        jobEntityRepository.save(job);

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("status", "DELETED".equalsIgnoreCase(newStatus) ? "DELETED" : job.getStatus());
        response.put("message", "채용공고 상태가 변경되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/admin/jobs/{jobId}
     * 채용공고 완전 삭제 (이미 soft-deleted 된 공고만 가능)
     */
    @DeleteMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> permanentlyDeleteJob(@PathVariable Long jobId) {
        log.info("채용공고 완전 삭제: jobId={}", jobId);

        JobEntity job = jobEntityRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("채용공고를 찾을 수 없습니다: " + jobId));

        if (job.getDeletedAt() == null) {
            throw new IllegalStateException("삭제 탭에서만 완전 삭제할 수 있습니다. 먼저 삭제 처리하세요.");
        }

        jobEntityRepository.delete(job);

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("message", "채용공고가 완전 삭제되었습니다.");
        return ResponseEntity.ok(response);
    }
}
