package com.example.pproject.job.service;

import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.dto.*;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobRepository;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobService {

    private final JobRepository jobRepository;
    private final EmployerRepository employerRepository;
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
     * 채용공고 상세 조회
     */
    public JobDTO getJob(String userid, String jobUid) {
        EmployerEntity employer = getEmployerByUserid(userid);

        JobEntity job = jobRepository.findByJobUid(UUID.fromString(jobUid))
                .orElseThrow(() -> new IllegalStateException("채용공고를 찾을 수 없습니다."));

        // 본인 기업의 채용공고인지 확인
        if (!job.getEmployerId().equals(employer.getId())) {
            throw new IllegalStateException("접근 권한이 없습니다.");
        }

        return toJobDTO(job, employer);
    }

    /**
     * 채용공고 등록
     */
    @Transactional
    public JobDTO createJob(String userid, JobCreateDTO dto) {
        EmployerEntity employer = getEmployerByUserid(userid);

        // 기업 승인 상태 확인
        if (!"APPROVED".equals(employer.getStatus())) {
            throw new IllegalStateException("기업 승인이 완료되지 않았습니다.");
        }

        JobEntity job = JobEntity.builder()
                .employerId(employer.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .requirements(dto.getRequirements())
                .preferredQualifications(dto.getPreferredQualifications())
                .jobType(dto.getJobType())
                .experienceLevel(dto.getExperienceLevel())
                .educationLevel(dto.getEducationLevel())
                .salaryMin(dto.getSalaryMin())
                .salaryMax(dto.getSalaryMax())
                .salaryNegotiable(dto.getSalaryNegotiable())
                .workLocation(dto.getWorkLocation())
                .remoteWorkAvailable(dto.getRemoteWorkAvailable())
                .postingStartDate(parseDate(dto.getPostingStartDate()))
                .postingEndDate(parseDate(dto.getPostingEndDate()))
                .isAlwaysRecruiting(dto.getIsAlwaysRecruiting())
                .status(dto.getStatus() != null ? dto.getStatus() : "DRAFT")
                .build();

        jobRepository.save(job);

        return toJobDTO(job, employer);
    }

    /**
     * 채용공고 수정
     */
    @Transactional
    public JobDTO updateJob(String userid, String jobUid, JobUpdateDTO dto) {
        EmployerEntity employer = getEmployerByUserid(userid);

        JobEntity job = jobRepository.findByJobUid(UUID.fromString(jobUid))
                .orElseThrow(() -> new IllegalStateException("채용공고를 찾을 수 없습니다."));

        // 본인 기업의 채용공고인지 확인
        if (!job.getEmployerId().equals(employer.getId())) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        // 업데이트
        if (dto.getTitle() != null) job.setTitle(dto.getTitle());
        if (dto.getDescription() != null) job.setDescription(dto.getDescription());
        if (dto.getRequirements() != null) job.setRequirements(dto.getRequirements());
        if (dto.getPreferredQualifications() != null) job.setPreferredQualifications(dto.getPreferredQualifications());
        if (dto.getJobType() != null) job.setJobType(dto.getJobType());
        if (dto.getExperienceLevel() != null) job.setExperienceLevel(dto.getExperienceLevel());
        if (dto.getEducationLevel() != null) job.setEducationLevel(dto.getEducationLevel());
        if (dto.getSalaryMin() != null) job.setSalaryMin(dto.getSalaryMin());
        if (dto.getSalaryMax() != null) job.setSalaryMax(dto.getSalaryMax());
        if (dto.getSalaryNegotiable() != null) job.setSalaryNegotiable(dto.getSalaryNegotiable());
        if (dto.getWorkLocation() != null) job.setWorkLocation(dto.getWorkLocation());
        if (dto.getRemoteWorkAvailable() != null) job.setRemoteWorkAvailable(dto.getRemoteWorkAvailable());
        if (dto.getPostingStartDate() != null) job.setPostingStartDate(parseDate(dto.getPostingStartDate()));
        if (dto.getPostingEndDate() != null) job.setPostingEndDate(parseDate(dto.getPostingEndDate()));
        if (dto.getIsAlwaysRecruiting() != null) job.setIsAlwaysRecruiting(dto.getIsAlwaysRecruiting());
        if (dto.getStatus() != null) job.setStatus(dto.getStatus());

        jobRepository.save(job);

        return toJobDTO(job, employer);
    }

    /**
     * 채용공고 삭제 (soft delete)
     */
    @Transactional
    public void deleteJob(String userid, String jobUid) {
        EmployerEntity employer = getEmployerByUserid(userid);

        JobEntity job = jobRepository.findByJobUid(UUID.fromString(jobUid))
                .orElseThrow(() -> new IllegalStateException("채용공고를 찾을 수 없습니다."));

        // 본인 기업의 채용공고인지 확인
        if (!job.getEmployerId().equals(employer.getId())) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        job.setDeletedAt(LocalDateTime.now());
        job.setStatus("CLOSED");
        jobRepository.save(job);
    }

    // === Helper Methods ===

    private EmployerEntity getEmployerByUserid(String userid) {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        return employerRepository.findByMemberId(user.getId())
                .orElseThrow(() -> new IllegalStateException("기업 정보를 찾을 수 없습니다."));
    }

    private JobDTO toJobDTO(JobEntity job, EmployerEntity employer) {
        return JobDTO.builder()
                .jobUid(job.getJobUid().toString())
                .title(job.getTitle())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .preferredQualifications(job.getPreferredQualifications())
                .jobType(job.getJobType())
                .experienceLevel(job.getExperienceLevel())
                .educationLevel(job.getEducationLevel())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryNegotiable(job.getSalaryNegotiable())
                .workLocation(job.getWorkLocation())
                .remoteWorkAvailable(job.getRemoteWorkAvailable())
                .postingStartDate(job.getPostingStartDate() != null ? job.getPostingStartDate().toString() : null)
                .postingEndDate(job.getPostingEndDate() != null ? job.getPostingEndDate().toString() : null)
                .isAlwaysRecruiting(job.getIsAlwaysRecruiting())
                .status(job.getStatus())
                .viewCount(job.getViewCount())
                .applicationCount(job.getApplicationCount())
                .createdAt(job.getCreatedAt().toString())
                .updatedAt(job.getUpdatedAt().toString())
                .companyName(employer.getCompanyName())
                .companyLogoUrl(employer.getLogoUrl())
                .build();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        return LocalDate.parse(dateStr);
    }
}
