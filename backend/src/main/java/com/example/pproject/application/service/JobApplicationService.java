package com.example.pproject.application.service;

import com.example.pproject.Constant.ApplicationStatus;
import com.example.pproject.application.dto.JobApplicationRequest;
import com.example.pproject.application.dto.JobApplicationResponse;
import com.example.pproject.application.entity.JobApplication;
import com.example.pproject.application.repository.JobApplicationRepository;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobEntityRepository;
import com.example.pproject.outbox.producer.OutboxEventProducer;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.repository.ResumeRepository;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobEntityRepository jobEntityRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final OutboxEventProducer outboxEventProducer;
    private final EmployerRepository employerRepository;

    @Transactional
    public Long apply(JobApplicationRequest request, Long userId) {
        Long userIdLong = userId.longValue();
        
        if (jobApplicationRepository.existsByJobIdAndMemberId(request.getJobId(), userIdLong)) {
            throw new IllegalArgumentException("이미 지원한 공고입니다.");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        JobEntity job = jobEntityRepository.findById(request.getJobId())
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
        jobEntityRepository.save(job);

        Long applicationId = jobApplicationRepository.save(application).getId();

        // 지원 완료 알림 발행 (Outbox → Consumer에서 구직자/기업 알림 생성)
        String companyName = employerRepository.findById(job.getEmployerId())
                .map(EmployerEntity::getName)
                .orElse("");
        outboxEventProducer.publishApplicationSubmittedEvent(
                applicationId, job.getId(), userId.intValue(),
                job.getTitle(), companyName);

        return applicationId;
    }

    @Transactional
    public void cancel(Long applicationId, Long memberId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        if (!application.getMember().getId().equals(memberId)) {
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

    public List<JobApplicationResponse> getMyApplications(Long memberId) {
        return jobApplicationRepository.findByMemberIdOrderByAppliedAtDesc(memberId).stream()
                .map(JobApplicationResponse::from)
                .collect(Collectors.toList());
    }
}