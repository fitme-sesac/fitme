package com.example.pproject.job.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.application.dto.JobApplicationRequest;
import com.example.pproject.application.service.JobApplicationService;
import com.example.pproject.job.dto.*;
import com.example.pproject.job.service.JobScrapService;
import com.example.pproject.job.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobScrapService jobScrapService;
    private final JobApplicationService jobApplicationService;

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
            log.warn("채용공고 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("채용공고 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
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
            log.warn("채용공고 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("채용공고 조회 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
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
        
        log.info("채용공고 등록 요청 - 사용자: {}, 제목: {}", principal.getUserid(), dto.getTitle());
        
        try {
            JobDTO created = jobService.createJob(principal.getUserid(), dto);
            log.info("채용공고 등록 성공 - ID: {}", created.getJobId());
            return ResponseEntity.ok(created);
        } catch (IllegalStateException e) {
            log.warn("채용공고 등록 실패 (IllegalState): {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("채용공고 등록 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
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
            log.warn("채용공고 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("채용공고 수정 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
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
            log.warn("채용공고 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("채용공고 삭제 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }

    // ----- 스크랩 / 지원 (fitme-2 프론트 연동, /api/jobs prefix) -----

    @GetMapping("/scrapped")
    public ResponseEntity<?> getScrapped(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            List<JobDTO> list = jobScrapService.getMyScrapList(principal.getId());
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            log.error("스크랩 목록 조회 중 오류", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{jobId}/scrap")
    public ResponseEntity<?> getScrapStatus(
            @PathVariable Long jobId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        boolean scraped = jobScrapService.isScraped(jobId, principal.getId());
        return ResponseEntity.ok(Map.of("scrapped", scraped));
    }

    @PostMapping("/{jobId}/scrap")
    public ResponseEntity<?> addScrap(
            @PathVariable Long jobId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            jobScrapService.addScrap(jobId, principal.getId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{jobId}/scrap")
    public ResponseEntity<?> deleteScrap(
            @PathVariable Long jobId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            jobScrapService.deleteScrap(jobId, principal.getId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{jobId}/apply")
    public ResponseEntity<?> apply(
            @PathVariable Long jobId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        Object rId = body != null ? body.get("resumeId") : null;
        if (rId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "resumeId는 필수입니다."));
        }
        Long resumeId = rId instanceof Number ? ((Number) rId).longValue() : Long.parseLong(rId.toString());
        String answers = null;
        if (body != null && body.get("answers") != null) {
            Object a = body.get("answers");
            answers = a instanceof String ? (String) a : (a != null ? a.toString() : null);
        }
        JobApplicationRequest req = new JobApplicationRequest();
        req.setJobId(jobId);
        req.setResumeId(resumeId);
        req.setAnswers(answers);
        try {
            Long applicationId = jobApplicationService.apply(req, principal.getId());
            return ResponseEntity.ok(applicationId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
