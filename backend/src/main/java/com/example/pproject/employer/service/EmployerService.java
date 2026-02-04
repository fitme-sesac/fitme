package com.example.pproject.employer.service;

import com.example.pproject.ad.dto.AdCampaignUpdateDTO;
import com.example.pproject.ad.repository.AdCampaignRepository;
import com.example.pproject.common.service.FileUploadService;
import com.example.pproject.employer.dto.*;
import com.example.pproject.common.util.ArrayStringUtil;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.dto.JobMatchInfoDTO;
import com.example.pproject.job.service.JobService;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobEntityRepository;
import com.example.pproject.outbox.producer.OutboxEventProducer;
import com.example.pproject.resume.service.ResumeSkillService;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployerService {

    private final EmployerRepository employerRepository;
    private final EmployerMemberRepository employerMemberRepository;
    private final JobEntityRepository jobEntityRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final OutboxEventProducer outboxEventProducer;
    private final JobService jobService;
    private final ResumeSkillService resumeSkillService;
    private final AdCampaignRepository adCampaignRepository;
    private final FileUploadService fileUploadService;

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
            activeJobCount = jobEntityRepository.countActiveByEmployerId(employer.getId());
            totalApplicationCount = jobEntityRepository.sumApplicationCountByEmployerId(employer.getId());

            // 최근 채용공고 5개
            List<JobEntity> recentJobs = jobEntityRepository.findByEmployerIdAndNotDeleted(employer.getId())
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

            // 전체 조회수 합계 (해당 기업의 모든 공고)
            totalViewCount = jobEntityRepository.sumViewCountByEmployerId(employer.getId());
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
        
        // 저장된 엔티티를 반환받아 ID 등이 채워진 상태로 갱신
        employer = employerRepository.save(employer);
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
     * 기업 로고 파일 업로드
     */
    @Transactional
    public String uploadLogo(String userid, MultipartFile file) throws IOException {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long memberId = user.getId().longValue();

        // 소속 기업 확인
        EmployerMemberEntity membership = employerMemberRepository
                .findFirstByMemberIdAndActiveTrue(memberId)
                .orElseThrow(() -> new IllegalStateException("소속된 기업이 없습니다."));

        // OWNER 또는 HR만 수정 가능
        if (!"OWNER".equals(membership.getRoleInCompany()) && !"HR".equals(membership.getRoleInCompany())) {
            throw new IllegalStateException("로고 수정 권한이 없습니다.");
        }

        EmployerEntity employer = employerRepository.findById(membership.getEmployerId())
                .orElseThrow(() -> new IllegalStateException("기업 정보를 찾을 수 없습니다."));

        // 기존 로고가 있으면 삭제 (내부 업로드 파일인 경우만)
        String oldLogoUrl = employer.getLogoUrl();
        if (oldLogoUrl != null && oldLogoUrl.startsWith("/images/")) {
            fileUploadService.deleteFile(oldLogoUrl);
        }

        // 새 로고 업로드
        String newLogoUrl = fileUploadService.uploadFile(file, "logos");

        // DB 업데이트
        employer.setLogoUrl(newLogoUrl);
        employerRepository.save(employer);

        log.info("기업 {} 로고 업로드 완료: {}", employer.getName(), newLogoUrl);

        return newLogoUrl;
    }

    /**
     * 기업 로고 삭제
     */
    @Transactional
    public void deleteLogo(String userid) {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long memberId = user.getId().longValue();

        // 소속 기업 확인
        EmployerMemberEntity membership = employerMemberRepository
                .findFirstByMemberIdAndActiveTrue(memberId)
                .orElseThrow(() -> new IllegalStateException("소속된 기업이 없습니다."));

        // OWNER 또는 HR만 수정 가능
        if (!"OWNER".equals(membership.getRoleInCompany()) && !"HR".equals(membership.getRoleInCompany())) {
            throw new IllegalStateException("로고 수정 권한이 없습니다.");
        }

        EmployerEntity employer = employerRepository.findById(membership.getEmployerId())
                .orElseThrow(() -> new IllegalStateException("기업 정보를 찾을 수 없습니다."));

        // 기존 로고 삭제 (내부 업로드 파일인 경우만)
        String oldLogoUrl = employer.getLogoUrl();
        if (oldLogoUrl != null && oldLogoUrl.startsWith("/images/")) {
            fileUploadService.deleteFile(oldLogoUrl);
        }

        // DB 업데이트
        employer.setLogoUrl(null);
        employerRepository.save(employer);

        log.info("기업 {} 로고 삭제 완료", employer.getName());
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
                .employerUid(entity.getEmployerUid() != null ? entity.getEmployerUid().toString() : null)
                .name(entity.getName())
                .logoUrl(entity.getLogoUrl())
                .industry(entity.getIndustry())
                .foundedYear(entity.getFoundedYear())
                .employeeCount(entity.getEmployeeCount())
                .location(entity.getLocation())
                .description(entity.getDescription())
                .culture(entity.getCulture())
                .benefits(entity.getBenefits())
                .techStack(ArrayStringUtil.cleanArrayString(entity.getTechStack()))
                .contactEmail(entity.getContactEmail())
                .contactPhone(entity.getContactPhone())
                .websiteUrl(entity.getWebsiteUrl())
                .status(entity.getStatus())
                .roleInCompany(roleInCompany)
                .build();
    }

    // ===== 광고 통계 =====

    /**
     * 광고 통계 조회
     */
    public AdStatsDTO getAdStats(String userid) {
        return getAdStats(userid, false);
    }

    /**
     * 광고 통계 조회
     * @param demo true면 클릭 로그가 없을 때도 데모용 수치 생성(DB에는 저장하지 않음)
     */
    public AdStatsDTO getAdStats(String userid, boolean demo) {
        Long employerId = getEmployerIdByUserid(userid);
        if (employerId == null) {
            return AdStatsDTO.builder()
                    .activeCampaigns(0)
                    .totalClicks(0)
                    .totalImpressions(0)
                    .totalSpent(0)
                    .ctr(0.0)
                    .campaigns(List.of())
                    .build();
        }

        try {
            // 광고 캠페인 통계 조회
            // impressions는 클릭 수에 기반해 계산 (CPC 입찰가가 높을수록 노출 대비 클릭률이 높다고 가정)
            // NOTE: clicks는 LEFT JOIN + GROUP BY로 집계 (샘플 데이터 대량 삽입 시에도 안정적으로 반영)
            // NOTE: applicants는 해당 job_id에 대한 지원자 수 (CANCELED 제외)
            String sql = """
                SELECT 
                    ac.campaign_id,
                    ac.job_id,
                    jp.title as job_title,
                    ac.status,
                    ac.cpc_bid,
                    ac.daily_budget,
                    COUNT(DISTINCT ace.click_id) as clicks,
                    EXTRACT(EPOCH FROM (NOW() - ac.created_at)) / 86400 as days_running,
                    (SELECT COUNT(*) FROM job_application ja 
                     WHERE ja.job_id = ac.job_id AND ja.status != 'CANCELED') as applicants
                FROM ad_campaign ac
                JOIN job_posting jp ON jp.job_id = ac.job_id
                LEFT JOIN ad_click_event ace ON ace.campaign_id = ac.campaign_id
                WHERE ac.employer_id = ?
                GROUP BY ac.campaign_id, ac.job_id, jp.title, ac.status, ac.cpc_bid, ac.daily_budget, ac.created_at
                ORDER BY ac.created_at DESC
                LIMIT 10
                """;

            // 공고마다 다른 CTR (0.7% ~ 4.5%) → 노출수·클릭률이 캠페인별로 확실히 다르게 나오도록
            double[] ctrRates = { 0.007, 0.012, 0.018, 0.028, 0.038, 0.045 }; // 0.7%, 1.2%, 1.8%, 2.8%, 3.8%, 4.5%
            final boolean demoMode = demo;
            List<AdStatsDTO.CampaignDTO> campaigns = jdbcTemplate.query(sql,
                    (rs, rowNum) -> {
                        long campaignId = rs.getLong("campaign_id");
                        long jobId = rs.getLong("job_id");
                        int clicks = rs.getInt("clicks");
                        int cpcBid = rs.getInt("cpc_bid");
                        int dailyBudget = rs.getInt("daily_budget");
                        double daysRunning = Math.max(1, rs.getDouble("days_running"));
                        int applicants = rs.getInt("applicants");
                        
                        int idx = rowNum % ctrRates.length;
                        double baseCtr = ctrRates[idx];

                        // 데모 모드: 클릭 로그가 0일 때도 "볼만한" 수치로 내려줌 (DB 저장 X)
                        // - 실제 로그가 쌓이기 시작하면(=clicks > 0) 그 값이 그대로 우선됨
                        if (demoMode && clicks == 0) {
                            long seed = (campaignId * 31L) ^ (jobId * 17L) ^ (cpcBid * 13L) ^ (dailyBudget * 7L);
                            int base = (int) (Math.abs(seed) % 9000) + 300; // 300~9299
                            // 운영 기간이 길수록 더 많은 클릭이 있었던 것처럼 보이게
                            clicks = (int) Math.min(25000, Math.round(base * Math.min(3.0, 0.6 + (daysRunning / 7.0))));
                        }

                        int impressions = clicks > 0 ? (int) Math.ceil(clicks / baseCtr) : (int) (daysRunning * dailyBudget / Math.max(1, cpcBid) * 12);
                        double ctr = impressions > 0 ? (double) clicks / impressions * 100 : 0;
                        
                        return AdStatsDTO.CampaignDTO.builder()
                                .campaignId(campaignId)
                                .jobId(jobId)
                                .jobTitle(rs.getString("job_title"))
                                .status(rs.getString("status"))
                                .cpcBid(cpcBid)
                                .dailyBudget(dailyBudget)
                                .clicks(clicks)
                                .impressions(impressions)
                                .ctr(Math.round(ctr * 10) / 10.0)
                                .applicants(applicants)
                                .build();
                    },
                    employerId);

            int activeCampaigns = (int) campaigns.stream().filter(c -> "ACTIVE".equals(c.getStatus())).count();
            long totalClicks = campaigns.stream().mapToLong(AdStatsDTO.CampaignDTO::getClicks).sum();
            long totalImpressions = campaigns.stream().mapToLong(AdStatsDTO.CampaignDTO::getImpressions).sum();
            long totalSpent = campaigns.stream().mapToLong(c -> (long) c.getClicks() * c.getCpcBid()).sum();
            double avgCtr = totalImpressions > 0 ? (double) totalClicks / totalImpressions * 100 : 0;

            return AdStatsDTO.builder()
                    .activeCampaigns(activeCampaigns)
                    .totalClicks(totalClicks)
                    .totalImpressions(totalImpressions)
                    .totalSpent(totalSpent)
                    .ctr(Math.round(avgCtr * 10) / 10.0)
                    .campaigns(campaigns)
                    .build();
        } catch (Exception e) {
            log.warn("광고 통계 조회 실패 (테이블 없음): {}", e.getMessage());
            return AdStatsDTO.builder()
                    .activeCampaigns(0)
                    .totalClicks(0)
                    .totalImpressions(0)
                    .totalSpent(0)
                    .ctr(0.0)
                    .campaigns(List.of())
                    .build();
        }
    }

    /**
     * 광고 캠페인 수정 (기업 소유권 검증 후 DB 직접 업데이트)
     * Redis 의존성을 제거하여 Redis 미실행 환경에서도 동작하도록 함
     */
    @Transactional
    public void updateAdCampaign(String userid, Long campaignId, AdCampaignUpdateDTO dto) {
        Long employerId = getEmployerIdByUserid(userid);
        if (employerId == null) {
            throw new IllegalStateException("소속된 기업이 없습니다.");
        }
        var campaignOpt = adCampaignRepository.findByIdAndNotDeleted(campaignId);
        if (campaignOpt.isEmpty()) {
            throw new IllegalArgumentException("광고 캠페인을 찾을 수 없습니다. ID: " + campaignId);
        }
        var campaign = campaignOpt.get();
        if (!employerId.equals(campaign.getEmployerId())) {
            throw new IllegalStateException("해당 광고 캠페인에 대한 권한이 없습니다.");
        }
        
        // Redis 없이 DB만 직접 업데이트
        if (dto.getCpcBid() != null) {
            campaign.setCpcBid(dto.getCpcBid());
        }
        if (dto.getDailyBudget() != null) {
            campaign.setDailyBudget(dto.getDailyBudget());
        }
        // startDate, endDate도 필요 시 추가 가능
        
        adCampaignRepository.save(campaign);
        log.info("광고 캠페인 수정 완료 (DB only). CampaignId: {}, EmployerId: {}", campaignId, employerId);
    }

    // ===== 지원자 관리 =====

    /**
     * 지원자 목록 조회 (userid 기반, 일반 로그인용)
     */
    public ApplicantListDTO getApplicants(String userid, String status) {
        Long employerId = getEmployerIdByUserid(userid);
        return getApplicantsByEmployerId(employerId, status, null);
    }

    /**
     * 지원자 목록 조회 (member_id 기반, JWT id 클레임 또는 OAuth 사용자 대응)
     */
    public ApplicantListDTO getApplicants(Long memberId, String status) {
        Long employerId = getEmployerIdByMemberId(memberId);
        return getApplicantsByEmployerId(employerId, status, null);
    }

    /**
     * 특정 채용공고의 지원자 목록 조회
     */
    public ApplicantListDTO getApplicantsByJob(Long memberId, Long jobId, String status) {
        Long employerId = getEmployerIdByMemberId(memberId);
        if (employerId == null) {
            throw new IllegalStateException("소속된 기업이 없습니다.");
        }
        // 해당 채용공고가 기업 소속인지 확인
        JobEntity job = jobEntityRepository.findById(jobId).orElse(null);
        if (job == null || job.getDeletedAt() != null) {
            throw new IllegalStateException("채용공고를 찾을 수 없습니다.");
        }
        if (!job.getEmployerId().equals(employerId)) {
            throw new IllegalStateException("해당 채용공고에 대한 권한이 없습니다.");
        }
        return getApplicantsByEmployerId(employerId, status, jobId);
    }

    private ApplicantListDTO getApplicantsByEmployerId(Long employerId, String status, Long jobId) {
        if (employerId == null) {
            return ApplicantListDTO.builder()
                    .applicants(List.of())
                    .total(0)
                    .build();
        }

        try {
            StringBuilder sql = new StringBuilder("""
                SELECT 
                    ja.application_id,
                    ja.job_id,
                    jp.title as job_title,
                    ja.member_id,
                    m.name,
                    m.email,
                    m.phone,
                    ja.resume_id,
                    r.title as resume_title,
                    ja.status,
                    ja.applied_at,
                    ja.viewed_at
                FROM job_application ja
                JOIN job_posting jp ON jp.job_id = ja.job_id
                JOIN member m ON m.member_id = ja.member_id
                LEFT JOIN resume r ON r.resume_id = ja.resume_id
                WHERE jp.employer_id = ?
                  AND jp.deleted_at IS NULL
                  AND m.deleted_at IS NULL
                  AND ja.status != 'CANCELED'
                """);

            List<Object> params = new ArrayList<>();
            params.add(employerId);

            // 특정 채용공고 필터
            if (jobId != null) {
                sql.append(" AND ja.job_id = ?");
                params.add(jobId);
            }

            if (status != null && !status.isBlank()) {
                sql.append(" AND ja.status = ?");
                params.add(status);
            }

            sql.append(" ORDER BY ja.applied_at DESC LIMIT 50");

            List<ApplicantListDTO.ApplicantDTO> applicants = jdbcTemplate.query(sql.toString(),
                    (rs, rowNum) -> ApplicantListDTO.ApplicantDTO.builder()
                            .applicationId(rs.getLong("application_id"))
                            .jobId(rs.getLong("job_id"))
                            .jobTitle(rs.getString("job_title"))
                            .memberId(rs.getLong("member_id"))
                            .name(rs.getString("name"))
                            .email(rs.getString("email"))
                            .phone(rs.getString("phone"))
                            .resumeId(rs.getLong("resume_id"))
                            .resumeTitle(rs.getString("resume_title"))
                            .status(rs.getString("status"))
                            .appliedAt(rs.getTimestamp("applied_at") != null ? 
                                    rs.getTimestamp("applied_at").toLocalDateTime().toString() : null)
                            .viewedAt(rs.getTimestamp("viewed_at") != null ? 
                                    rs.getTimestamp("viewed_at").toLocalDateTime().toString() : null)
                            .build(),
                    params.toArray());

            // 채용공고(stack) vs 구직자 이력서(re_stack, tech_stack) 기반 매칭 정보 계산
            for (ApplicantListDTO.ApplicantDTO dto : applicants) {
                try {
                    JobEntity job = jobEntityRepository.findById(dto.getJobId()).orElse(null);
                    if (job == null) {
                        dto.setMatchInfo(emptyMatchInfo());
                        continue;
                    }
                    String jobStack = ArrayStringUtil.listToString(job.getStack());
                    Set<String> candidateSkills = dto.getMemberId() != null
                            ? resumeSkillService.getSkillsByMemberId(dto.getMemberId())
                            : Collections.emptySet();
                    if (candidateSkills == null) candidateSkills = Collections.emptySet();
                    JobMatchInfoDTO matchInfo = jobService.calculateMatchInfo(
                            jobStack != null ? jobStack : "",
                            candidateSkills,
                            job,
                            dto.getMemberId());
                    dto.setMatchInfo(matchInfo);
                } catch (Exception e) {
                    log.warn("지원자 매칭률 계산 실패 applicationId={}, jobId={}, memberId={}: {}",
                            dto.getApplicationId(), dto.getJobId(), dto.getMemberId(), e.getMessage());
                    dto.setMatchInfo(emptyMatchInfo());
                }
            }

            return ApplicantListDTO.builder()
                    .applicants(applicants)
                    .total(applicants.size())
                    .build();
        } catch (Exception e) {
            log.warn("지원자 목록 조회 실패: {}", e.getMessage());
            return ApplicantListDTO.builder()
                    .applicants(List.of())
                    .total(0)
                    .build();
        }
    }

    /**
     * 지원자 상태 변경
     */
    @Transactional
    public void updateApplicantStatus(String userid, Long applicationId, String newStatus) {
        Long employerId = getEmployerIdByUserid(userid);
        if (employerId == null) {
            throw new IllegalStateException("소속된 기업이 없습니다.");
        }

        // 지원서가 해당 기업의 공고에 대한 것인지 확인
        String checkSql = """
            SELECT COUNT(*) FROM job_application ja
            JOIN job_posting jp ON jp.job_id = ja.job_id
            WHERE ja.application_id = ? AND jp.employer_id = ?
            """;
        
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, applicationId, employerId);
        if (count == null || count == 0) {
            throw new IllegalStateException("해당 지원서에 대한 권한이 없습니다.");
        }

        // 상태 업데이트
        String updateSql = "UPDATE job_application SET status = ?, updated_at = NOW() WHERE application_id = ?";
        jdbcTemplate.update(updateSql, newStatus, applicationId);
        
        log.info("지원서 {} 상태 변경: {}", applicationId, newStatus);

        // 지원자에게 알림 발송
        try {
            String infoSql = """
                SELECT ja.member_id, jp.title as job_title, e.name as company_name
                FROM job_application ja
                JOIN job_posting jp ON jp.job_id = ja.job_id
                JOIN employer e ON e.employer_id = jp.employer_id
                WHERE ja.application_id = ?
                """;
            var info = jdbcTemplate.queryForMap(infoSql, applicationId);
            
            Long candidateMemberId = ((Number) info.get("member_id")).longValue();
            String jobTitle = (String) info.get("job_title");
            String companyName = (String) info.get("company_name");
            
            outboxEventProducer.publishApplicationStatusChangedEvent(
                    applicationId,
                    candidateMemberId,
                    jobTitle,
                    companyName,
                    newStatus
            );
            log.info("지원 상태 변경 알림 발행: applicationId={}, candidateMemberId={}, newStatus={}", 
                    applicationId, candidateMemberId, newStatus);
        } catch (Exception e) {
            log.warn("지원 상태 변경 알림 발행 실패: {}", e.getMessage());
        }
    }

    // ===== 면접 일정 관리 =====

    /**
     * 면접 일정 목록 조회
     */
    public InterviewListDTO getInterviews(String userid, Integer year, Integer month) {
        Long employerId = getEmployerIdByUserid(userid);
        if (employerId == null) {
            return InterviewListDTO.builder()
                    .interviews(List.of())
                    .total(0)
                    .build();
        }

        // 기본값: 현재 년/월
        int y = year != null ? year : LocalDateTime.now().getYear();
        int m = month != null ? month : LocalDateTime.now().getMonthValue();

        YearMonth ym = YearMonth.of(y, m);
        LocalDateTime startDate = ym.atDay(1).atStartOfDay();
        LocalDateTime endDate = ym.atEndOfMonth().atTime(23, 59, 59);

        try {
            String sql = """
                SELECT 
                    isc.interview_id,
                    isc.application_id,
                    ja.member_id as applicant_member_id,
                    mb.name as applicant_name,
                    mb.email as applicant_email,
                    jp.title as job_title,
                    isc.stage,
                    isc.method,
                    isc.location,
                    isc.meeting_url,
                    isc.start_at,
                    isc.end_at,
                    isc.status,
                    isc.created_at,
                    isc.updated_at
                FROM interview_schedule isc
                JOIN job_application ja ON ja.application_id = isc.application_id
                JOIN job_posting jp ON jp.job_id = ja.job_id
                JOIN member mb ON mb.member_id = ja.member_id
                WHERE jp.employer_id = ?
                  AND isc.start_at >= ? AND isc.start_at <= ?
                ORDER BY isc.start_at ASC
                """;

            List<InterviewDTO> interviews = jdbcTemplate.query(sql,
                    (rs, rowNum) -> InterviewDTO.builder()
                            .interviewId(rs.getLong("interview_id"))
                            .applicationId(rs.getLong("application_id"))
                            .applicantMemberId(rs.getLong("applicant_member_id"))
                            .applicantName(rs.getString("applicant_name"))
                            .applicantEmail(rs.getString("applicant_email"))
                            .jobTitle(rs.getString("job_title"))
                            .stage(rs.getString("stage"))
                            .method(rs.getString("method"))
                            .location(rs.getString("location"))
                            .meetingUrl(rs.getString("meeting_url"))
                            .startAt(rs.getTimestamp("start_at") != null ? 
                                    rs.getTimestamp("start_at").toLocalDateTime().toString() : null)
                            .endAt(rs.getTimestamp("end_at") != null ? 
                                    rs.getTimestamp("end_at").toLocalDateTime().toString() : null)
                            .status(rs.getString("status"))
                            .createdAt(rs.getTimestamp("created_at") != null ? 
                                    rs.getTimestamp("created_at").toLocalDateTime().toString() : null)
                            .build(),
                    employerId, startDate, endDate);

            return InterviewListDTO.builder()
                    .interviews(interviews)
                    .total(interviews.size())
                    .year(y)
                    .month(m)
                    .build();
        } catch (Exception e) {
            log.warn("면접 일정 조회 실패: {}", e.getMessage());
            return InterviewListDTO.builder()
                    .interviews(List.of())
                    .total(0)
                    .year(y)
                    .month(m)
                    .build();
        }
    }

    /**
     * 면접 일정 등록
     */
    @Transactional
    public InterviewDTO createInterview(String userid, InterviewDTO dto) {
        Long employerId = getEmployerIdByUserid(userid);
        if (employerId == null) {
            throw new IllegalStateException("소속된 기업이 없습니다.");
        }

        // 지원서가 해당 기업의 공고에 대한 것인지 확인
        String checkSql = """
            SELECT COUNT(*) FROM job_application ja
            JOIN job_posting jp ON jp.job_id = ja.job_id
            WHERE ja.application_id = ? AND jp.employer_id = ?
            """;
        
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, dto.getApplicationId(), employerId);
        if (count == null || count == 0) {
            throw new IllegalStateException("해당 지원서에 대한 권한이 없습니다.");
        }

        // 면접 일정 생성
        String insertSql = """
            INSERT INTO interview_schedule 
            (application_id, stage, method, location, meeting_url, start_at, end_at, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?::timestamp, ?::timestamp, 'PROPOSED', NOW(), NOW())
            RETURNING interview_id
            """;

        Long interviewId = jdbcTemplate.queryForObject(insertSql, Long.class,
                dto.getApplicationId(),
                dto.getStage() != null ? dto.getStage() : "1ST",
                dto.getMethod() != null ? dto.getMethod() : "ONSITE",
                dto.getLocation(),
                dto.getMeetingUrl(),
                dto.getStartAt(),
                dto.getEndAt());

        // 지원서 상태 업데이트
        jdbcTemplate.update("UPDATE job_application SET status = 'INTERVIEW', updated_at = NOW() WHERE application_id = ?",
                dto.getApplicationId());

        log.info("면접 일정 생성: {}", interviewId);

        // 지원자에게 알림 발송
        try {
            // 지원자 정보 조회
            String infoSql = """
                SELECT ja.member_id, jp.title as job_title, e.name as company_name
                FROM job_application ja
                JOIN job_posting jp ON jp.job_id = ja.job_id
                JOIN employer e ON e.employer_id = jp.employer_id
                WHERE ja.application_id = ?
                """;
            var info = jdbcTemplate.queryForMap(infoSql, dto.getApplicationId());
            
            Long candidateMemberId = ((Number) info.get("member_id")).longValue();
            String jobTitle = (String) info.get("job_title");
            String companyName = (String) info.get("company_name");
            
            outboxEventProducer.publishInterviewCreatedEvent(
                    interviewId,
                    dto.getApplicationId(),
                    candidateMemberId,
                    jobTitle,
                    companyName,
                    dto.getStartAt(),
                    dto.getLocation()
            );
            log.info("면접 일정 등록 알림 발행: interviewId={}, candidateMemberId={}", interviewId, candidateMemberId);
        } catch (Exception e) {
            log.warn("면접 일정 등록 알림 발행 실패: {}", e.getMessage());
        }

        dto.setInterviewId(interviewId);
        dto.setStatus("PROPOSED");
        return dto;
    }

    /**
     * 면접 일정 수정
     */
    @Transactional
    public InterviewDTO updateInterview(String userid, Long interviewId, InterviewDTO dto) {
        Long employerId = getEmployerIdByUserid(userid);
        if (employerId == null) {
            throw new IllegalStateException("소속된 기업이 없습니다.");
        }

        // 권한 확인
        String checkSql = """
            SELECT COUNT(*) FROM interview_schedule isc
            JOIN job_application ja ON ja.application_id = isc.application_id
            JOIN job_posting jp ON jp.job_id = ja.job_id
            WHERE isc.interview_id = ? AND jp.employer_id = ?
            """;
        
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, interviewId, employerId);
        if (count == null || count == 0) {
            throw new IllegalStateException("해당 면접 일정에 대한 권한이 없습니다.");
        }

        // 업데이트
        String updateSql = """
            UPDATE interview_schedule SET
                stage = ?,
                method = ?,
                location = ?,
                meeting_url = ?,
                start_at = ?::timestamp,
                end_at = ?::timestamp,
                updated_at = NOW()
            WHERE interview_id = ?
            """;

        jdbcTemplate.update(updateSql,
                dto.getStage(),
                dto.getMethod(),
                dto.getLocation(),
                dto.getMeetingUrl(),
                dto.getStartAt(),
                dto.getEndAt(),
                interviewId);

        log.info("면접 일정 수정: {}", interviewId);

        // 지원자에게 알림 발송
        try {
            String infoSql = """
                SELECT ja.application_id, ja.member_id, jp.title as job_title, e.name as company_name
                FROM interview_schedule isc
                JOIN job_application ja ON ja.application_id = isc.application_id
                JOIN job_posting jp ON jp.job_id = ja.job_id
                JOIN employer e ON e.employer_id = jp.employer_id
                WHERE isc.interview_id = ?
                """;
            var info = jdbcTemplate.queryForMap(infoSql, interviewId);
            
            Long applicationId = ((Number) info.get("application_id")).longValue();
            Long candidateMemberId = ((Number) info.get("member_id")).longValue();
            String jobTitle = (String) info.get("job_title");
            String companyName = (String) info.get("company_name");
            
            outboxEventProducer.publishInterviewUpdatedEvent(
                    interviewId,
                    applicationId,
                    candidateMemberId,
                    jobTitle,
                    companyName,
                    dto.getStartAt()
            );
            log.info("면접 일정 수정 알림 발행: interviewId={}, candidateMemberId={}", interviewId, candidateMemberId);
        } catch (Exception e) {
            log.warn("면접 일정 수정 알림 발행 실패: {}", e.getMessage());
        }

        dto.setInterviewId(interviewId);
        return dto;
    }

    /**
     * 면접 일정 삭제
     */
    @Transactional
    public void deleteInterview(String userid, Long interviewId) {
        Long employerId = getEmployerIdByUserid(userid);
        if (employerId == null) {
            throw new IllegalStateException("소속된 기업이 없습니다.");
        }

        // 권한 확인
        String checkSql = """
            SELECT COUNT(*) FROM interview_schedule isc
            JOIN job_application ja ON ja.application_id = isc.application_id
            JOIN job_posting jp ON jp.job_id = ja.job_id
            WHERE isc.interview_id = ? AND jp.employer_id = ?
            """;
        
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, interviewId, employerId);
        if (count == null || count == 0) {
            throw new IllegalStateException("해당 면접 일정에 대한 권한이 없습니다.");
        }

        // 지원자 정보 조회 (삭제 전에)
        Long applicationId = null;
        Long candidateMemberId = null;
        String jobTitle = null;
        String companyName = null;
        
        try {
            String infoSql = """
                SELECT ja.application_id, ja.member_id, jp.title as job_title, e.name as company_name
                FROM interview_schedule isc
                JOIN job_application ja ON ja.application_id = isc.application_id
                JOIN job_posting jp ON jp.job_id = ja.job_id
                JOIN employer e ON e.employer_id = jp.employer_id
                WHERE isc.interview_id = ?
                """;
            var info = jdbcTemplate.queryForMap(infoSql, interviewId);
            
            applicationId = ((Number) info.get("application_id")).longValue();
            candidateMemberId = ((Number) info.get("member_id")).longValue();
            jobTitle = (String) info.get("job_title");
            companyName = (String) info.get("company_name");
        } catch (Exception e) {
            log.warn("면접 정보 조회 실패: {}", e.getMessage());
        }

        // 삭제
        jdbcTemplate.update("DELETE FROM interview_schedule WHERE interview_id = ?", interviewId);
        log.info("면접 일정 삭제: {}", interviewId);

        // 지원자에게 알림 발송
        if (candidateMemberId != null) {
            try {
                outboxEventProducer.publishInterviewCanceledEvent(
                        interviewId,
                        applicationId,
                        candidateMemberId,
                        jobTitle,
                        companyName
                );
                log.info("면접 일정 취소 알림 발행: interviewId={}, candidateMemberId={}", interviewId, candidateMemberId);
            } catch (Exception e) {
                log.warn("면접 일정 취소 알림 발행 실패: {}", e.getMessage());
            }
        }
    }

    // ===== Helper Methods =====

    /**
     * 매칭 정보 없을 때 사용할 빈 DTO
     */
    private static JobMatchInfoDTO emptyMatchInfo() {
        JobMatchInfoDTO m = new JobMatchInfoDTO();
        m.setOverallMatchRate(0);
        m.setStackMatchRate(0);
        m.setExperienceMatchRate(0);
        m.setVectorMatchRate(0);
        m.setMatchLevel("LOW");
        m.setRequiredStacks(Collections.emptyList());
        m.setMatchedStacks(Collections.emptyList());
        m.setMissingStacks(Collections.emptyList());
        return m;
    }

    /**
     * userid로 employer_id 조회
     */
    private Long getEmployerIdByUserid(String userid) {
        UserEntity user = userRepository.findByUserid(userid).orElse(null);
        if (user == null) return null;
        
        return employerMemberRepository
                .findFirstByMemberIdAndActiveTrue(user.getId().longValue())
                .map(EmployerMemberEntity::getEmployerId)
                .orElse(null);
    }
}
