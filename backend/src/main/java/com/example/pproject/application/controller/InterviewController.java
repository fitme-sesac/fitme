package com.example.pproject.application.controller;

import com.example.pproject.application.dto.InterviewCreateRequest;
import com.example.pproject.application.dto.InterviewDTO;
import com.example.pproject.application.dto.InterviewRespondRequest;
import com.example.pproject.application.service.InterviewService;
import com.example.pproject.Config.JwtUserPrincipal;
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

    /**
     * 면접 일정 생성 (기업용)
     */
    @PostMapping
    public ResponseEntity<Long> createInterview(
            @Valid @RequestBody InterviewCreateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long interviewId = interviewService.createInterview(request, principal.getId());
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
        interviewService.respondToInterview(interviewId, request, principal.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * 면접 취소
     */
    @PutMapping("/{interviewId}/cancel")
    public ResponseEntity<Void> cancelInterview(
            @PathVariable Long interviewId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        interviewService.cancelInterview(interviewId, principal.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * 면접 완료 처리 (기업용)
     */
    @PutMapping("/{interviewId}/complete")
    public ResponseEntity<Void> completeInterview(
            @PathVariable Long interviewId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        interviewService.completeInterview(interviewId, principal.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * 내 면접 일정 조회 (지원자용)
     */
    @GetMapping("/me")
    public ResponseEntity<List<InterviewDTO>> getMyInterviews(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        List<InterviewDTO> interviews = interviewService.getMyInterviews(principal.getId());
        return ResponseEntity.ok(interviews);
    }

    /**
     * 다가오는 면접 조회 (지원자용)
     */
    @GetMapping("/me/upcoming")
    public ResponseEntity<List<InterviewDTO>> getUpcomingInterviews(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        List<InterviewDTO> interviews = interviewService.getUpcomingInterviews(principal.getId());
        return ResponseEntity.ok(interviews);
    }

    /**
     * 기업 면접 일정 조회 (기업용)
     */
    @GetMapping("/employer")
    public ResponseEntity<List<InterviewDTO>> getEmployerInterviews(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        List<InterviewDTO> interviews = interviewService.getEmployerInterviews(principal.getId());
        return ResponseEntity.ok(interviews);
    }

    /**
     * 기업의 다가오는 면접 조회 (기업용)
     */
    @GetMapping("/employer/upcoming")
    public ResponseEntity<List<InterviewDTO>> getEmployerUpcomingInterviews(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        List<InterviewDTO> interviews = interviewService.getEmployerUpcomingInterviews(principal.getId());
        return ResponseEntity.ok(interviews);
    }

    /**
     * 특정 지원의 면접 일정 조회
     */
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<InterviewDTO>> getInterviewsByApplication(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        List<InterviewDTO> interviews = interviewService.getInterviewsByApplication(applicationId, principal.getId());
        return ResponseEntity.ok(interviews);
    }

    /**
     * 면접 상세 조회
     */
    @GetMapping("/{interviewId}")
    public ResponseEntity<InterviewDTO> getInterview(
            @PathVariable Long interviewId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        InterviewDTO interview = interviewService.getInterview(interviewId, principal.getId());
        return ResponseEntity.ok(interview);
    }
}
