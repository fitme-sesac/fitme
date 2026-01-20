package com.example.pproject.job.service;

import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.dto.*;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobRepository;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobService {

    private final JobRepository jobRepository;
    private final EmployerRepository employerRepository;
    private final EmployerMemberRepository employerMemberRepository;
    private final UserRepository userRepository;

    /**
     * 기업의 채용공고 목록 조회
     */
    public JobListResponseDTO getJobsByEmployer(String userid, int page, int size) {
        EmployerEntity employer = getEmployerByUserid(userid);

        Page<JobEntity> jobPage = jobRepository.findByEmployerIdAndNotDeleted(
                employer.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<JobDTO> jobs = jobPage.getContent().stream()
                .map(job -> toJobDTO(job, employer))
                .collect(Collectors.toList());

        return JobListResponseDTO.builder()
                .jobs(jobs)
                .page(page)
                .size(size)
                .totalElements(jobPage.getTotalElements())
                .totalPages(jobPage.getTotalPages())
                .build();
    }

    /**
     * 채용공고 상세 조회 (jobId 사용)
     */
    public JobDTO getJob(String userid, Long jobId) {
        EmployerEntity employer = getEmployerByUserid(userid);

        JobEntity job = jobRepository.findByIdAndNotDeleted(jobId)
                .orElseThrow(() -> new IllegalStateException("채용공고를 찾을 수 없습니다."));

        // 본인 기업의 채용공고인지 확인
        if (!job.getEmployerId().equals(employer.getId())) {
            throw new IllegalStateException("접근 권한이 없습니다.");
        }

        return toJobDTO(job, employer);
    }

    /**
     * 채용공고 상세 조회 (문자열 ID - 기존 API 호환)
     */
    public JobDTO getJob(String userid, String jobIdStr) {
        try {
            Long jobId = Long.parseLong(jobIdStr);
            return getJob(userid, jobId);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("잘못된 채용공고 ID입니다.");
        }
    }

    /**
     * 채용공고 등록
     */
    @Transactional
    public JobDTO createJob(String userid, JobCreateDTO dto) {
        EmployerEntity employer = getEmployerByUserid(userid);

        // 기업 상태 확인 (ACTIVE만 등록 가능)
        if (!"ACTIVE".equals(employer.getStatus())) {
            throw new IllegalStateException("기업이 활성 상태가 아닙니다.");
        }

        // description에서 자동 요약 생성
        String summary = generateSummary(dto.getDescription());

        JobEntity job = JobEntity.builder()
                .employerId(employer.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .summary(summary)  // 자동 생성된 요약
                .status(dto.getStatus() != null ? dto.getStatus() : "DRAFT")
                .location(dto.getLocation())
                .salaryText(dto.getSalaryText())
                .stack(dto.getStack())
                .requiredQuestions(dto.getRequiredQuestions())
                .build();

        jobRepository.save(job);
        log.info("채용공고 생성: {} (기업: {}, 요약: {})", job.getTitle(), employer.getName(), summary);

        return toJobDTO(job, employer);
    }

    /**
     * 채용공고 수정
     */
    @Transactional
    public JobDTO updateJob(String userid, Long jobId, JobUpdateDTO dto) {
        EmployerEntity employer = getEmployerByUserid(userid);

        JobEntity job = jobRepository.findByIdAndNotDeleted(jobId)
                .orElseThrow(() -> new IllegalStateException("채용공고를 찾을 수 없습니다."));

        // 본인 기업의 채용공고인지 확인
        if (!job.getEmployerId().equals(employer.getId())) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        // 업데이트
        if (dto.getTitle() != null) job.setTitle(dto.getTitle());
        if (dto.getDescription() != null) {
            job.setDescription(dto.getDescription());
            // description이 변경되면 요약도 다시 생성
            job.setSummary(generateSummary(dto.getDescription()));
        }
        if (dto.getStatus() != null) job.setStatus(dto.getStatus());
        if (dto.getLocation() != null) job.setLocation(dto.getLocation());
        if (dto.getSalaryText() != null) job.setSalaryText(dto.getSalaryText());
        if (dto.getStack() != null) job.setStack(dto.getStack());
        if (dto.getRequiredQuestions() != null) job.setRequiredQuestions(dto.getRequiredQuestions());

        jobRepository.save(job);
        log.info("채용공고 수정: {}", job.getTitle());

        return toJobDTO(job, employer);
    }

    /**
     * 채용공고 수정 (문자열 ID - 기존 API 호환)
     */
    @Transactional
    public JobDTO updateJob(String userid, String jobIdStr, JobUpdateDTO dto) {
        try {
            Long jobId = Long.parseLong(jobIdStr);
            return updateJob(userid, jobId, dto);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("잘못된 채용공고 ID입니다.");
        }
    }

    /**
     * 채용공고 삭제 (soft delete)
     */
    @Transactional
    public void deleteJob(String userid, Long jobId) {
        EmployerEntity employer = getEmployerByUserid(userid);

        JobEntity job = jobRepository.findByIdAndNotDeleted(jobId)
                .orElseThrow(() -> new IllegalStateException("채용공고를 찾을 수 없습니다."));

        // 본인 기업의 채용공고인지 확인
        if (!job.getEmployerId().equals(employer.getId())) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        job.setDeletedAt(LocalDateTime.now());
        job.setStatus("CLOSED");
        jobRepository.save(job);
        log.info("채용공고 삭제: {}", job.getTitle());
    }

    /**
     * 채용공고 삭제 (문자열 ID - 기존 API 호환)
     */
    @Transactional
    public void deleteJob(String userid, String jobIdStr) {
        try {
            Long jobId = Long.parseLong(jobIdStr);
            deleteJob(userid, jobId);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("잘못된 채용공고 ID입니다.");
        }
    }

    // === Helper Methods ===

    /**
     * description에서 자동 요약 생성 (첫 1~2문장, 최대 200자)
     */
    private String generateSummary(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        // 줄바꿈으로 문단 분리
        String cleaned = description.trim();
        
        // 첫 번째 문단 또는 첫 몇 문장 추출
        String[] paragraphs = cleaned.split("\\n\\n|\\r\\n\\r\\n");
        String firstParagraph = paragraphs[0].trim();
        
        // 문장 단위로 분리 (마침표, 느낌표, 물음표 기준)
        String[] sentences = firstParagraph.split("(?<=[.!?])\\s+");
        
        StringBuilder summary = new StringBuilder();
        int sentenceCount = 0;
        
        for (String sentence : sentences) {
            if (sentenceCount >= 2 || summary.length() + sentence.length() > 200) {
                break;
            }
            if (summary.length() > 0) {
                summary.append(" ");
            }
            summary.append(sentence.trim());
            sentenceCount++;
        }
        
        String result = summary.toString().trim();
        
        // 200자 초과 시 자르기
        if (result.length() > 200) {
            result = result.substring(0, 197) + "...";
        }
        
        return result.isEmpty() ? null : result;
    }

    /**
     * userid로 소속 기업 조회 (employer_member 통해)
     */
    private EmployerEntity getEmployerByUserid(String userid) {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long memberId = user.getId().longValue();

        // employer_member에서 소속 기업 찾기
        EmployerMemberEntity membership = employerMemberRepository
                .findFirstByMemberIdAndActiveTrue(memberId)
                .orElseThrow(() -> new IllegalStateException("소속된 기업이 없습니다. 먼저 기업 정보를 등록해주세요."));
        
        return employerRepository.findById(membership.getEmployerId())
                .orElseThrow(() -> new IllegalStateException("기업 정보를 찾을 수 없습니다."));
    }

    private JobDTO toJobDTO(JobEntity job, EmployerEntity employer) {
        return JobDTO.builder()
                .jobId(job.getId())
                .jobUid(String.valueOf(job.getId()))
                .title(job.getTitle())
                .description(job.getDescription())
                .summary(job.getSummary())  // 요약 추가
                .status(job.getStatus())
                .location(job.getLocation())
                .salaryText(job.getSalaryText())
                .stack(job.getStack())
                .viewCount(job.getViewCount() != null ? job.getViewCount() : 0)
                .applicationCount(job.getApplicationCount() != null ? job.getApplicationCount() : 0)
                .createdAt(job.getCreatedAt() != null ? job.getCreatedAt().toString() : null)
                .updatedAt(job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : null)
                .companyName(employer.getName())
                .companyLogoUrl(employer.getLogoUrl())
                .build();
    }
}
