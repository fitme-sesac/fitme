package com.example.pproject.job.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.common.service.FileUploadService;
import com.example.pproject.job.dto.*;
import com.example.pproject.job.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final FileUploadService fileUploadService;

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

    /**
     * 채용공고 이미지 업로드 (다중 파일)
     * @param files 업로드할 이미지 파일들
     * @return 업로드된 이미지 URL 목록
     */
    @PostMapping("/upload-images")
    public ResponseEntity<?> uploadImages(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam("files") List<MultipartFile> files) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "업로드할 파일이 없습니다."));
        }

        try {
            List<String> uploadedUrls = new ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String url = fileUploadService.uploadFile(file, "jobs");
                    uploadedUrls.add(url);
                    log.info("채용공고 이미지 업로드 완료: {}", url);
                }
            }
            return ResponseEntity.ok(Map.of(
                    "message", "이미지 업로드 완료",
                    "urls", uploadedUrls
            ));
        } catch (IllegalArgumentException e) {
            log.warn("이미지 업로드 실패 (잘못된 형식): {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("이미지 업로드 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "이미지 업로드 실패: " + e.getMessage()));
        }
    }
}
