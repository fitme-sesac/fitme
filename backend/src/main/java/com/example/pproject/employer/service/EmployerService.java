package com.example.pproject.employer.service;

import com.example.pproject.employer.dto.EmployerDashboardDTO;
import com.example.pproject.employer.dto.EmployerProfileDTO;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobRepository;
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
public class EmployerService {

    private final EmployerRepository employerRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    /**
     * 로그인 사용자의 기업 프로필 조회
     */
    public EmployerProfileDTO getProfile(String userid) {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        EmployerEntity employer = employerRepository.findByMemberId(user.getId())
                .orElseThrow(() -> new IllegalStateException("기업 정보를 찾을 수 없습니다."));

        return toProfileDTO(employer);
    }

    /**
     * 기업 대시보드 정보 조회
     */
    public EmployerDashboardDTO getDashboard(String userid) {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        EmployerEntity employer = employerRepository.findByMemberId(user.getId())
                .orElseThrow(() -> new IllegalStateException("기업 정보를 찾을 수 없습니다."));

        // 채용공고 통계
        long activeJobCount = jobRepository.countActiveByEmployerId(employer.getId());
        long totalApplicationCount = jobRepository.sumApplicationCountByEmployerId(employer.getId());

        // 최근 채용공고 5개
        List<JobEntity> recentJobs = jobRepository.findByEmployerIdAndNotDeleted(employer.getId())
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        List<EmployerDashboardDTO.JobSummaryDTO> jobSummaries = recentJobs.stream()
                .map(job -> EmployerDashboardDTO.JobSummaryDTO.builder()
                        .jobUid(job.getJobUid().toString())
                        .title(job.getTitle())
                        .status(job.getStatus())
                        .applicationCount(job.getApplicationCount())
                        .viewCount(job.getViewCount())
                        .createdAt(job.getCreatedAt().toString())
                        .build())
                .collect(Collectors.toList());

        // 전체 조회수 합계
        long totalViewCount = recentJobs.stream()
                .mapToLong(JobEntity::getViewCount)
                .sum();

        return EmployerDashboardDTO.builder()
                .profile(toProfileDTO(employer))
                .activeJobCount(activeJobCount)
                .totalApplicationCount(totalApplicationCount)
                .totalViewCount(totalViewCount)
                .recentJobs(jobSummaries)
                .build();
    }

    /**
     * 기업 프로필 등록/수정
     */
    @Transactional
    public EmployerProfileDTO saveProfile(String userid, EmployerProfileDTO dto) {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        EmployerEntity employer = employerRepository.findByMemberId(user.getId())
                .orElse(EmployerEntity.builder()
                        .memberId(user.getId())
                        .build());

        // 업데이트
        employer.setCompanyName(dto.getCompanyName());
        employer.setBusinessRegistrationNumber(dto.getBusinessRegistrationNumber());
        employer.setRepresentativeName(dto.getRepresentativeName());
        employer.setCompanyAddress(dto.getCompanyAddress());
        employer.setCompanyPhone(dto.getCompanyPhone());
        employer.setCompanyWebsite(dto.getCompanyWebsite());
        employer.setIndustry(dto.getIndustry());
        employer.setEmployeeCount(dto.getEmployeeCount());
        employer.setCompanyDescription(dto.getCompanyDescription());
        employer.setLogoUrl(dto.getLogoUrl());

        employerRepository.save(employer);

        return toProfileDTO(employer);
    }

    private EmployerProfileDTO toProfileDTO(EmployerEntity entity) {
        return EmployerProfileDTO.builder()
                .employerUid(entity.getEmployerUid().toString())
                .companyName(entity.getCompanyName())
                .businessRegistrationNumber(entity.getBusinessRegistrationNumber())
                .representativeName(entity.getRepresentativeName())
                .companyAddress(entity.getCompanyAddress())
                .companyPhone(entity.getCompanyPhone())
                .companyWebsite(entity.getCompanyWebsite())
                .industry(entity.getIndustry())
                .employeeCount(entity.getEmployeeCount())
                .companyDescription(entity.getCompanyDescription())
                .logoUrl(entity.getLogoUrl())
                .status(entity.getStatus())
                .build();
    }
}
