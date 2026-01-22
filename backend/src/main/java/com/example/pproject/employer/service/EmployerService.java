package com.example.pproject.employer.service;

import com.example.pproject.common.util.ArrayStringUtil;
import com.example.pproject.employer.dto.*;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobRepository;
import com.example.pproject.outbox.producer.OutboxEventProducer;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
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
    private final JdbcTemplate jdbcTemplate;
    private final OutboxEventProducer outboxEventProducer;

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
                .techStack(ArrayStringUtil.cleanArrayString(entity.getTechStack())) // {} 제거
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
            String sql = """
                SELECT 
                    ac.campaign_id,
                    ac.job_id,
                    jp.title as job_title,
                    ac.status,
                    ac.cpc_bid,
                    ac.daily_budget,
                    COALESCE((SELECT COUNT(*) FROM ad_click_event ace WHERE ace.campaign_id = ac.campaign_id), 0) as clicks
                FROM ad_campaign ac
                JOIN job_posting jp ON jp.job_id = ac.job_id
                WHERE ac.employer_id = ?
                ORDER BY ac.created_at DESC
                LIMIT 10
                """;

            List<AdStatsDTO.CampaignDTO> campaigns = jdbcTemplate.query(sql,
                    (rs, rowNum) -> AdStatsDTO.CampaignDTO.builder()
                            .campaignId(rs.getLong("campaign_id"))
                            .jobId(rs.getLong("job_id"))
                            .jobTitle(rs.getString("job_title"))
                            .status(rs.getString("status"))
                            .cpcBid(rs.getInt("cpc_bid"))
                            .dailyBudget(rs.getInt("daily_budget"))
                            .clicks(rs.getInt("clicks"))
                            .build(),
                    employerId);

            int activeCampaigns = (int) campaigns.stream().filter(c -> "ACTIVE".equals(c.getStatus())).count();
            long totalClicks = campaigns.stream().mapToLong(AdStatsDTO.CampaignDTO::getClicks).sum();
            long totalSpent = campaigns.stream().mapToLong(c -> (long) c.getClicks() * c.getCpcBid()).sum();

            return AdStatsDTO.builder()
                    .activeCampaigns(activeCampaigns)
                    .totalClicks(totalClicks)
                    .totalImpressions(totalClicks * 100) // 임시: 클릭 * 100 = 노출 (CTR 1% 가정)
                    .totalSpent(totalSpent)
                    .ctr(1.0)
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

    // ===== 지원자 관리 =====

    /**
     * 지원자 목록 조회
     */
    public ApplicantListDTO getApplicants(String userid, String status) {
        Long employerId = getEmployerIdByUserid(userid);
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
                """);

            List<Object> params = new ArrayList<>();
            params.add(employerId);

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
            
            Integer candidateMemberId = ((Number) info.get("member_id")).intValue();
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
            
            Integer candidateMemberId = ((Number) info.get("member_id")).intValue();
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
            Integer candidateMemberId = ((Number) info.get("member_id")).intValue();
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
        Integer candidateMemberId = null;
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
            candidateMemberId = ((Number) info.get("member_id")).intValue();
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
