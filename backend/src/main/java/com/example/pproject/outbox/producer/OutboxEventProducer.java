package com.example.pproject.outbox.producer;

import com.example.pproject.Constant.NotificationEventType;
import com.example.pproject.Constant.OutboxStatus;
import com.example.pproject.outbox.entity.OutboxEvent;
import com.example.pproject.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 아웃박스 이벤트 생산자
 * - 비즈니스 트랜잭션 내에서 이벤트를 DB에 저장
 * - 트랜잭션 커밋 후 Consumer가 비동기로 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventProducer {

    private final OutboxEventRepository outboxEventRepository;

    /**
     * 아웃박스 이벤트 발행 (현재 트랜잭션에 참여)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publish(String eventType, String aggregateType, Long aggregateId, Map<String, Object> payload) {
        OutboxEvent event = OutboxEvent.builder()
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload != null ? payload : new HashMap<>())
                .status(OutboxStatus.PENDING)
                .nextRunAt(LocalDateTime.now())
                .build();

        OutboxEvent saved = outboxEventRepository.save(event);
        log.debug("아웃박스 이벤트 발행: type={}, aggregateType={}, aggregateId={}, outboxId={}",
                eventType, aggregateType, aggregateId, saved.getId());

        return saved;
    }

    /**
     * 알림 이벤트 발행 헬퍼
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishNotificationEvent(
            NotificationEventType eventType,
            String aggregateType,
            Long aggregateId,
            Integer targetMemberId,
            String title,
            String message,
            String linkUrl,
            Map<String, Object> additionalData
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetMemberId", targetMemberId);
        payload.put("notificationEventType", eventType.name());
        payload.put("title", title);
        payload.put("message", message);
        payload.put("linkUrl", linkUrl);

        if (additionalData != null) {
            payload.putAll(additionalData);
        }

        return publish("NOTIFICATION_" + eventType.name(), aggregateType, aggregateId, payload);
    }

    /**
     * 지원 완료 알림 이벤트
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishApplicationSubmittedEvent(
            Long applicationId,
            Integer candidateMemberId,
            String jobTitle,
            String companyName
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("jobTitle", jobTitle);
        data.put("companyName", companyName);
        data.put("applicationId", applicationId);

        return publishNotificationEvent(
                NotificationEventType.APPLICATION_SUBMITTED,
                "JOB_APPLICATION",
                applicationId,
                candidateMemberId,
                "지원 완료",
                String.format("[%s] %s에 지원이 완료되었습니다.", companyName, jobTitle),
                "/mypage/applications/" + applicationId,
                data
        );
    }

    /**
     * 새 지원자 알림 이벤트 (기업용)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishNewApplicationReceivedEvent(
            Long applicationId,
            Integer employerMemberId,
            Long jobId,
            String jobTitle,
            String candidateName
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("jobId", jobId);
        data.put("jobTitle", jobTitle);
        data.put("candidateName", candidateName);
        data.put("applicationId", applicationId);

        return publishNotificationEvent(
                NotificationEventType.NEW_APPLICATION_RECEIVED,
                "JOB_APPLICATION",
                applicationId,
                employerMemberId,
                "새로운 지원자",
                String.format("[%s] %s님이 지원하셨습니다.", jobTitle, candidateName),
                "/employer/applications/" + applicationId,
                data
        );
    }

    /**
     * 면접 일정 확정 알림 이벤트
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishInterviewScheduledEvent(
            Long interviewId,
            Long applicationId,
            Integer candidateMemberId,
            String jobTitle,
            String companyName,
            LocalDateTime interviewDateTime,
            String location
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("interviewId", interviewId);
        data.put("applicationId", applicationId);
        data.put("jobTitle", jobTitle);
        data.put("companyName", companyName);
        data.put("interviewDateTime", interviewDateTime.toString());
        data.put("location", location);

        return publishNotificationEvent(
                NotificationEventType.INTERVIEW_SCHEDULED,
                "INTERVIEW",
                interviewId,
                candidateMemberId,
                "면접 일정 확정",
                String.format("[%s] 면접이 %s에 예정되어 있습니다.", companyName, interviewDateTime.toLocalDate()),
                "/mypage/interviews/" + interviewId,
                data
        );
    }

    /**
     * 결제 완료 알림 이벤트
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishPaymentCompletedEvent(
            Long paymentId,
            Integer memberId,
            String productName,
            String amount
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", paymentId);
        data.put("productName", productName);
        data.put("amount", amount);

        return publishNotificationEvent(
                NotificationEventType.PAYMENT_COMPLETED,
                "PAYMENT",
                paymentId,
                memberId,
                "결제 완료",
                String.format("%s 결제가 완료되었습니다. (%s)", productName, amount),
                "/mypage/payments/" + paymentId,
                data
        );
    }

    /**
     * 크레딧 충전 알림 이벤트
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishCreditChargedEvent(
            Long ledgerId,
            Integer memberId,
            Integer creditAmount,
            Integer newBalance
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("ledgerId", ledgerId);
        data.put("creditAmount", creditAmount);
        data.put("newBalance", newBalance);

        return publishNotificationEvent(
                NotificationEventType.CREDIT_CHARGED,
                "WALLET",
                ledgerId,
                memberId,
                "크레딧 충전",
                String.format("%d 크레딧이 충전되었습니다. (잔액: %d)", creditAmount, newBalance),
                "/mypage/wallet",
                data
        );
    }

    /**
     * 합격 알림 이벤트
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishHiredEvent(
            Long applicationId,
            Integer candidateMemberId,
            String jobTitle,
            String companyName
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("applicationId", applicationId);
        data.put("jobTitle", jobTitle);
        data.put("companyName", companyName);

        return publishNotificationEvent(
                NotificationEventType.HIRED,
                "JOB_APPLICATION",
                applicationId,
                candidateMemberId,
                "합격을 축하합니다!",
                String.format("[%s] %s 포지션에 최종 합격하셨습니다. 축하드립니다!", companyName, jobTitle),
                "/mypage/applications/" + applicationId,
                data
        );
    }

    // ===== 채용공고 관련 알림 이벤트 =====

    /**
     * 채용공고 등록 완료 알림 이벤트
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishJobPostingCreatedEvent(
            Long jobId,
            Integer memberId,
            String jobTitle,
            String companyName,
            String status
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("jobId", jobId);
        data.put("jobTitle", jobTitle);
        data.put("companyName", companyName);
        data.put("status", status);

        String statusText = "OPEN".equals(status) ? "공개" : "임시저장";

        return publishNotificationEvent(
                NotificationEventType.JOB_POSTING_APPROVED, // 등록 완료로 사용
                "JOB_POSTING",
                jobId,
                memberId,
                "채용공고 등록 완료",
                String.format("[%s] '%s' 채용공고가 %s 상태로 등록되었습니다.", companyName, jobTitle, statusText),
                "/employer/jobs/" + jobId,
                data
        );
    }

    /**
     * 채용공고 수정 완료 알림 이벤트
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishJobPostingUpdatedEvent(
            Long jobId,
            Integer memberId,
            String jobTitle,
            String companyName
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("jobId", jobId);
        data.put("jobTitle", jobTitle);
        data.put("companyName", companyName);

        return publishNotificationEvent(
                NotificationEventType.APPLICATION_STATUS_CHANGED, // 수정으로 사용
                "JOB_POSTING",
                jobId,
                memberId,
                "채용공고 수정 완료",
                String.format("[%s] '%s' 채용공고가 수정되었습니다.", companyName, jobTitle),
                "/employer/jobs/" + jobId,
                data
        );
    }

    /**
     * 채용공고 삭제 알림 이벤트
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishJobPostingDeletedEvent(
            Long jobId,
            Integer memberId,
            String jobTitle,
            String companyName
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("jobId", jobId);
        data.put("jobTitle", jobTitle);
        data.put("companyName", companyName);

        return publishNotificationEvent(
                NotificationEventType.JOB_POSTING_EXPIRED, // 삭제로 사용
                "JOB_POSTING",
                jobId,
                memberId,
                "채용공고 삭제 완료",
                String.format("[%s] '%s' 채용공고가 삭제되었습니다.", companyName, jobTitle),
                "/employer/jobs",
                data
        );
    }

    /**
     * 채용공고 상태 변경 알림 이벤트 (DRAFT → OPEN, OPEN → CLOSED 등)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishJobPostingStatusChangedEvent(
            Long jobId,
            Integer memberId,
            String jobTitle,
            String companyName,
            String oldStatus,
            String newStatus
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("jobId", jobId);
        data.put("jobTitle", jobTitle);
        data.put("companyName", companyName);
        data.put("oldStatus", oldStatus);
        data.put("newStatus", newStatus);

        String statusMessage = getStatusChangeMessage(oldStatus, newStatus);

        return publishNotificationEvent(
                NotificationEventType.APPLICATION_STATUS_CHANGED,
                "JOB_POSTING",
                jobId,
                memberId,
                "채용공고 상태 변경",
                String.format("[%s] '%s' 채용공고가 %s", companyName, jobTitle, statusMessage),
                "/employer/jobs/" + jobId,
                data
        );
    }

    private String getStatusChangeMessage(String oldStatus, String newStatus) {
        if ("DRAFT".equals(oldStatus) && "OPEN".equals(newStatus)) {
            return "공개되었습니다.";
        } else if ("OPEN".equals(oldStatus) && "CLOSED".equals(newStatus)) {
            return "마감되었습니다.";
        } else if ("CLOSED".equals(oldStatus) && "OPEN".equals(newStatus)) {
            return "다시 공개되었습니다.";
        } else {
            return newStatus + " 상태로 변경되었습니다.";
        }
    }

    // ===== 회원 활동 관련 알림 이벤트 =====

    /**
     * 회원정보 수정 완료 알림
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishProfileUpdatedEvent(Integer memberId, String username) {
        Map<String, Object> data = new HashMap<>();
        data.put("memberId", memberId);
        data.put("username", username);

        return publishNotificationEvent(
                NotificationEventType.PROFILE_UPDATED,
                "MEMBER",
                memberId.longValue(),
                memberId,
                "회원정보 수정 완료",
                "회원 정보가 성공적으로 수정되었습니다.",
                "/MyPage",
                data
        );
    }

    /**
     * 비밀번호 변경 완료 알림
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishPasswordChangedEvent(Integer memberId, String username) {
        Map<String, Object> data = new HashMap<>();
        data.put("memberId", memberId);
        data.put("username", username);

        return publishNotificationEvent(
                NotificationEventType.PASSWORD_CHANGED,
                "MEMBER",
                memberId.longValue(),
                memberId,
                "비밀번호 변경 완료",
                "비밀번호가 성공적으로 변경되었습니다. 본인이 아니라면 고객센터에 문의해주세요.",
                "/MyPage",
                data
        );
    }

    // ===== 면접 관련 알림 이벤트 =====

    /**
     * 면접 일정 등록 알림 (지원자에게)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishInterviewCreatedEvent(
            Long interviewId,
            Long applicationId,
            Integer candidateMemberId,
            String jobTitle,
            String companyName,
            String startAt,
            String location
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("interviewId", interviewId);
        data.put("applicationId", applicationId);
        data.put("jobTitle", jobTitle);
        data.put("companyName", companyName);
        data.put("startAt", startAt);
        data.put("location", location);

        return publishNotificationEvent(
                NotificationEventType.INTERVIEW_SCHEDULED,
                "INTERVIEW",
                interviewId,
                candidateMemberId,
                "면접 일정 확정",
                String.format("[%s] %s 포지션 면접 일정이 등록되었습니다. 일시: %s", companyName, jobTitle, startAt),
                "/mypage/applications/" + applicationId,
                data
        );
    }

    /**
     * 면접 일정 수정 알림 (지원자에게)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishInterviewUpdatedEvent(
            Long interviewId,
            Long applicationId,
            Integer candidateMemberId,
            String jobTitle,
            String companyName,
            String startAt
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("interviewId", interviewId);
        data.put("applicationId", applicationId);
        data.put("jobTitle", jobTitle);
        data.put("companyName", companyName);
        data.put("startAt", startAt);

        return publishNotificationEvent(
                NotificationEventType.INTERVIEW_SCHEDULED,
                "INTERVIEW",
                interviewId,
                candidateMemberId,
                "면접 일정 변경",
                String.format("[%s] %s 포지션 면접 일정이 변경되었습니다. 새로운 일시: %s", companyName, jobTitle, startAt),
                "/mypage/applications/" + applicationId,
                data
        );
    }

    /**
     * 면접 일정 취소 알림 (지원자에게)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishInterviewCanceledEvent(
            Long interviewId,
            Long applicationId,
            Integer candidateMemberId,
            String jobTitle,
            String companyName
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("interviewId", interviewId);
        data.put("applicationId", applicationId);
        data.put("jobTitle", jobTitle);
        data.put("companyName", companyName);

        return publishNotificationEvent(
                NotificationEventType.INTERVIEW_CANCELED,
                "INTERVIEW",
                interviewId,
                candidateMemberId,
                "면접 일정 취소",
                String.format("[%s] %s 포지션 면접 일정이 취소되었습니다.", companyName, jobTitle),
                "/mypage/applications/" + applicationId,
                data
        );
    }

    /**
     * 지원 상태 변경 알림 (지원자에게)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishApplicationStatusChangedEvent(
            Long applicationId,
            Integer candidateMemberId,
            String jobTitle,
            String companyName,
            String newStatus
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("applicationId", applicationId);
        data.put("jobTitle", jobTitle);
        data.put("companyName", companyName);
        data.put("newStatus", newStatus);

        String statusMessage = getApplicationStatusMessage(newStatus);

        return publishNotificationEvent(
                NotificationEventType.APPLICATION_STATUS_CHANGED,
                "JOB_APPLICATION",
                applicationId,
                candidateMemberId,
                "지원 상태 변경",
                String.format("[%s] %s 포지션 지원 상태가 '%s'(으)로 변경되었습니다.", companyName, jobTitle, statusMessage),
                "/mypage/applications/" + applicationId,
                data
        );
    }

    private String getApplicationStatusMessage(String status) {
        return switch (status) {
            case "REVIEWING" -> "서류 검토중";
            case "INTERVIEW" -> "면접 예정";
            case "OFFERED" -> "합격";
            case "HIRED" -> "채용 완료";
            case "REJECTED" -> "불합격";
            default -> status;
        };
    }
}
