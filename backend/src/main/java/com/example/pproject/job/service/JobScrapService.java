package com.example.pproject.job.service;

import com.example.pproject.job.dto.JobDTO;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.entity.JobScrap;
import com.example.pproject.job.repository.JobRepository;
import com.example.pproject.job.repository.JobScrapRepository;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.common.util.ArrayStringUtil;
import com.example.pproject.common.util.JobPositionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobScrapService {

    private final JobScrapRepository jobScrapRepository;
    private final JobRepository jobRepository;
    private final EmployerRepository employerRepository;

    /**
     * 스크랩 토글 (추가/삭제)
     * @return true: 스크랩 추가됨, false: 스크랩 삭제됨
     */
    @Transactional
    public boolean toggleScrap(Long jobId, Long memberId) {
        // 공고 존재 여부 확인
        JobEntity job = jobRepository.findByIdAndNotDeleted(jobId)
                .orElseThrow(() -> new IllegalArgumentException("채용공고를 찾을 수 없습니다."));

        // 이미 스크랩한 경우 삭제
        if (jobScrapRepository.existsByMemberIdAndJobId(memberId, jobId)) {
            jobScrapRepository.deleteByMemberIdAndJobId(memberId, jobId);
            log.info("스크랩 삭제: memberId={}, jobId={}", memberId, jobId);
            return false;
        }

        // 스크랩 추가
        JobScrap scrap = JobScrap.builder()
                .memberId(memberId)
                .job(job)
                .build();
        jobScrapRepository.save(scrap);
        log.info("스크랩 추가: memberId={}, jobId={}", memberId, jobId);
        return true;
    }

    /**
     * 스크랩 여부 확인
     */
    public boolean isScraped(Long jobId, Long memberId) {
        return jobScrapRepository.existsByMemberIdAndJobId(memberId, jobId);
    }

    /**
     * 내 스크랩 목록 조회 (페이징)
     */
    public Page<JobDTO> getMyScrapList(Long memberId, int page, int size) {
        Page<JobScrap> scrapPage = jobScrapRepository.findActiveScrapsByMemberId(
                memberId, PageRequest.of(page, size));

        return scrapPage.map(scrap -> toJobDTO(scrap.getJob()));
    }

    /**
     * 내 스크랩 목록 조회 (전체)
     */
    public List<JobDTO> getMyScrapList(Long memberId) {
        List<JobScrap> scraps = jobScrapRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        return scraps.stream()
                .filter(s -> s.getJob().getDeletedAt() == null)
                .map(s -> toJobDTO(s.getJob()))
                .collect(Collectors.toList());
    }

    /**
     * 스크랩 수 조회
     */
    public long getScrapCount(Long memberId) {
        return jobScrapRepository.countByMemberId(memberId);
    }

    /**
     * 특정 공고의 스크랩 수 조회
     */
    public long getJobScrapCount(Long jobId) {
        return jobScrapRepository.countByJobId(jobId);
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
                .position(JobPositionUtil.derivePosition(job.getStack()))
                .viewCount(job.getViewCount() != null ? job.getViewCount() : 0)
                .applicationCount(job.getApplicationCount() != null ? job.getApplicationCount() : 0)
                .createdAt(job.getCreatedAt() != null ? job.getCreatedAt().toString() : null)
                .updatedAt(job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : null)
                .companyName(employer != null ? employer.getName() : "알 수 없음")
                .companyLogoUrl(employer != null ? employer.getLogoUrl() : null)
                .build();
    }
}
