package com.example.pproject.proposal.service;

import com.example.pproject.Constant.ApplicationStatus;
import com.example.pproject.Constant.ProposalStatus;
import com.example.pproject.application.entity.JobApplication;
import com.example.pproject.application.repository.JobApplicationRepository;
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
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.repository.ResumeRepository;
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
    private final JobApplicationRepository jobApplicationRepository;
    private final ResumeRepository resumeRepository;

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

            // 제안에 공고가 연결되어 있으면 지원서(JobApplication) 자동 생성 -> 면접 대기자로 추가
            if (proposal.getJob() != null) {
                createJobApplicationFromProposal(proposal);
            }

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
                                    "proposalId", proposal.getId().toString(),
                                    "jobTitle", proposal.getJob() != null ? proposal.getJob().getTitle() : ""
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

    /**
     * 제안 수락 시 지원서(JobApplication) 자동 생성
     * - 인재가 제안을 수락하면 면접 대기자로 자동 등록됨
     * - 기업에서 바로 면접 일정을 잡을 수 있음
     */
    private void createJobApplicationFromProposal(TalentProposal proposal) {
        try {
            JobEntity job = proposal.getJob();
            UserEntity candidate = proposal.getCandidate();

            // 이미 해당 공고에 지원한 이력이 있는지 확인
            if (jobApplicationRepository.existsByJobIdAndMemberId(job.getId(), candidate.getId())) {
                log.info("이미 지원한 공고입니다. 중복 지원서 생성 생략: jobId={}, candidateId={}",
                        job.getId(), candidate.getId());
                return;
            }

            // 구직자의 대표 이력서 또는 최근 이력서 조회
            Resume resume = resumeRepository.findPrimaryOrLatest(candidate.getId())
                    .orElse(null);

            if (resume == null) {
                log.warn("구직자의 이력서가 없어 지원서 생성 생략: candidateId={}", candidate.getId());
                return;
            }

            // 지원서 생성 (제안을 통한 지원이므로 INTERVIEW 상태로 바로 설정)
            JobApplication application = JobApplication.builder()
                    .job(job)
                    .member(candidate)
                    .resume(resume)
                    .proposalId(proposal.getId())
                    .build();

            // 제안 수락을 통한 지원은 바로 면접 대기 상태로 설정
            application.updateStatus(ApplicationStatus.INTERVIEW);

            JobApplication saved = jobApplicationRepository.save(application);

            log.info("제안 수락으로 지원서 자동 생성: applicationId={}, proposalId={}, jobId={}, candidateId={}",
                    saved.getId(), proposal.getId(), job.getId(), candidate.getId());

        } catch (Exception e) {
            log.error("제안 수락 시 지원서 생성 실패: proposalId={}, error={}", proposal.getId(), e.getMessage());
            // 지원서 생성 실패해도 제안 수락은 유지 (트랜잭션 롤백하지 않음)
        }
    }
}
