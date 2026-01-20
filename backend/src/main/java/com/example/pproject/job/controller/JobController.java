package com.example.pproject.job.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.job.dto.*;
import com.example.pproject.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    /**
     * 채용공고 목록 조회 (기업 본인 것만)
     */
    @GetMapping
    public ResponseEntity<?> getJobs(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            JobListResponseDTO response = jobService.getJobsByEmployer(principal.getUserid(), page, size);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 채용공고 상세 조회
     */
    @GetMapping("/{jobUid}")
    public ResponseEntity<?> getJob(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable String jobUid) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            JobDTO job = jobService.getJob(principal.getUserid(), jobUid);
            return ResponseEntity.ok(job);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 채용공고 등록
     */
    @PostMapping
    public ResponseEntity<?> createJob(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody JobCreateDTO dto) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            JobDTO created = jobService.createJob(principal.getUserid(), dto);
            return ResponseEntity.ok(created);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 채용공고 수정
     */
    @PutMapping("/{jobUid}")
    public ResponseEntity<?> updateJob(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable String jobUid,
            @RequestBody JobUpdateDTO dto) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            JobDTO updated = jobService.updateJob(principal.getUserid(), jobUid, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 채용공고 삭제
     */
    @DeleteMapping("/{jobUid}")
    public ResponseEntity<?> deleteJob(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable String jobUid) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            jobService.deleteJob(principal.getUserid(), jobUid);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
