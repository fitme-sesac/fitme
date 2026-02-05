package com.example.pproject.job.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.job.dto.JobDTO;
import com.example.pproject.job.service.JobScrapService;
import com.example.pproject.job.service.JobViewLogService;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 공고 스크랩 및 열람 기록 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobScrapController {

    private final JobScrapService jobScrapService;
    private final JobViewLogService jobViewLogService;
    private final UserRepository userRepository;

    /**
     * 스크랩 토글 (추가/삭제)
     */
    @PostMapping("/{jobId}/scrap")
    public ResponseEntity<?> toggleScrap(
            @PathVariable Long jobId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        log.info("=== 스크랩 토글 요청 === jobId={}", jobId);
        try {
            Long memberId = getMemberId(principal);
            log.info("스크랩 토글 - memberId={}, jobId={}", memberId, jobId);
            boolean isScraped = jobScrapService.toggleScrap(jobId, memberId);
            log.info("스크랩 토글 완료 - isScraped={}", isScraped);
            return ResponseEntity.ok(Map.of(
                    "scraped", isScraped,
                    "message", isScraped ? "스크랩되었습니다." : "스크랩이 취소되었습니다."
            ));
        } catch (Exception e) {
            log.error("스크랩 토글 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 스크랩 여부 확인
     */
    @GetMapping("/{jobId}/scrap-status")
    public ResponseEntity<?> getScrapStatus(
            @PathVariable Long jobId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        try {
            Long memberId = getMemberId(principal);
            boolean isScraped = jobScrapService.isScraped(jobId, memberId);
            return ResponseEntity.ok(Map.of("scraped", isScraped));
        } catch (Exception e) {
            log.error("스크랩 상태 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("scraped", false));
        }
    }

    /**
     * 내 스크랩 목록 조회 (페이징)
     */
    @GetMapping("/scraps")
    public ResponseEntity<?> getMyScrapList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        log.info("=== 내 스크랩 목록 조회 ===");
        try {
            Long memberId = getMemberId(principal);
            log.info("스크랩 목록 조회 - memberId={}", memberId);
            Page<JobDTO> scraps = jobScrapService.getMyScrapList(memberId, page, size);
            log.info("스크랩 목록 조회 완료 - count={}", scraps.getTotalElements());
            return ResponseEntity.ok(scraps);
        } catch (Exception e) {
            log.error("스크랩 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 내 스크랩 수 조회
     */
    @GetMapping("/scraps/count")
    public ResponseEntity<Map<String, Long>> getScrapCount(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberId(principal);
        long count = jobScrapService.getScrapCount(memberId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 최근 본 공고 목록 조회
     */
    @GetMapping("/recent-views")
    public ResponseEntity<List<JobDTO>> getRecentViewedJobs(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberId(principal);
        List<JobDTO> recentJobs = jobViewLogService.getRecentViewedJobs(memberId, limit);
        return ResponseEntity.ok(recentJobs);
    }

    /**
     * Principal에서 사용자 DB ID 추출
     */
    private Long getMemberId(JwtUserPrincipal principal) {
        log.info("getMemberId 호출 - principal null? {}", principal == null);
        
        if (principal == null) {
            log.error("principal이 null입니다. 로그인이 필요합니다.");
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        
        // 1. JWT에 ID가 포함된 경우 (정상 케이스)
        Long id = principal.getId();
        String userid = principal.getUserid();
        String email = principal.getEmail();
        
        log.info("Principal 정보 - id={}, userid={}, email={}", id, userid, email);
        
        if (id != null) {
            log.info("JWT에서 ID 추출 성공: {}", id);
            return id;
        }
        
        // 2. ID가 없으면 userid 또는 email로 DB 조회
        log.warn("JWT에 ID가 없음. DB 조회 시도");
        
        // userid로 조회
        if (userid != null && !userid.isBlank()) {
            log.info("userid로 사용자 조회 시도: {}", userid);
            UserEntity user = userRepository.findByUserid(userid).orElse(null);
            if (user != null) {
                log.info("userid로 사용자 조회 성공: id={}", user.getId());
                return user.getId();
            }
            log.warn("userid로 사용자를 찾을 수 없음: {}", userid);
        }
        
        // email로 조회
        if (email != null && !email.isBlank()) {
            log.info("email로 사용자 조회 시도: {}", email);
            UserEntity user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                log.info("email로 사용자 조회 성공: id={}", user.getId());
                return user.getId();
            }
            log.warn("email로 사용자를 찾을 수 없음: {}", email);
        }
        
        log.error("사용자 조회 최종 실패 - userid={}, email={}", userid, email);
        throw new IllegalStateException("사용자 정보를 확인할 수 없습니다. 다시 로그인해주세요.");
    }
}
