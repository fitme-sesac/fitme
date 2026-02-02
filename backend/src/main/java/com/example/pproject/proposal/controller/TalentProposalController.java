package com.example.pproject.proposal.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.proposal.dto.ProposalCreateRequest;
import com.example.pproject.proposal.dto.ProposalRespondRequest;
import com.example.pproject.proposal.dto.ProposalResponse;
import com.example.pproject.proposal.service.TalentProposalService;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 인재 포지션 제안 API
 */
@Slf4j
@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class TalentProposalController {

    private final TalentProposalService proposalService;
    private final UserRepository userRepository;

    /** JWT principal에서 회원 ID(member_id) 조회 */
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

    // ==================== 기업용 API ====================

    /**
     * 인재에게 제안 보내기 (기업용)
     */
    @PostMapping
    public ResponseEntity<ProposalResponse> createProposal(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody ProposalCreateRequest request) {

        Long memberId = resolveMemberId(principal);
        log.info("[제안] 제안 생성 - 기업회원: {}, 대상: {}", memberId, request.getCandidateId());
        ProposalResponse response = proposalService.createProposal(memberId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 보낸 제안 목록 조회 (기업용)
     */
    @GetMapping("/sent")
    public ResponseEntity<Page<ProposalResponse>> getSentProposals(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ProposalResponse> proposals = proposalService.getSentProposals(resolveMemberId(principal), pageable);
        return ResponseEntity.ok(proposals);
    }

    /**
     * 제안 취소 (기업용)
     */
    @DeleteMapping("/{proposalId}")
    public ResponseEntity<Map<String, String>> cancelProposal(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long proposalId) {

        Long memberId = resolveMemberId(principal);
        log.info("[제안] 제안 취소 - 기업회원: {}, 제안: {}", memberId, proposalId);
        proposalService.cancelProposal(proposalId, memberId);
        return ResponseEntity.ok(Map.of("message", "제안이 취소되었습니다."));
    }

    // ==================== 구직자용 API ====================

    /**
     * 받은 제안 목록 조회 (구직자용)
     */
    @GetMapping("/received")
    public ResponseEntity<Page<ProposalResponse>> getReceivedProposals(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ProposalResponse> proposals = proposalService.getReceivedProposals(resolveMemberId(principal), pageable);
        return ResponseEntity.ok(proposals);
    }

    /**
     * 제안 상세 조회 (구직자용 - 조회 시 VIEWED 처리)
     */
    @GetMapping("/{proposalId}")
    public ResponseEntity<ProposalResponse> getProposal(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long proposalId) {

        ProposalResponse response = proposalService.getProposalForCandidate(proposalId, resolveMemberId(principal));
        return ResponseEntity.ok(response);
    }

    /**
     * 제안에 응답 (수락/거절) (구직자용)
     */
    @PostMapping("/{proposalId}/respond")
    public ResponseEntity<ProposalResponse> respondToProposal(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long proposalId,
            @Valid @RequestBody ProposalRespondRequest request) {

        Long memberId = resolveMemberId(principal);
        log.info("[제안] 제안 응답 - 구직자: {}, 제안: {}, 수락: {}", memberId, proposalId, request.getAccept());
        ProposalResponse response = proposalService.respondToProposal(proposalId, memberId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 미확인 제안 수 조회 (구직자용)
     */
    @GetMapping("/pending-count")
    public ResponseEntity<Map<String, Long>> getPendingCount(
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        long count = proposalService.countPendingProposals(resolveMemberId(principal));
        return ResponseEntity.ok(Map.of("count", count));
    }
}
