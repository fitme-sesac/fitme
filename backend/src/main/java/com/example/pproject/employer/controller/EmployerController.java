package com.example.pproject.employer.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import com.example.pproject.employer.dto.*;
import com.example.pproject.employer.service.EmployerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/employer")
@RequiredArgsConstructor
public class EmployerController {

    private final EmployerService employerService;
    private final UserRepository userRepository;

    /** JWT principal에서 회원 PK(member_id) 조회. id 클레임 우선, 없으면 userid로 DB 조회 (OAuth 등 userid null 대응) */
    private Long resolveMemberId(JwtUserPrincipal principal) {
        if (principal == null) return null;
        if (principal.getId() != null) return principal.getId();
        if (principal.getUserid() != null && !principal.getUserid().isBlank()) {
            return userRepository.findByUserid(principal.getUserid())
                    .map(UserEntity::getId)
                    .orElse(null);
        }
        return null;
    }

    /**
     * 기업 대시보드 조회
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            EmployerDashboardDTO dashboard = employerService.getDashboard(principal.getUserid());
            return ResponseEntity.ok(dashboard);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 기업 프로필 조회
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            EmployerProfileDTO profile = employerService.getProfile(principal.getUserid());
            return ResponseEntity.ok(profile);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 기업 프로필 등록/수정
     */
    @PostMapping("/profile")
    public ResponseEntity<?> saveProfile(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody EmployerProfileDTO dto) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            EmployerProfileDTO saved = employerService.saveProfile(principal.getUserid(), dto);
            return ResponseEntity.ok(saved);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===== 광고 통계 API =====

    /**
     * 광고 통계 조회
     */
    @GetMapping("/ad-stats")
    public ResponseEntity<?> getAdStats(@AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            AdStatsDTO stats = employerService.getAdStats(principal.getUserid());
            return ResponseEntity.ok(stats);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===== 지원자 관리 API =====

    /**
     * 지원자 목록 조회 (memberId 기반으로 기업 소속 조회 → OAuth/일반 로그인 모두 대응)
     */
    @GetMapping("/applicants")
    public ResponseEntity<?> getApplicants(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(required = false) String status) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        Long memberId = resolveMemberId(principal);
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "회원 정보를 찾을 수 없습니다."));
        }
        try {
            ApplicantListDTO applicants = employerService.getApplicants(memberId, status);
            return ResponseEntity.ok(applicants);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 지원자 상태 변경
     */
    @PatchMapping("/applicants/{applicationId}/status")
    public ResponseEntity<?> updateApplicantStatus(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long applicationId,
            @RequestBody Map<String, String> body) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            String newStatus = body.get("status");
            employerService.updateApplicantStatus(principal.getUserid(), applicationId, newStatus);
            return ResponseEntity.ok(Map.of("message", "상태가 변경되었습니다."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===== 면접 일정 API =====

    /**
     * 면접 일정 목록 조회
     */
    @GetMapping("/interviews")
    public ResponseEntity<?> getInterviews(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            InterviewListDTO interviews = employerService.getInterviews(principal.getUserid(), year, month);
            return ResponseEntity.ok(interviews);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 면접 일정 등록
     */
    @PostMapping("/interviews")
    public ResponseEntity<?> createInterview(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody InterviewDTO dto) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            log.info("면접 일정 등록 요청: {}", dto);
            InterviewDTO created = employerService.createInterview(principal.getUserid(), dto);
            return ResponseEntity.ok(created);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 면접 일정 수정
     */
    @PutMapping("/interviews/{interviewId}")
    public ResponseEntity<?> updateInterview(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long interviewId,
            @RequestBody InterviewDTO dto) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            InterviewDTO updated = employerService.updateInterview(principal.getUserid(), interviewId, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 면접 일정 삭제
     */
    @DeleteMapping("/interviews/{interviewId}")
    public ResponseEntity<?> deleteInterview(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long interviewId) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            employerService.deleteInterview(principal.getUserid(), interviewId);
            return ResponseEntity.ok(Map.of("message", "면접 일정이 삭제되었습니다."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
