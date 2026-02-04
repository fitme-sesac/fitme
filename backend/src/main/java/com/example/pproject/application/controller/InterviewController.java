package com.example.pproject.application.controller;

import com.example.pproject.application.dto.InterviewCreateRequest;
import com.example.pproject.application.dto.InterviewDTO;
import com.example.pproject.application.dto.InterviewRespondRequest;
import com.example.pproject.application.service.InterviewService;
import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 면접 일정 관리 Controller
 */
@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final UserRepository userRepository;

    /** JWT principal에서 회원 PK(member_id) 조회. id 클레임이 있으면 사용, 없으면 userid로 DB 조회 (구직자 캘린더 등 500 방지) */
    private Long resolveMemberId(JwtUserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (principal.getId() != null) {
            return principal.getId();
        }
        return userRepository.findByUserid(principal.getUserid())
                .map(UserEntity::getId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
    }

    /**
     * 면접 일정 생성 (기업용)
     */
    @PostMapping
    public ResponseEntity<Long> createInterview(
            @Valid @RequestBody InterviewCreateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long interviewId = interviewService.createInterview(request, resolveMemberId(principal));
        return ResponseEntity.ok(interviewId);
    }

    /**
     * 면접 응답 (지원자용) - 수락/거절/일정변경요청
     */
    @PostMapping("/{interviewId}/respond")
    public ResponseEntity<Void> respondToInterview(
            @PathVariable Long interviewId,
            @Valid @RequestBody InterviewRespondRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        interviewService.respondToInterview(interviewId, request, resolveMemberId(principal));
        return ResponseEntity.ok().build();
    }

    /**
     * 면접 취소
     */
    @PutMapping("/{interviewId}/cancel")
    public ResponseEntity<Void> cancelInterview(
            @PathVariable Long interviewId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        interviewService.cancelInterview(interviewId, resolveMemberId(principal));
        return ResponseEntity.ok().build();
    }

    /**
     * 면접 완료 처리 (기업용)
     */
    @PutMapping("/{interviewId}/complete")
    public ResponseEntity<Void> completeInterview(
            @PathVariable Long interviewId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        interviewService.completeInterview(interviewId, resolveMemberId(principal));
        return ResponseEntity.ok().build();
    }

    /**
     * 내 면접 일정 조회 (지원자용)
     */
    @GetMapping("/me")
    public ResponseEntity<List<InterviewDTO>> getMyInterviews(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = resolveMemberId(principal);
        List<InterviewDTO> interviews = interviewService.getMyInterviews(memberId);
        return ResponseEntity.ok(interviews);
    }

    /**
     * 다가오는 면접 조회 (지원자용)
     */
    @GetMapping("/me/upcoming")
    public ResponseEntity<List<InterviewDTO>> getUpcomingInterviews(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = resolveMemberId(principal);
        List<InterviewDTO> interviews = interviewService.getUpcomingInterviews(memberId);
        return ResponseEntity.ok(interviews);
    }

    /**
     * 기업 면접 일정 조회 (기업용)
     */
    @GetMapping("/employer")
    public ResponseEntity<List<InterviewDTO>> getEmployerInterviews(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        List<InterviewDTO> interviews = interviewService.getEmployerInterviews(resolveMemberId(principal));
        return ResponseEntity.ok(interviews);
    }

    /**
     * 기업의 다가오는 면접 조회 (기업용)
     */
    @GetMapping("/employer/upcoming")
    public ResponseEntity<List<InterviewDTO>> getEmployerUpcomingInterviews(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        List<InterviewDTO> interviews = interviewService.getEmployerUpcomingInterviews(resolveMemberId(principal));
        return ResponseEntity.ok(interviews);
    }

    /**
     * 특정 지원의 면접 일정 조회
     */
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<InterviewDTO>> getInterviewsByApplication(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        List<InterviewDTO> interviews = interviewService.getInterviewsByApplication(applicationId, resolveMemberId(principal));
        return ResponseEntity.ok(interviews);
    }

    /**
     * 면접 상세 조회
     */
    @GetMapping("/{interviewId}")
    public ResponseEntity<InterviewDTO> getInterview(
            @PathVariable Long interviewId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        InterviewDTO interview = interviewService.getInterview(interviewId, resolveMemberId(principal));
        return ResponseEntity.ok(interview);
    }
}
