package com.example.pproject.employer.service;

import com.example.pproject.employer.dto.EmployerDashboardDTO;
import com.example.pproject.employer.dto.EmployerProfileDTO;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobRepository;
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
public class EmployerService {

    private final EmployerRepository employerRepository;
    private final EmployerMemberRepository employerMemberRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    /**
     * 로그인 사용자의 기업 프로필 조회
     */
    public EmployerProfileDTO getProfile(String userid) {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long memberId = user.getId().longValue();

        // employer_member에서 소속 기업 찾기
        EmployerMemberEntity membership = employerMemberRepository
                .findFirstByMemberIdAndActiveTrue(memberId)
                .orElse(null);
        
        if (membership == null) {
            log.info("사용자 {}에게 소속된 기업이 없습니다.", userid);
            return null;
        }
        
        EmployerEntity employer = employerRepository.findById(membership.getEmployerId())
                .orElse(null);
        
        if (employer == null || employer.getDeletedAt() != null) {
            log.info("기업 ID {}를 찾을 수 없습니다.", membership.getEmployerId());
            return null;
        }

        return toProfileDTO(employer, membership.getRoleInCompany());
    }

    /**
     * 기업 대시보드 정보 조회
     */
    public EmployerDashboardDTO getDashboard(String userid) {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long memberId = user.getId().longValue();

        // employer_member에서 소속 기업 찾기
        EmployerMemberEntity membership = employerMemberRepository
                .findFirstByMemberIdAndActiveTrue(memberId)
                .orElse(null);
        
        if (membership == null) {
            // 소속 기업이 없는 경우 빈 대시보드 반환
            log.info("사용자 {}에게 소속된 기업이 없어 빈 대시보드 반환", userid);
            return EmployerDashboardDTO.builder()
                    .profile(null)
                    .activeJobCount(0)
                    .totalApplicationCount(0)
                    .totalViewCount(0)
                    .recentJobs(List.of())
                    .build();
        }
        
        EmployerEntity employer = employerRepository.findById(membership.getEmployerId())
                .orElse(null);

        if (employer == null || employer.getDeletedAt() != null) {
            return EmployerDashboardDTO.builder()
                    .profile(null)
                    .activeJobCount(0)
                    .totalApplicationCount(0)
                    .totalViewCount(0)
                    .recentJobs(List.of())
                    .build();
        }

        // 채용공고 통계
        long activeJobCount = 0;
        long totalApplicationCount = 0;
        long totalViewCount = 0;
        List<EmployerDashboardDTO.JobSummaryDTO> jobSummaries = List.of();

        try {
            activeJobCount = jobRepository.countActiveByEmployerId(employer.getId());
            totalApplicationCount = jobRepository.sumApplicationCountByEmployerId(employer.getId());

            // 최근 채용공고 5개
            List<JobEntity> recentJobs = jobRepository.findByEmployerIdAndNotDeleted(employer.getId())
                    .stream()
                    .limit(5)
                    .collect(Collectors.toList());

            jobSummaries = recentJobs.stream()
                    .map(job -> EmployerDashboardDTO.JobSummaryDTO.builder()
                            .jobId(job.getId())
                            .jobUid(String.valueOf(job.getId())) // ID를 문자열로
                            .title(job.getTitle())
                            .status(job.getStatus())
                            .applicationCount(job.getApplicationCount() != null ? job.getApplicationCount() : 0)
                            .viewCount(job.getViewCount() != null ? job.getViewCount() : 0)
                            .createdAt(job.getCreatedAt() != null ? job.getCreatedAt().toString() : null)
                            .build())
                    .collect(Collectors.toList());

            // 전체 조회수 합계
            totalViewCount = recentJobs.stream()
                    .mapToLong(job -> job.getViewCount() != null ? job.getViewCount() : 0)
                    .sum();
        } catch (Exception e) {
            log.warn("채용공고 통계 조회 중 오류 (무시됨): {}", e.getMessage());
        }

        return EmployerDashboardDTO.builder()
                .profile(toProfileDTO(employer, membership.getRoleInCompany()))
                .activeJobCount(activeJobCount)
                .totalApplicationCount(totalApplicationCount)
                .totalViewCount(totalViewCount)
                .recentJobs(jobSummaries)
                .build();
    }

