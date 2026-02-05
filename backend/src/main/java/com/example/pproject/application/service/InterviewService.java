package com.example.pproject.application.service;

import com.example.pproject.Constant.ApplicationStatus;
import com.example.pproject.Constant.InterviewMethod;
import com.example.pproject.Constant.InterviewResponseType;
import com.example.pproject.Constant.InterviewStage;
import com.example.pproject.Constant.InterviewStatus;
import com.example.pproject.application.dto.InterviewCreateRequest;
import com.example.pproject.application.dto.InterviewDTO;
import com.example.pproject.application.dto.InterviewRespondRequest;
import com.example.pproject.application.entity.InterviewResponseEntity;
import com.example.pproject.application.entity.InterviewSchedule;
import com.example.pproject.application.entity.JobApplication;
import com.example.pproject.application.repository.InterviewResponseRepository;
import com.example.pproject.application.repository.InterviewScheduleRepository;
import com.example.pproject.application.repository.JobApplicationRepository;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

    private final InterviewScheduleRepository interviewScheduleRepository;
    private final InterviewResponseRepository interviewResponseRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final EmployerMemberRepository employerMemberRepository;
    private final EmployerRepository employerRepository;
    private final NotificationService notificationService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 면접 일정 생성 (기업용)
     */
    @Transactional
    public Long createInterview(InterviewCreateRequest request, Long userId) {
        // 지원 정보 조회
        JobApplication application = jobApplicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("지원 정보를 찾을 수 없습니다."));

        // 권한 검증 (해당 기업의 채용공고인지)
        Long employerId = application.getJob().getEmployerId();
        validateEmployerAccess(userId, employerId);

        // 시간 유효성 검증
        if (request.getEndAt().isBefore(request.getStartAt())) {
            throw new IllegalArgumentException("종료 시간은 시작 시간 이후여야 합니다.");
        }

        // 면접 일정 생성
        InterviewSchedule interview = InterviewSchedule.builder()
                .application(application)
                .stage(request.getStage())
                .method(request.getMethod())
                .location(request.getLocation())
                .meetingUrl(request.getMeetingUrl())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .createdByMemberId(userId)
                .build();

        interviewScheduleRepository.save(interview);

        // 지원 상태를 INTERVIEW로 변경
        updateApplicationStatusToInterview(application);

        log.info("면접 일정 생성: interviewId={}, applicationId={}, stage={}",
                interview.getId(), application.getId(), request.getStage());

        // 지원자에게 면접 일정 알림 발송
        sendInterviewScheduledNotification(interview, application);

        return interview.getId();
    }

    /**
     * 면접 응답 (지원자용)
     */
    @Transactional
    public void respondToInterview(Long interviewId, InterviewRespondRequest request, Long userId) {
        InterviewSchedule interview = interviewScheduleRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("면접 일정을 찾을 수 없습니다."));

        // 권한 검증 (해당 지원자인지)
        Long applicantId = interview.getApplication().getMember().getId().longValue();
        if (!applicantId.equals(userId)) {
            throw new SecurityException("본인의 면접 일정만 응답할 수 있습니다.");
        }

        // 상태 검증 (PROPOSED 상태만 응답 가능)
        if (interview.getStatus() != InterviewStatus.PROPOSED) {
            throw new IllegalStateException("제안된 면접 일정에만 응답할 수 있습니다.");
        }

        // 응답 기록 저장
        InterviewResponseEntity response = InterviewResponseEntity.builder()
                .interview(interview)
                .response(request.getResponse())
                .message(request.getMessage())
                .build();
        interviewResponseRepository.save(response);

        // 면접 상태 업데이트
        switch (request.getResponse()) {
            case ACCEPT:
                interview.confirm();
                log.info("면접 수락: interviewId={}", interviewId);
                // 기업에게 면접 수락 알림 발송
                sendInterviewResponseNotification(interview, "INTERVIEW_ACCEPTED");
                break;
            case DECLINE:
                interview.cancel();
                log.info("면접 거절: interviewId={}", interviewId);
                // 기업에게 면접 거절 알림 발송
                sendInterviewResponseNotification(interview, "INTERVIEW_DECLINED");
                break;
            case REQUEST_CHANGE:
                // 일정 변경 요청은 상태 유지, 기업에서 재제안 필요
                log.info("면접 일정 변경 요청: interviewId={}, message={}", interviewId, request.getMessage());
                // 기업에게 일정 변경 요청 알림 발송
                sendInterviewResponseNotification(interview, "INTERVIEW_RESCHEDULE_REQUEST");
                break;
        }
    }

    /**
     * 면접 취소 (기업/지원자 모두 가능)
     */
    @Transactional
    public void cancelInterview(Long interviewId, Long userId) {
        InterviewSchedule interview = interviewScheduleRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("면접 일정을 찾을 수 없습니다."));

        // 권한 검증
        Long applicantId = interview.getApplication().getMember().getId().longValue();
        Long employerId = interview.getApplication().getJob().getEmployerId();

        boolean isApplicant = applicantId.equals(userId);
        boolean isEmployer = isEmployerMember(userId, employerId);

        if (!isApplicant && !isEmployer) {
            throw new SecurityException("면접 취소 권한이 없습니다.");
        }

        // 이미 완료된 면접은 취소 불가
        if (interview.getStatus() == InterviewStatus.DONE) {
            throw new IllegalStateException("완료된 면접은 취소할 수 없습니다.");
        }

        interview.cancel();
        log.info("면접 취소: interviewId={}, canceledBy={}", interviewId, userId);

        // 상대방에게 면접 취소 알림 발송
        if (isApplicant) {
            // 지원자가 취소한 경우 -> 기업에게 알림
            sendInterviewCancelledNotificationToEmployer(interview, userId);
        } else {
            // 기업이 취소한 경우 -> 지원자에게 알림
            sendInterviewCancelledNotificationToApplicant(interview);
        }
    }

    /**
     * 면접 완료 처리 (기업용)
     */
    @Transactional
    public void completeInterview(Long interviewId, Long userId) {
        InterviewSchedule interview = interviewScheduleRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("면접 일정을 찾을 수 없습니다."));

        // 권한 검증
        Long employerId = interview.getApplication().getJob().getEmployerId();
        validateEmployerAccess(userId, employerId);

        // 확정된 면접만 완료 처리 가능
        if (interview.getStatus() != InterviewStatus.CONFIRMED) {
            throw new IllegalStateException("확정된 면접만 완료 처리할 수 있습니다.");
        }

        interview.complete();
        log.info("면접 완료: interviewId={}", interviewId);
    }

    /**
     * 지원자의 면접 일정 목록 조회 (JdbcTemplate 기반 - JPA 조인 이슈 해결)
     */
    public List<InterviewDTO> getMyInterviews(Long memberId) {
        log.debug("지원자 면접 일정 조회: memberId={}", memberId);
        
        String sql = """
            SELECT 
                isc.interview_id,
                isc.application_id,
                ja.member_id as candidate_id,
                m.name as candidate_name,
                jp.job_id,
                jp.title as job_title,
                e.name as company_name,
                r.title as resume_title,
                isc.stage,
                isc.method,
                isc.location,
                isc.meeting_url,
                isc.start_at,
                isc.end_at,
                isc.status,
                isc.created_at
            FROM interview_schedule isc
            JOIN job_application ja ON ja.application_id = isc.application_id
            JOIN job_posting jp ON jp.job_id = ja.job_id
            JOIN employer e ON e.employer_id = jp.employer_id
            JOIN member m ON m.member_id = ja.member_id
            LEFT JOIN resume r ON r.resume_id = ja.resume_id
            WHERE ja.member_id = ?
            ORDER BY isc.start_at DESC
            """;
        
        try {
            List<InterviewDTO> interviews = jdbcTemplate.query(sql, (rs, rowNum) -> 
                InterviewDTO.builder()
                    .interviewId(rs.getLong("interview_id"))
                    .applicationId(rs.getLong("application_id"))
                    .candidateId(rs.getLong("candidate_id"))
                    .candidateName(rs.getString("candidate_name"))
                    .jobId(rs.getLong("job_id"))
                    .jobTitle(rs.getString("job_title"))
                    .companyName(rs.getString("company_name"))
                    .resumeTitle(rs.getString("resume_title"))
                    .stage(InterviewStage.fromCode(rs.getString("stage")))
                    .method(InterviewMethod.valueOf(rs.getString("method")))
                    .location(rs.getString("location"))
                    .meetingUrl(rs.getString("meeting_url"))
                    .startAt(rs.getTimestamp("start_at") != null ? 
                            rs.getTimestamp("start_at").toLocalDateTime() : null)
                    .endAt(rs.getTimestamp("end_at") != null ? 
                            rs.getTimestamp("end_at").toLocalDateTime() : null)
                    .status(InterviewStatus.valueOf(rs.getString("status")))
                    .createdAt(rs.getTimestamp("created_at") != null ? 
                            rs.getTimestamp("created_at").toLocalDateTime() : null)
                    .build(),
                memberId);
            
            log.info("지원자 면접 일정 조회 완료: memberId={}, count={}", memberId, interviews.size());
            return interviews;
        } catch (Exception e) {
            log.error("지원자 면접 일정 조회 실패: memberId={}, error={}", memberId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 지원자의 다가오는 면접 일정 조회 (JdbcTemplate 기반)
     */
    public List<InterviewDTO> getUpcomingInterviews(Long memberId) {
        log.debug("지원자 다가오는 면접 일정 조회: memberId={}", memberId);
        
        String sql = """
            SELECT 
                isc.interview_id,
                isc.application_id,
                ja.member_id as candidate_id,
                m.name as candidate_name,
                jp.job_id,
                jp.title as job_title,
                e.name as company_name,
                r.title as resume_title,
                isc.stage,
                isc.method,
                isc.location,
                isc.meeting_url,
                isc.start_at,
                isc.end_at,
                isc.status,
                isc.created_at
            FROM interview_schedule isc
            JOIN job_application ja ON ja.application_id = isc.application_id
            JOIN job_posting jp ON jp.job_id = ja.job_id
            JOIN employer e ON e.employer_id = jp.employer_id
            JOIN member m ON m.member_id = ja.member_id
            LEFT JOIN resume r ON r.resume_id = ja.resume_id
            WHERE ja.member_id = ?
              AND isc.status IN ('PROPOSED', 'CONFIRMED')
              AND isc.start_at > NOW()
            ORDER BY isc.start_at ASC
            """;
        
        try {
            List<InterviewDTO> interviews = jdbcTemplate.query(sql, (rs, rowNum) -> 
                InterviewDTO.builder()
                    .interviewId(rs.getLong("interview_id"))
                    .applicationId(rs.getLong("application_id"))
                    .candidateId(rs.getLong("candidate_id"))
                    .candidateName(rs.getString("candidate_name"))
                    .jobId(rs.getLong("job_id"))
                    .jobTitle(rs.getString("job_title"))
                    .companyName(rs.getString("company_name"))
                    .resumeTitle(rs.getString("resume_title"))
                    .stage(InterviewStage.fromCode(rs.getString("stage")))
                    .method(InterviewMethod.valueOf(rs.getString("method")))
                    .location(rs.getString("location"))
                    .meetingUrl(rs.getString("meeting_url"))
                    .startAt(rs.getTimestamp("start_at") != null ? 
                            rs.getTimestamp("start_at").toLocalDateTime() : null)
                    .endAt(rs.getTimestamp("end_at") != null ? 
                            rs.getTimestamp("end_at").toLocalDateTime() : null)
                    .status(InterviewStatus.valueOf(rs.getString("status")))
                    .createdAt(rs.getTimestamp("created_at") != null ? 
                            rs.getTimestamp("created_at").toLocalDateTime() : null)
                    .build(),
                memberId);
            
            log.info("지원자 다가오는 면접 일정 조회 완료: memberId={}, count={}", memberId, interviews.size());
            return interviews;
        } catch (Exception e) {
            log.error("지원자 다가오는 면접 일정 조회 실패: memberId={}, error={}", memberId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 기업의 면접 일정 목록 조회
     */
    public List<InterviewDTO> getEmployerInterviews(Long userId) {
        Long employerId = getEmployerIdByUserId(userId);
        List<InterviewSchedule> interviews = interviewScheduleRepository.findByEmployerId(employerId);
        return interviews.stream()
                .map(this::toInterviewDTOWithCompanyName)
                .collect(Collectors.toList());
    }

    /**
     * 기업의 다가오는 면접 일정 조회
     */
    public List<InterviewDTO> getEmployerUpcomingInterviews(Long userId) {
        Long employerId = getEmployerIdByUserId(userId);
        List<InterviewStatus> statuses = Arrays.asList(InterviewStatus.PROPOSED, InterviewStatus.CONFIRMED);
        List<InterviewSchedule> interviews = interviewScheduleRepository.findUpcomingByEmployerId(
                employerId, statuses, LocalDateTime.now());
        return interviews.stream()
                .map(this::toInterviewDTOWithCompanyName)
                .collect(Collectors.toList());
    }

    /**
     * 특정 지원의 면접 일정 조회
     */
    public List<InterviewDTO> getInterviewsByApplication(Long applicationId, Long userId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("지원 정보를 찾을 수 없습니다."));

        // 권한 검증 (지원자 또는 해당 기업)
        Long applicantId = application.getMember().getId().longValue();
        Long employerId = application.getJob().getEmployerId();

        if (!applicantId.equals(userId) && !isEmployerMember(userId, employerId)) {
            throw new SecurityException("조회 권한이 없습니다.");
        }

        List<InterviewSchedule> interviews = interviewScheduleRepository.findByApplicationIdOrderByStartAtDesc(applicationId);
        return interviews.stream()
                .map(this::toInterviewDTOWithCompanyName)
                .collect(Collectors.toList());
    }

    /**
     * 면접 상세 조회
     */
    public InterviewDTO getInterview(Long interviewId, Long userId) {
        InterviewSchedule interview = interviewScheduleRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("면접 일정을 찾을 수 없습니다."));

        // 권한 검증
        Long applicantId = interview.getApplication().getMember().getId().longValue();
        Long employerId = interview.getApplication().getJob().getEmployerId();

        if (!applicantId.equals(userId) && !isEmployerMember(userId, employerId)) {
            throw new SecurityException("조회 권한이 없습니다.");
        }

        return toInterviewDTOWithCompanyName(interview);
    }

    // ===== Helper Methods =====

    private InterviewDTO toInterviewDTOWithCompanyName(InterviewSchedule interview) {
        Long employerId = interview.getApplication().getJob().getEmployerId();
        String companyName = employerRepository.findById(employerId)
                .map(EmployerEntity::getName)
                .orElse("알 수 없음");
        return InterviewDTO.from(interview, companyName);
    }

    private void validateEmployerAccess(Long userId, Long employerId) {
        if (!isEmployerMember(userId, employerId)) {
            throw new SecurityException("해당 기업의 면접 관리 권한이 없습니다.");
        }
    }

    private boolean isEmployerMember(Long userId, Long employerId) {
        return employerMemberRepository.findFirstByMemberIdAndActiveTrue(userId)
                .map(em -> em.getEmployerId().equals(employerId))
                .orElse(false);
    }

    private Long getEmployerIdByUserId(Long userId) {
        return employerMemberRepository.findFirstByMemberIdAndActiveTrue(userId)
                .map(EmployerMemberEntity::getEmployerId)
                .orElseThrow(() -> new IllegalStateException("소속된 기업이 없습니다."));
    }

    private void updateApplicationStatusToInterview(JobApplication application) {
        // 지원 상태가 SUBMITTED 또는 VIEWED인 경우에만 INTERVIEW로 변경
        if (application.getStatus() == ApplicationStatus.SUBMITTED ||
            application.getStatus() == ApplicationStatus.VIEWED) {
            // JobApplication에 상태 변경 메서드가 필요하면 추가
            // 현재는 직접 변경 불가하므로 로그만 남김
            log.info("지원 상태 INTERVIEW 변경 필요: applicationId={}", application.getId());
        }
    }

    // ===== Notification Helper Methods =====

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 면접 일정 생성 시 지원자에게 알림 발송
     */
    private void sendInterviewScheduledNotification(InterviewSchedule interview, JobApplication application) {
        try {
            Long applicantId = application.getMember().getId().longValue();
            Long employerId = application.getJob().getEmployerId();
            String companyName = employerRepository.findById(employerId)
                    .map(EmployerEntity::getName)
                    .orElse("기업");
            String jobTitle = application.getJob().getTitle();

            Map<String, Object> data = new HashMap<>();
            data.put("interviewId", interview.getId());
            data.put("applicationId", application.getId());
            data.put("companyName", companyName);
            data.put("jobTitle", jobTitle);
            data.put("stage", interview.getStage().name());
            data.put("method", interview.getMethod().name());
            data.put("startAt", interview.getStartAt().format(DATE_FORMATTER));
            data.put("endAt", interview.getEndAt().format(DATE_FORMATTER));
            data.put("location", interview.getLocation());
            data.put("meetingUrl", interview.getMeetingUrl());
            data.put("title", companyName + "에서 면접 일정을 제안했습니다");
            data.put("message", jobTitle + " 포지션의 " + interview.getStage().name() + " 면접이 " + interview.getStartAt().format(DATE_FORMATTER) + "에 예정되어 있습니다.");

            notificationService.sendNotification(applicantId, "INTERVIEW_SCHEDULED", data);
            log.info("면접 일정 알림 발송: applicantId={}, interviewId={}", applicantId, interview.getId());
        } catch (Exception e) {
            log.error("면접 일정 알림 발송 실패: interviewId={}, error={}", interview.getId(), e.getMessage());
        }
    }

    /**
     * 면접 응답 시 기업에게 알림 발송
     */
    private void sendInterviewResponseNotification(InterviewSchedule interview, String eventType) {
        try {
            Long employerId = interview.getApplication().getJob().getEmployerId();
            // 기업 담당자(면접 생성자)에게 알림
            Long creatorId = interview.getCreatedByMemberId();
            if (creatorId == null) {
                log.warn("면접 생성자 정보가 없어 알림 발송 생략: interviewId={}", interview.getId());
                return;
            }

            JobApplication application = interview.getApplication();
            String applicantName = application.getMember().getUsername();
            String jobTitle = application.getJob().getTitle();
            String companyName = employerRepository.findById(employerId)
                    .map(EmployerEntity::getName)
                    .orElse("기업");

            Map<String, Object> data = new HashMap<>();
            data.put("interviewId", interview.getId());
            data.put("applicationId", application.getId());
            data.put("applicantName", applicantName);
            data.put("jobTitle", jobTitle);
            data.put("stage", interview.getStage().name());
            data.put("startAt", interview.getStartAt().format(DATE_FORMATTER));

            String title;
            String message;
            switch (eventType) {
                case "INTERVIEW_ACCEPTED":
                    title = applicantName + "님이 면접 일정을 수락했습니다";
                    message = jobTitle + " 포지션의 면접 일정(" + interview.getStartAt().format(DATE_FORMATTER) + ")이 확정되었습니다.";
                    break;
                case "INTERVIEW_DECLINED":
                    title = applicantName + "님이 면접 일정을 거절했습니다";
                    message = jobTitle + " 포지션의 면접 일정이 거절되었습니다. 새로운 일정을 제안해 주세요.";
                    break;
                case "INTERVIEW_RESCHEDULE_REQUEST":
                    title = applicantName + "님이 면접 일정 변경을 요청했습니다";
                    message = jobTitle + " 포지션의 면접 일정 변경 요청이 있습니다. 새로운 일정을 제안해 주세요.";
                    break;
                default:
                    title = "면접 관련 알림";
                    message = "면접 관련 업데이트가 있습니다.";
            }

            data.put("title", title);
            data.put("message", message);

            notificationService.sendNotification(creatorId, eventType, data);
            log.info("면접 응답 알림 발송: creatorId={}, eventType={}, interviewId={}", creatorId, eventType, interview.getId());
        } catch (Exception e) {
            log.error("면접 응답 알림 발송 실패: interviewId={}, eventType={}, error={}", interview.getId(), eventType, e.getMessage());
        }
    }

    /**
     * 면접 취소 시 기업에게 알림 발송 (지원자가 취소한 경우)
     */
    private void sendInterviewCancelledNotificationToEmployer(InterviewSchedule interview, Long applicantId) {
        try {
            Long creatorId = interview.getCreatedByMemberId();
            if (creatorId == null) {
                log.warn("면접 생성자 정보가 없어 알림 발송 생략: interviewId={}", interview.getId());
                return;
            }

            JobApplication application = interview.getApplication();
            String applicantName = application.getMember().getUsername();
            String jobTitle = application.getJob().getTitle();

            Map<String, Object> data = new HashMap<>();
            data.put("interviewId", interview.getId());
            data.put("applicationId", application.getId());
            data.put("applicantName", applicantName);
            data.put("jobTitle", jobTitle);
            data.put("startAt", interview.getStartAt().format(DATE_FORMATTER));
            data.put("title", applicantName + "님이 면접을 취소했습니다");
            data.put("message", jobTitle + " 포지션의 면접(" + interview.getStartAt().format(DATE_FORMATTER) + ")이 지원자에 의해 취소되었습니다.");

            notificationService.sendNotification(creatorId, "INTERVIEW_CANCELLED", data);
            log.info("면접 취소 알림 발송 (기업): creatorId={}, interviewId={}", creatorId, interview.getId());
        } catch (Exception e) {
            log.error("면접 취소 알림 발송 실패: interviewId={}, error={}", interview.getId(), e.getMessage());
        }
    }

    /**
     * 면접 취소 시 지원자에게 알림 발송 (기업이 취소한 경우)
     */
    private void sendInterviewCancelledNotificationToApplicant(InterviewSchedule interview) {
        try {
            JobApplication application = interview.getApplication();
            Long applicantId = application.getMember().getId().longValue();
            Long employerId = application.getJob().getEmployerId();
            String companyName = employerRepository.findById(employerId)
                    .map(EmployerEntity::getName)
                    .orElse("기업");
            String jobTitle = application.getJob().getTitle();

            Map<String, Object> data = new HashMap<>();
            data.put("interviewId", interview.getId());
            data.put("applicationId", application.getId());
            data.put("companyName", companyName);
            data.put("jobTitle", jobTitle);
            data.put("startAt", interview.getStartAt().format(DATE_FORMATTER));
            data.put("title", companyName + "에서 면접 일정을 취소했습니다");
            data.put("message", jobTitle + " 포지션의 면접(" + interview.getStartAt().format(DATE_FORMATTER) + ")이 기업에 의해 취소되었습니다.");

            notificationService.sendNotification(applicantId, "INTERVIEW_CANCELLED", data);
            log.info("면접 취소 알림 발송 (지원자): applicantId={}, interviewId={}", applicantId, interview.getId());
        } catch (Exception e) {
            log.error("면접 취소 알림 발송 실패: interviewId={}, error={}", interview.getId(), e.getMessage());
        }
    }
}
