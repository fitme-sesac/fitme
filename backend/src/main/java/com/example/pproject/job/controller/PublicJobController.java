package com.example.pproject.job.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.job.dto.JobDTO;
import com.example.pproject.job.dto.JobFilterOptionsDTO;
import com.example.pproject.job.dto.JobListResponseDTO;
import com.example.pproject.job.dto.JobMatchInfoDTO;
import com.example.pproject.job.service.JobService;
import com.example.pproject.job.service.JobViewLogService;
import com.example.pproject.resume.service.ResumeSkillService;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 공개 채용공고 API (인증 없이 접근 가능)
 * - 일반 사용자(구직자)가 채용공고를 열람하는 용도
 * - /api/public/jobs/* 엔드포인트
 * - 로그인한 사용자의 경우 기술 스택 매칭 정보 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/public/jobs")
@RequiredArgsConstructor
public class PublicJobController {

    private final JobService jobService;
    private final JobViewLogService jobViewLogService;
    private final ResumeSkillService resumeSkillService;
    private final UserRepository userRepository;

    /**
     * 필터 옵션 조회
     * - 사용 가능한 기술 스택 목록
     * - 사용 가능한 지역 목록 (시/도 단위)
     */
    @GetMapping("/filter-options")
    public ResponseEntity<?> getFilterOptions() {
        try {
            log.info("필터 옵션 조회 요청");
            JobFilterOptionsDTO options = jobService.getFilterOptions();
            return ResponseEntity.ok(options);
        } catch (Exception e) {
            log.error("필터 옵션 조회 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }

    /**
     * 공개 채용공고 목록 조회
     * - status='OPEN'인 공고만 반환
     * - 다중 필터 지원 (keyword + stack + location + experience)
     * - 로그인 사용자의 경우 기술 스택 매칭 정보 포함
     */
    @GetMapping
    public ResponseEntity<?> getPublicJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stack,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) Integer maxExperience,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        try {
            Long memberId = getMemberIdFromPrincipal(principal);
            log.info("공개 채용공고 조회 - page: {}, size: {}, keyword: {}, stack: {}, location: {}, experience: {}-{}, memberId: {}",
                    page, size, keyword, stack, location, minExperience, maxExperience, memberId);

            JobListResponseDTO response;
            if (memberId != null) {
                // 로그인 사용자: 매칭 정보 포함 시도
                try {
                    response = jobService.getPublicJobsWithMatch(page, size, keyword, stack, location, memberId);
                } catch (Exception matchError) {
                    // 매칭 계산 실패 시 기본 조회로 폴백
                    log.warn("매칭 정보 계산 실패, 기본 조회로 전환: {}", matchError.getMessage());
                    response = jobService.getPublicJobs(page, size, keyword, stack, location, minExperience, maxExperience);
                }
            } else {
                // 비로그인 사용자: 기본 조회 (다중 필터 적용)
                response = jobService.getPublicJobs(page, size, keyword, stack, location, minExperience, maxExperience);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("공개 채용공고 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }

    /**
     * 최근 본 공고 목록 조회 (로그인 사용자 전용)
     */
    /**
     * 최근 본 공고 목록 조회 (로그인 사용자 전용)
     */
    @GetMapping("/recently-viewed")
    public ResponseEntity<?> getRecentlyViewedJobs(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(defaultValue = "6") int limit) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            Long memberId = getMemberIdFromPrincipal(principal);
            if (memberId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "회원 정보를 찾을 수 없습니다."));
            }

            log.info("최근 본 공고 조회 요청 - memberId: {}, limit: {}", memberId, limit);
            List<JobDTO> recentJobs = jobViewLogService.getRecentViewedJobs(memberId, limit);

            // 최근 본 공고에도 매칭 정보 포함 시도 (선택적)
            try {
                // 지원자의 기술 스택 조회
                Set<String> candidateSkills = resumeSkillService.getSkillsByMemberId(memberId);
                if (candidateSkills != null && !candidateSkills.isEmpty()) {
                    recentJobs.forEach(job -> {
                        try {
                            // 단순 스택 매칭만 계산
                            JobMatchInfoDTO matchInfo = jobService.calculateMatchInfo(
                                    job.getStack(),
                                    candidateSkills,
                                    null,
                                    memberId);
                            job.setMatchInfo(matchInfo);
                        } catch (Exception e) {
                            // 개별 매칭 계산 실패 무시
                        }
                    });
                }
            } catch (Exception e) {
                log.warn("최근 본 공고 매칭 정보 계산 중 오류 (무시됨): {}", e.getMessage());
            }

            return ResponseEntity.ok(recentJobs);
        } catch (Exception e) {
            log.error("최근 본 공고 조회 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }

    /**
     * 공개 채용공고 상세 조회
     * - 조회수 자동 증가
     * - 로그인 사용자의 경우 기술 스택 매칭 정보 포함
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<?> getPublicJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        try {
            Long memberId = getMemberIdFromPrincipal(principal);
            log.info("공개 채용공고 상세 조회 - jobId: {}, memberId: {}", jobId, memberId);

            // 열람 로그 저장 (비동기적으로 처리하여 응답 속도에 영향 없음)
            try {
                jobViewLogService.logView(jobId, memberId);
            } catch (Exception logError) {
                log.warn("열람 로그 저장 실패: {}", logError.getMessage());
            }

            JobDTO job;
            if (memberId != null) {
                // 로그인 사용자: 매칭 정보 포함 시도
                try {
                    job = jobService.getPublicJobWithMatch(jobId, memberId);
                } catch (Exception matchError) {
                    // 매칭 계산 실패 시 기본 조회로 폴백
                    log.warn("매칭 정보 계산 실패, 기본 조회로 전환: {}", matchError.getMessage());
                    job = jobService.getPublicJob(jobId);
                }
            } else {
                // 비로그인 사용자: 기본 조회
                job = jobService.getPublicJob(jobId);
            }
            return ResponseEntity.ok(job);
        } catch (IllegalStateException e) {
            log.warn("공개 채용공고 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("공개 채용공고 상세 조회 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }

    /**
     * JWT Principal에서 memberId 추출
     * - 비로그인 또는 인증 실패 시 null 반환
     * - login_id로 조회 실패 시 email로도 시도 (소셜 로그인 사용자 지원)
     */
    private Long getMemberIdFromPrincipal(JwtUserPrincipal principal) {
        if (principal == null) {
            log.debug("Principal이 null입니다.");
            return null;
        }

        String userid = principal.getUserid();
        String email = principal.getEmail();

        log.debug("JWT Principal - userid: {}, email: {}", userid, email);

        try {
            // 1. login_id로 조회 시도
            if (userid != null && !userid.isBlank()) {
                var userOpt = userRepository.findByUserid(userid);
                if (userOpt.isPresent()) {
                    Long memberId = userOpt.get().getId();
                    log.debug("login_id로 회원 조회 성공 - memberId: {}", memberId);
                    return memberId;
                }
            }

            // 2. login_id로 못 찾으면 email로 시도 (소셜 로그인 사용자)
            if (email != null && !email.isBlank()) {
                var userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    Long memberId = userOpt.get().getId();
                    log.debug("email로 회원 조회 성공 - memberId: {}", memberId);
                    return memberId;
                }
            }

            // 3. userid가 email 형식인 경우에도 시도 (하위 호환)
            if (userid != null && userid.contains("@")) {
                var userOpt = userRepository.findByEmail(userid);
                if (userOpt.isPresent()) {
                    Long memberId = userOpt.get().getId();
                    log.debug("userid(email형식)로 회원 조회 성공 - memberId: {}", memberId);
                    return memberId;
                }
            }

            log.warn("회원을 찾을 수 없습니다 - userid: {}, email: {}", userid, email);
            return null;
        } catch (Exception e) {
            log.warn("memberId 조회 실패: {}", e.getMessage());
            return null;
        }
    }
}
