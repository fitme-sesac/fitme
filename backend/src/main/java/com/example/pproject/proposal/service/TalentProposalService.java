package com.example.pproject.proposal.service;

import com.example.pproject.Constant.ProposalStatus;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobEntityRepository;
import com.example.pproject.notification.service.NotificationService;
import com.example.pproject.proposal.dto.ProposalCreateRequest;
import com.example.pproject.proposal.dto.ProposalRespondRequest;
import com.example.pproject.proposal.dto.ProposalResponse;
import com.example.pproject.proposal.entity.TalentProposal;
import com.example.pproject.proposal.repository.TalentProposalRepository;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TalentProposalService {

    private final TalentProposalRepository proposalRepository;
    private final EmployerRepository employerRepository;
    private final EmployerMemberRepository employerMemberRepository;
    private final UserRepository userRepository;
    private final JobEntityRepository jobEntityRepository;
    private final NotificationService notificationService;

    /**
     * 기업이 인재에게 제안 보내기
     */
    @Transactional
    public ProposalResponse createProposal(Long memberId, ProposalCreateRequest request) {
        // 1. 기업 회원 확인
        var employerMember = employerMemberRepository.findFirstByMemberIdAndActiveTrue(memberId)
                .orElseThrow(() -> new IllegalArgumentException("기업 회원 정보를 찾을 수 없습니다."));

        EmployerEntity employer = employerRepository.findById(employerMember.getEmployerId())
                .orElseThrow(() -> new IllegalArgumentException("기업 정보를 찾을 수 없습니다."));

        // 2. 대상 구직자 확인
        UserEntity candidate = userRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new IllegalArgumentException("대상 인재를 찾을 수 없습니다."));

        // 3. 공고 확인 (선택)
        JobEntity job = null;
        if (request.getJobId() != null) {
            job = jobEntityRepository.findByIdAndNotDeleted(request.getJobId())
                    .orElseThrow(() -> new IllegalArgumentException("공고를 찾을 수 없습니다."));

            // 해당 공고가 이 기업의 공고인지 확인
            if (!job.getEmployerId().equals(employer.getId())) {
                throw new IllegalArgumentException("해당 공고에 대한 권한이 없습니다.");
            }
        }

        // 4. 중복 제안 확인
        var existingProposal = proposalRepository.findByEmployerIdAndCandidateIdAndJobId(
                employer.getId(), candidate.getId(), request.getJobId());
        if (existingProposal.isPresent()) {
            ProposalStatus status = existingProposal.get().getStatus();
            if (status == ProposalStatus.PENDING || status == ProposalStatus.VIEWED) {
                throw new IllegalArgumentException("이미 진행 중인 제안이 있습니다.");
            }
        }

        // 5. 제안 생성
        int expirationDays = request.getExpirationDays() != null ? request.getExpirationDays() : 14;
        TalentProposal proposal = TalentProposal.builder()
                .employer(employer)
                .candidate(candidate)
                .job(job)
                .title(request.getTitle())
                .message(request.getMessage())
                .offeredSalary(request.getOfferedSalary())
                .offeredPosition(request.getOfferedPosition())
                .expiresAt(LocalDateTime.now().plusDays(expirationDays))
                .build();

        TalentProposal saved = proposalRepository.save(proposal);

        // 6. 알림 발송
        try {
            notificationService.sendNotification(
                    candidate.getId(),
                    "PROPOSAL_RECEIVED",
                    Map.of(
                            "employerName", employer.getName(),
                            "proposalTitle", request.getTitle(),
                            "proposalId", saved.getId().toString()
                    )
            );
        } catch (Exception e) {
            log.warn("알림 발송 실패: {}", e.getMessage());
        }

        return ProposalResponse.from(saved);
    }

    /**
     * 구직자가 받은 제안 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<ProposalResponse> getReceivedProposals(Long candidateId, Pageable pageable) {
        return proposalRepository.findByCandidateId(candidateId, pageable)
                .map(ProposalResponse::from);
    }

    /**
     * 기업이 보낸 제안 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<ProposalResponse> getSentProposals(Long memberId, Pageable pageable) {
        var employerMember = employerMemberRepository.findFirstByMemberIdAndActiveTrue(memberId)
                .orElseThrow(() -> new IllegalArgumentException("기업 회원 정보를 찾을 수 없습니다."));

        return proposalRepository.findByEmployerId(employerMember.getEmployerId(), pageable)
                .map(ProposalResponse::from);
    }

    /**
     * 제안 상세 조회 (구직자용 - 조회 시 VIEWED 처리)
     */
    @Transactional
    public ProposalResponse getProposalForCandidate(Long proposalId, Long candidateId) {
        TalentProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("제안을 찾을 수 없습니다."));

        if (!proposal.getCandidate().getId().equals(candidateId)) {
            throw new IllegalArgumentException("해당 제안에 대한 권한이 없습니다.");
        }

        // PENDING -> VIEWED 전환
        proposal.markAsViewed();

        return ProposalResponse.from(proposal);
    }

    /**
     * 구직자가 제안에 응답 (수락/거절)
     */
    @Transactional
    public ProposalResponse respondToProposal(Long proposalId, Long candidateId, ProposalRespondRequest request) {
        TalentProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("제안을 찾을 수 없습니다."));

        if (!proposal.getCandidate().getId().equals(candidateId)) {
            throw new IllegalArgumentException("해당 제안에 대한 권한이 없습니다.");
        }

        if (proposal.getStatus() == ProposalStatus.EXPIRED || proposal.isExpired()) {
            throw new IllegalArgumentException("만료된 제안입니다.");
        }

        if (proposal.getStatus() == ProposalStatus.ACCEPTED || proposal.getStatus() == ProposalStatus.REJECTED) {
            throw new IllegalArgumentException("이미 응답한 제안입니다.");
        }

        if (Boolean.TRUE.equals(request.getAccept())) {
            proposal.accept(request.getMessage());

            // 기업에게 알림
            try {
                // employer의 member를 찾아서 알림 전송
                var employerMembers = employerMemberRepository.findByEmployerIdAndActiveTrue(proposal.getEmployer().getId());
                for (var em : employerMembers) {
                    notificationService.sendNotification(
                            em.getMemberId(),
                            "PROPOSAL_ACCEPTED",
                            Map.of(
                                    "candidateName", proposal.getCandidate().getUsername(),
                                    "proposalTitle", proposal.getTitle(),
                                    "proposalId", proposal.getId().toString()
                            )
                    );
                }
            } catch (Exception e) {
                log.warn("알림 발송 실패: {}", e.getMessage());
            }
        } else {
            proposal.reject(request.getMessage());
        }

        return ProposalResponse.from(proposal);
    }

    /**
     * 구직자의 미확인 제안 수
     */
    @Transactional(readOnly = true)
    public long countPendingProposals(Long candidateId) {
        return proposalRepository.countPendingByCandidateId(candidateId);
    }

    /**
     * 기업이 제안 취소
     */
    @Transactional
    public void cancelProposal(Long proposalId, Long memberId) {
        TalentProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("제안을 찾을 수 없습니다."));

        var employerMember = employerMemberRepository.findFirstByMemberIdAndActiveTrue(memberId)
                .orElseThrow(() -> new IllegalArgumentException("기업 회원 정보를 찾을 수 없습니다."));

        if (!proposal.getEmployer().getId().equals(employerMember.getEmployerId())) {
            throw new IllegalArgumentException("해당 제안에 대한 권한이 없습니다.");
        }

        if (proposal.getStatus() == ProposalStatus.ACCEPTED) {
            throw new IllegalArgumentException("이미 수락된 제안은 취소할 수 없습니다.");
        }

        proposal.cancel();
    }
}
