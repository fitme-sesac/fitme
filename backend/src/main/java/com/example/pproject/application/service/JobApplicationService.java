package com.example.pproject.application.service;

import com.example.pproject.Constant.ApplicationStatus;
import com.example.pproject.application.dto.JobApplicationRequest;
import com.example.pproject.application.dto.JobApplicationResponse;
import com.example.pproject.application.entity.JobApplication;
import com.example.pproject.application.repository.JobApplicationRepository;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobRepository;
import com.example.pproject.outbox.producer.OutboxEventProducer;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.repository.ResumeRepository;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final EmployerRepository employerRepository;
    private final EmployerMemberRepository employerMemberRepository;
    private final OutboxEventProducer outboxEventProducer;

    @Transactional
    public Long apply(JobApplicationRequest request, Integer userId) {
        if (jobApplicationRepository.existsByJobIdAndMemberId(request.getJobId(), userId)) {
            throw new IllegalArgumentException("이미 지원한 공고입니다.");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        JobEntity job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));

        Resume resume = resumeRepository.findById(request.getResumeId())
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));

        if (!resume.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 이력서로만 지원할 수 있습니다.");
        }

        JobApplication application = JobApplication.builder()
                .job(job)
                .member(user)
                .resume(resume)
                .answers(request.getAnswers())
                .build();

        job.setApplicationCount(job.getApplicationCount() + 1);
        jobRepository.save(job);

        JobApplication savedApplication = jobApplicationRepository.save(application);

        // 기업 정보 조회
        EmployerEntity employer = employerRepository.findById(job.getEmployerId())
                .orElse(null);
        String companyName = employer != null ? employer.getName() : "기업";

        // 1. 지원자에게 지원 완료 알림
        try {
            outboxEventProducer.publishApplicationSubmittedEvent(
                    savedApplication.getId(),
                    userId,
                    job.getTitle(),
                    companyName
            );
            log.info("지원 완료 알림 이벤트 발행: applicationId={}, userId={}", savedApplication.getId(), userId);
        } catch (Exception e) {
            log.error("지원 완료 알림 이벤트 발행 실패: {}", e.getMessage());
        }

        // 2. 기업 담당자에게 새 지원자 알림
        try {
            List<EmployerMemberEntity> employerMembers = employerMemberRepository
                    .findByEmployerIdAndActiveTrue(job.getEmployerId());

            for (EmployerMemberEntity em : employerMembers) {
                outboxEventProducer.publishNewApplicationReceivedEvent(
                        savedApplication.getId(),
                        em.getMemberId().intValue(),
                        job.getId(),
                        job.getTitle(),
                        user.getUsername()
                );
            }
            log.info("새 지원자 알림 이벤트 발행: applicationId={}, 담당자 수={}", 
                    savedApplication.getId(), employerMembers.size());
        } catch (Exception e) {
            log.error("새 지원자 알림 이벤트 발행 실패: {}", e.getMessage());
        }

        return savedApplication.getId();
    }

    @Transactional
    public void cancel(Long applicationId, Integer userId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        if (!application.getMember().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 지원 내역만 취소할 수 있습니다.");
        }

        if (application.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new IllegalStateException("이미 전형이 진행 중이거나 종료된 지원은 취소할 수 없습니다.");
        }

        application.cancel();

        JobEntity job = application.getJob();
        if (job.getApplicationCount() > 0) {
            job.setApplicationCount(job.getApplicationCount() - 1);
        }
    }

    public List<JobApplicationResponse> getMyApplications(Integer userId) {
        return jobApplicationRepository.findByMemberIdOrderByAppliedAtDesc(userId).stream()
                .map(JobApplicationResponse::from)
                .collect(Collectors.toList());
    }
}