    /**
     * 기업 프로필 등록 (새 기업 생성 + employer_member 매핑)
     */
    @Transactional
    public EmployerProfileDTO createProfile(String userid, EmployerProfileDTO dto) {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long memberId = user.getId().longValue();

        // 이미 소속된 기업이 있는지 확인
        if (employerMemberRepository.existsByMemberIdAndActiveTrue(memberId)) {
            throw new IllegalStateException("이미 소속된 기업이 있습니다.");
        }

        // 기업 생성
        EmployerEntity employer = EmployerEntity.builder()
                .name(dto.getName())
                .logoUrl(dto.getLogoUrl())
                .industry(dto.getIndustry())
                .foundedYear(dto.getFoundedYear())
                .employeeCount(dto.getEmployeeCount())
                .location(dto.getLocation())
                .description(dto.getDescription())
                .culture(dto.getCulture())
                .benefits(dto.getBenefits())
                .techStack(dto.getTechStack())
                .contactEmail(dto.getContactEmail())
                .contactPhone(dto.getContactPhone())
                .websiteUrl(dto.getWebsiteUrl())
                .build();
        
        employerRepository.save(employer);
        log.info("새 기업 생성: {}", employer.getName());

        // employer_member 매핑 생성 (OWNER로)
        EmployerMemberEntity membership = EmployerMemberEntity.builder()
                .employerId(employer.getId())
                .memberId(memberId)
                .roleInCompany("OWNER")
                .active(true)
                .build();
        
        employerMemberRepository.save(membership);
        log.info("사용자 {}를 기업 {} OWNER로 등록", userid, employer.getName());

        return toProfileDTO(employer, "OWNER");
    }

    /**
     * 기업 프로필 수정
     */
    @Transactional
    public EmployerProfileDTO updateProfile(String userid, EmployerProfileDTO dto) {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long memberId = user.getId().longValue();

        // 소속 기업 확인
        EmployerMemberEntity membership = employerMemberRepository
                .findFirstByMemberIdAndActiveTrue(memberId)
                .orElseThrow(() -> new IllegalStateException("소속된 기업이 없습니다. 먼저 기업을 등록해주세요."));
        
        // OWNER 또는 HR만 수정 가능
        if (!"OWNER".equals(membership.getRoleInCompany()) && !"HR".equals(membership.getRoleInCompany())) {
            throw new IllegalStateException("기업 정보 수정 권한이 없습니다.");
        }
        
        EmployerEntity employer = employerRepository.findById(membership.getEmployerId())
                .orElseThrow(() -> new IllegalStateException("기업 정보를 찾을 수 없습니다."));

        // 업데이트
        employer.setName(dto.getName());
        employer.setLogoUrl(dto.getLogoUrl());
        employer.setIndustry(dto.getIndustry());
        employer.setFoundedYear(dto.getFoundedYear());
        employer.setEmployeeCount(dto.getEmployeeCount());
        employer.setLocation(dto.getLocation());
        employer.setDescription(dto.getDescription());
        employer.setCulture(dto.getCulture());
        employer.setBenefits(dto.getBenefits());
        employer.setTechStack(dto.getTechStack());
        employer.setContactEmail(dto.getContactEmail());
        employer.setContactPhone(dto.getContactPhone());
        employer.setWebsiteUrl(dto.getWebsiteUrl());

        employerRepository.save(employer);
        log.info("기업 {} 정보 수정", employer.getName());

        return toProfileDTO(employer, membership.getRoleInCompany());
    }

    /**
     * 기업 프로필 등록/수정 (기존 API 호환)
     */
    @Transactional
    public EmployerProfileDTO saveProfile(String userid, EmployerProfileDTO dto) {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long memberId = user.getId().longValue();

        // 소속 기업 확인
        boolean hasEmployer = employerMemberRepository.existsByMemberIdAndActiveTrue(memberId);
        
        if (!hasEmployer) {
            // 새로 생성
            return createProfile(userid, dto);
        } else {
            // 수정
            return updateProfile(userid, dto);
        }
    }

    /**
     * 회원의 소속 기업 ID 조회 (JobService 등에서 사용)
     */
    public Long getEmployerIdByMemberId(Long memberId) {
        return employerMemberRepository
                .findFirstByMemberIdAndActiveTrue(memberId)
                .map(EmployerMemberEntity::getEmployerId)
                .orElse(null);
    }

    private EmployerProfileDTO toProfileDTO(EmployerEntity entity, String roleInCompany) {
        return EmployerProfileDTO.builder()
                .employerId(entity.getId())
                .employerUid(entity.getEmployerUid().toString())
                .name(entity.getName())
                .logoUrl(entity.getLogoUrl())
                .industry(entity.getIndustry())
                .foundedYear(entity.getFoundedYear())
                .employeeCount(entity.getEmployeeCount())
                .location(entity.getLocation())
                .description(entity.getDescription())
                .culture(entity.getCulture())
                .benefits(entity.getBenefits())
                .techStack(entity.getTechStack())
                .contactEmail(entity.getContactEmail())
                .contactPhone(entity.getContactPhone())
                .websiteUrl(entity.getWebsiteUrl())
                .status(entity.getStatus())
                .roleInCompany(roleInCompany)
                .build();
    }
}
