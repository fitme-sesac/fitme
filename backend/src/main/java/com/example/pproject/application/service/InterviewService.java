package com.example.pproject.application.service;

import com.example.pproject.Constant.ApplicationStatus;
import com.example.pproject.Constant.InterviewResponseType;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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
                break;
            case DECLINE:
                interview.cancel();
                log.info("면접 거절: interviewId={}", interviewId);
                break;
            case REQUEST_CHANGE:
                // 일정 변경 요청은 상태 유지, 기업에서 재제안 필요
                log.info("면접 일정 변경 요청: interviewId={}, message={}", interviewId, request.getMessage());
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
     * 지원자의 면접 일정 목록 조회
     */
    public List<InterviewDTO> getMyInterviews(Long userId) {
        List<InterviewSchedule> interviews = interviewScheduleRepository.findByMemberId(userId);
        return interviews.stream()
                .map(this::toInterviewDTOWithCompanyName)
                .collect(Collectors.toList());
    }

    /**
     * 지원자의 다가오는 면접 일정 조회
     */
    public List<InterviewDTO> getUpcomingInterviews(Long userId) {
        List<InterviewSchedule> interviews = interviewScheduleRepository.findUpcomingByMemberIdAndStatus(
                userId, InterviewStatus.CONFIRMED, LocalDateTime.now());
        return interviews.stream()
                .map(this::toInterviewDTOWithCompanyName)
                .collect(Collectors.toList());
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
}
