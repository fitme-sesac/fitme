package com.example.pproject.application.service;

import com.example.pproject.Constant.ApplicationStatus;
import com.example.pproject.application.dto.JobApplicationRequest;
import com.example.pproject.application.dto.JobApplicationResponse;
import com.example.pproject.application.entity.JobApplication;
import com.example.pproject.application.repository.JobApplicationRepository;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobRepository;
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
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long apply(JobApplicationRequest request, Long userId) {
        Long userIdLong = userId.longValue();
        
        if (jobApplicationRepository.existsByJobIdAndMemberId(request.getJobId(), userIdLong)) {
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

        return jobApplicationRepository.save(application).getId();
    }

    @Transactional
    public void cancel(Long applicationId, Integer userId) {
        cancel(applicationId, userId != null ? userId.longValue() : null);
    }

    @Transactional
    public void cancel(Long applicationId, Long memberId) {
        if (memberId == null) throw new IllegalArgumentException("로그인이 필요합니다.");
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

    public List<JobApplicationResponse> getMyApplications(Integer userId) {
        return getMyApplications(userId != null ? userId.longValue() : null);
    }

    public List<JobApplicationResponse> getMyApplications(Long memberId) {
        if (memberId == null) return List.of();
        return jobApplicationRepository.findByMemberIdOrderByAppliedAtDesc(memberId).stream()
                .map(JobApplicationResponse::from)
                .collect(Collectors.toList());
    }

    public JobApplicationResponse getApplication(Long applicationId, Long memberId) {
        JobApplication app = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("지원 내역을 찾을 수 없습니다."));
        if (!app.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 지원 내역만 조회할 수 있습니다.");
        }
        return JobApplicationResponse.from(app);
    }

    /**
     * 지원 상태별 건수 (CANCELED 제외)
     * 프론트: { submitted, viewed, interview, hired, rejected }
     */
    public java.util.Map<String, Long> getMyApplicationCounts(Long memberId) {
        List<JobApplication> list = jobApplicationRepository.findByMemberIdOrderByAppliedAtDesc(memberId);
        long submitted = 0, viewed = 0, interview = 0, hired = 0, rejected = 0;
        for (JobApplication a : list) {
            if (a.getStatus() == ApplicationStatus.CANCELED) continue;
            switch (a.getStatus()) {
                case SUBMITTED -> submitted++;
                case VIEWED -> viewed++;
                case INTERVIEW -> interview++;
                case HIRED -> hired++;
                case REJECTED -> rejected++;
                default -> {}
            }
        }
        return java.util.Map.of(
                "submitted", submitted,
                "viewed", viewed,
                "interview", interview,
                "hired", hired,
                "rejected", rejected
        );
    }
}