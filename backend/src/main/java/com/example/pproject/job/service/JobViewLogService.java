package com.example.pproject.job.service;

import com.example.pproject.job.dto.JobDTO;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.entity.JobViewLog;
import com.example.pproject.job.repository.JobRepository;
import com.example.pproject.job.repository.JobViewLogRepository;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.common.util.ArrayStringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobViewLogService {

    private final JobViewLogRepository jobViewLogRepository;
    private final JobRepository jobRepository;
    private final EmployerRepository employerRepository;

    // 중복 로그 방지를 위한 시간 간격 (분)
    private static final int DUPLICATE_LOG_INTERVAL_MINUTES = 30;

    /**
     * 열람 로그 저장
     * - 비회원도 로그 저장 가능 (memberId = null)
     * - 중복 로그 방지 (30분 이내 같은 공고 조회는 무시)
     */
    @Transactional
    public void logView(Long jobId, Long memberId) {
        JobEntity job = jobRepository.findByIdAndNotDeleted(jobId).orElse(null);
        if (job == null) {
            return; // 존재하지 않는 공고는 무시
        }

        // 회원인 경우 중복 로그 방지
        if (memberId != null) {
            LocalDateTime since = LocalDateTime.now().minusMinutes(DUPLICATE_LOG_INTERVAL_MINUTES);
            if (jobViewLogRepository.existsRecentView(memberId, jobId, since)) {
                log.debug("중복 열람 로그 무시: memberId={}, jobId={}", memberId, jobId);
                return;
            }
        }

        JobViewLog viewLog = JobViewLog.builder()
                .memberId(memberId)
                .job(job)
                .build();
        jobViewLogRepository.save(viewLog);
        log.debug("열람 로그 저장: memberId={}, jobId={}", memberId, jobId);
    }

    /**
     * 최근 본 공고 목록 조회 (중복 제거)
     */
    public List<JobDTO> getRecentViewedJobs(Long memberId, int limit) {
        List<JobViewLog> viewLogs = jobViewLogRepository.findRecentViewedJobs(
                memberId, PageRequest.of(0, limit));

        return viewLogs.stream()
                .map(log -> toJobDTO(log.getJob()))
                .collect(Collectors.toList());
    }

    /**
     * 회원의 열람 기록 수 조회
     */
    public long getViewCount(Long memberId) {
        return jobViewLogRepository.findByMemberIdOrderByViewedAtDesc(memberId).size();
    }

    // ===== Helper Methods =====

    private JobDTO toJobDTO(JobEntity job) {
        EmployerEntity employer = employerRepository.findById(job.getEmployerId())
                .orElse(null);

        return JobDTO.builder()
                .jobId(job.getId())
                .jobUid(String.valueOf(job.getId()))
                .title(job.getTitle())
                .description(job.getDescription())
                .summary(job.getSummary())
                .status(job.getStatus())
                .location(job.getLocation())
                .salaryText(job.getSalaryText())
                .stack(ArrayStringUtil.listToString(job.getStack()))
                .viewCount(job.getViewCount() != null ? job.getViewCount() : 0)
                .applicationCount(job.getApplicationCount() != null ? job.getApplicationCount() : 0)
                .createdAt(job.getCreatedAt() != null ? job.getCreatedAt().toString() : null)
                .updatedAt(job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : null)
                .companyName(employer != null ? employer.getName() : "알 수 없음")
                .companyLogoUrl(employer != null ? employer.getLogoUrl() : null)
                .build();
    }
}
