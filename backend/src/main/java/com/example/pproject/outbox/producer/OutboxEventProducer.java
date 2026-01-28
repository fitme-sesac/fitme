package com.example.pproject.outbox.producer;

import com.example.pproject.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Outbox 패턴 기반 이벤트 발행 클래스
 * 
 * ========================================
 * ERD 테이블: outbox_event (39번)
 * ========================================
 * 
 * 역할:
 * - 비즈니스 로직에서 호출하여 이벤트를 Outbox 테이블에 저장
 * - 동일 트랜잭션 내에서 저장되어 데이터 일관성 보장
 * 
 * ERD 매핑 (publishEvent 호출 시):
 * - outbox_id      <- 자동 생성 (IDENTITY)
 * - event_type     <- 메서드별 이벤트 타입 (예: JOB_POSTING_CREATED)
 * - aggregate_type <- 집합체 타입 (예: JOB_POSTING)
 * - aggregate_id   <- 엔티티 PK (예: job_id)
 * - payload        <- Map -> JSON 변환 (JSONB)
 * - status         <- 'PENDING' (DEFAULT)
 * - retry_count    <- 0 (DEFAULT)
 * - next_run_at    <- now() (DEFAULT)
 * - created_at     <- now() (DEFAULT)
 * - updated_at     <- now() (DEFAULT)
 * 
 * Consumer에서 처리:
 * - OutboxEventConsumer가 PENDING 이벤트를 폴링하여 알림 발송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventProducer {

    private final OutboxService outboxService;

    // ==================== 채용공고 이벤트 ====================

    /**
     * 채용공고 등록 이벤트 발행
     * 
     * ERD 매핑:
     * - aggregate_type = "JOB_POSTING"
     * - aggregate_id = job_posting.job_id
     * - event_type = "JOB_POSTING_CREATED"
     * 
     * @param jobId job_posting.job_id
     * @param userId member.member_id (등록한 사용자)
     * @param jobTitle job_posting.title
     * @param companyName employer.name
     * @param status job_posting.status
     */
    public void publishJobPostingCreatedEvent(Long jobId, Long userId, String jobTitle, 
                                               String companyName, String status) {
        log.info("[Outbox] 채용공고 등록 이벤트 - jobId: {}, userId: {}, title: {}, company: {}, status: {}", 
                jobId, userId, jobTitle, companyName, status);

        Map<String, Object> payload = new HashMap<>();
        payload.put("jobId", jobId);
        payload.put("userId", userId);
        payload.put("title", jobTitle);
        payload.put("companyName", companyName);
        payload.put("status", status);

        outboxService.publishEvent("JOB_POSTING_CREATED", "JOB_POSTING", jobId, payload);
    }

    /**
     * 채용공고 수정 이벤트 발행
     * 
     * @param jobId job_posting.job_id
     * @param userId member.member_id
     * @param jobTitle job_posting.title
     * @param companyName employer.name
     */
    public void publishJobPostingUpdatedEvent(Long jobId, Long userId, String jobTitle, String companyName) {
        log.info("[Outbox] 채용공고 수정 이벤트 - jobId: {}, userId: {}, title: {}, company: {}", 
                jobId, userId, jobTitle, companyName);

        Map<String, Object> payload = new HashMap<>();
        payload.put("jobId", jobId);
        payload.put("userId", userId);
        payload.put("title", jobTitle);
        payload.put("companyName", companyName);

        outboxService.publishEvent("JOB_POSTING_UPDATED", "JOB_POSTING", jobId, payload);
    }

    /**
     * 채용공고 상태 변경 이벤트 발행
     * 
     * @param jobId job_posting.job_id
     * @param userId member.member_id
     * @param jobTitle job_posting.title
     * @param companyName employer.name
     * @param oldStatus 이전 상태
     * @param newStatus 새 상태 (DRAFT/OPEN/CLOSED)
     */
    public void publishJobPostingStatusChangedEvent(Long jobId, Long userId, String jobTitle, 
                                                     String companyName, String oldStatus, String newStatus) {
        log.info("[Outbox] 채용공고 상태 변경 이벤트 - jobId: {}, userId: {}, title: {}, {} -> {}", 
                jobId, userId, jobTitle, oldStatus, newStatus);

        Map<String, Object> payload = new HashMap<>();
        payload.put("jobId", jobId);
        payload.put("userId", userId);
        payload.put("title", jobTitle);
        payload.put("companyName", companyName);
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);

        outboxService.publishEvent("JOB_POSTING_STATUS_CHANGED", "JOB_POSTING", jobId, payload);
    }

    /**
     * 채용공고 삭제 이벤트 발행
     * 
     * @param jobId job_posting.job_id
     * @param userId member.member_id
     * @param jobTitle job_posting.title
     * @param companyName employer.name
     */
    public void publishJobPostingDeletedEvent(Long jobId, Long userId, String jobTitle, String companyName) {
        log.info("[Outbox] 채용공고 삭제 이벤트 - jobId: {}, userId: {}, title: {}, company: {}", 
                jobId, userId, jobTitle, companyName);

        Map<String, Object> payload = new HashMap<>();
        payload.put("jobId", jobId);
        payload.put("userId", userId);
        payload.put("title", jobTitle);
        payload.put("companyName", companyName);

        outboxService.publishEvent("JOB_POSTING_DELETED", "JOB_POSTING", jobId, payload);
    }

    // ==================== 지원 이벤트 ====================

    /**
     * 지원서 제출 이벤트 발행
     * 
     * ERD 매핑:
     * - aggregate_type = "APPLICATION"
     * - aggregate_id = job_application.application_id
     * - event_type = "APPLICATION_SUBMITTED"
     * 
     * @param applicationId job_application.application_id
     * @param jobId job_posting.job_id
     * @param userId member.member_id (지원자)
     * @param jobTitle job_posting.title
     * @param companyName employer.name
     */
    public void publishApplicationSubmittedEvent(Long applicationId, Long jobId, Integer userId, 
                                                  String jobTitle, String companyName) {
        log.info("[Outbox] 지원서 제출 이벤트 - applicationId: {}, jobId: {}, userId: {}, title: {}", 
                applicationId, jobId, userId, jobTitle);

        Map<String, Object> payload = new HashMap<>();
        payload.put("applicationId", applicationId);
        payload.put("jobId", jobId);
        payload.put("userId", userId != null ? userId.longValue() : null);
        payload.put("jobTitle", jobTitle);
        payload.put("companyName", companyName);

        outboxService.publishEvent("APPLICATION_SUBMITTED", "APPLICATION", applicationId, payload);
    }

    /**
     * 지원서 상태 변경 이벤트 발행 (EmployerService용)
     * 
     * ERD 매핑:
     * - aggregate_type = "APPLICATION"
     * - aggregate_id = job_application.application_id
     * - event_type = "APPLICATION_STATUS_CHANGED"
     * 
     * @param applicationId job_application.application_id
     * @param candidateMemberId member.member_id (지원자)
     * @param jobTitle job_posting.title
     * @param companyName employer.name
     * @param newStatus 새 상태 (VIEWED/INTERVIEW/HIRED/REJECTED/CANCELED)
     */
    public void publishApplicationStatusChangedEvent(Long applicationId, Long candidateMemberId,
                                                      String jobTitle, String companyName, String newStatus) {
        log.info("[Outbox] 지원서 상태 변경 이벤트 - applicationId: {}, candidateMemberId: {}, jobTitle: {}, status: {}", 
                applicationId, candidateMemberId, jobTitle, newStatus);

        Map<String, Object> payload = new HashMap<>();
        payload.put("applicationId", applicationId);
        payload.put("candidateMemberId", candidateMemberId);
        payload.put("jobTitle", jobTitle);
        payload.put("companyName", companyName);
        payload.put("newStatus", newStatus);

        outboxService.publishEvent("APPLICATION_STATUS_CHANGED", "APPLICATION", applicationId, payload);
    }

    // ==================== 면접 이벤트 ====================

    /**
     * 면접 일정 등록 이벤트 발행 (기존 호환용)
     * 
     * @param interviewId interview_schedule.interview_id
     * @param applicationId job_application.application_id
     * @param userId member.member_id (지원자)
     * @param jobTitle job_posting.title
     * @param interviewDate 면접 일시
     */
    public void publishInterviewScheduledEvent(Long interviewId, Long applicationId, Integer userId,
                                                String jobTitle, String interviewDate) {
        log.info("[Outbox] 면접 일정 등록 이벤트 - interviewId: {}, applicationId: {}, userId: {}, date: {}", 
                interviewId, applicationId, userId, interviewDate);

        Map<String, Object> payload = new HashMap<>();
        payload.put("interviewId", interviewId);
        payload.put("applicationId", applicationId);
        payload.put("candidateMemberId", userId != null ? userId.longValue() : null);
        payload.put("jobTitle", jobTitle);
        payload.put("startAt", interviewDate);

        outboxService.publishEvent("INTERVIEW_CREATED", "INTERVIEW", interviewId, payload);
    }
    
    /**
     * 면접 일정 생성 이벤트 발행
     * 
     * ERD 매핑:
     * - aggregate_type = "INTERVIEW"
     * - aggregate_id = interview_schedule.interview_id
     * - event_type = "INTERVIEW_CREATED"
     * 
     * @param interviewId interview_schedule.interview_id
     * @param applicationId job_application.application_id
     * @param candidateMemberId member.member_id (지원자)
     * @param jobTitle job_posting.title
     * @param companyName employer.name
     * @param startAt interview_schedule.start_at
     * @param location interview_schedule.location
     */
    public void publishInterviewCreatedEvent(Long interviewId, Long applicationId, Long candidateMemberId,
                                              String jobTitle, String companyName, String startAt, String location) {
        log.info("[Outbox] 면접 일정 생성 이벤트 - interviewId: {}, applicationId: {}, candidateMemberId: {}, date: {}, location: {}", 
                interviewId, applicationId, candidateMemberId, startAt, location);

        Map<String, Object> payload = new HashMap<>();
        payload.put("interviewId", interviewId);
        payload.put("applicationId", applicationId);
        payload.put("candidateMemberId", candidateMemberId);
        payload.put("jobTitle", jobTitle);
        payload.put("companyName", companyName);
        payload.put("startAt", startAt);
        payload.put("location", location);

        outboxService.publishEvent("INTERVIEW_CREATED", "INTERVIEW", interviewId, payload);
    }
    
    /**
     * 면접 일정 수정 이벤트 발행
     * 
     * @param interviewId interview_schedule.interview_id
     * @param applicationId job_application.application_id
     * @param candidateMemberId member.member_id (지원자)
     * @param jobTitle job_posting.title
     * @param companyName employer.name
     * @param startAt interview_schedule.start_at
     */
    public void publishInterviewUpdatedEvent(Long interviewId, Long applicationId, Long candidateMemberId,
                                              String jobTitle, String companyName, String startAt) {
        log.info("[Outbox] 면접 일정 수정 이벤트 - interviewId: {}, applicationId: {}, candidateMemberId: {}, date: {}", 
                interviewId, applicationId, candidateMemberId, startAt);

        Map<String, Object> payload = new HashMap<>();
        payload.put("interviewId", interviewId);
        payload.put("applicationId", applicationId);
        payload.put("candidateMemberId", candidateMemberId);
        payload.put("jobTitle", jobTitle);
        payload.put("companyName", companyName);
        payload.put("startAt", startAt);

        outboxService.publishEvent("INTERVIEW_UPDATED", "INTERVIEW", interviewId, payload);
    }
    
    /**
     * 면접 일정 취소 이벤트 발행
     * 
     * @param interviewId interview_schedule.interview_id
     * @param applicationId job_application.application_id
     * @param candidateMemberId member.member_id (지원자)
     * @param jobTitle job_posting.title
     * @param companyName employer.name
     */
    public void publishInterviewCanceledEvent(Long interviewId, Long applicationId, Long candidateMemberId,
                                               String jobTitle, String companyName) {
        log.info("[Outbox] 면접 일정 취소 이벤트 - interviewId: {}, applicationId: {}, candidateMemberId: {}", 
                interviewId, applicationId, candidateMemberId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("interviewId", interviewId);
        payload.put("applicationId", applicationId);
        payload.put("candidateMemberId", candidateMemberId);
        payload.put("jobTitle", jobTitle);
        payload.put("companyName", companyName);

        outboxService.publishEvent("INTERVIEW_CANCELED", "INTERVIEW", interviewId, payload);
    }

    // ==================== 기업 이벤트 ====================

    /**
     * 기업 프로필 등록 이벤트 발행
     * 
     * ERD 매핑:
     * - aggregate_type = "EMPLOYER"
     * - aggregate_id = employer.employer_id
     * - event_type = "EMPLOYER_CREATED"
     * 
     * @param employerId employer.employer_id
     * @param userId member.member_id
     * @param companyName employer.name
     */
    public void publishEmployerCreatedEvent(Long employerId, Integer userId, String companyName) {
        log.info("[Outbox] 기업 등록 이벤트 - employerId: {}, userId: {}, name: {}", 
                employerId, userId, companyName);

        Map<String, Object> payload = new HashMap<>();
        payload.put("employerId", employerId);
        payload.put("userId", userId != null ? userId.longValue() : null);
        payload.put("companyName", companyName);

        outboxService.publishEvent("EMPLOYER_CREATED", "EMPLOYER", employerId, payload);
    }

    /**
     * 기업 프로필 수정 이벤트 발행
     * 
     * @param employerId employer.employer_id
     * @param userId member.member_id
     * @param companyName employer.name
     */
    public void publishEmployerUpdatedEvent(Long employerId, Integer userId, String companyName) {
        log.info("[Outbox] 기업 수정 이벤트 - employerId: {}, userId: {}, name: {}", 
                employerId, userId, companyName);

        Map<String, Object> payload = new HashMap<>();
        payload.put("employerId", employerId);
        payload.put("userId", userId != null ? userId.longValue() : null);
        payload.put("companyName", companyName);

        outboxService.publishEvent("EMPLOYER_UPDATED", "EMPLOYER", employerId, payload);
    }

    // ==================== 결제/크레딧 이벤트 ====================

    /**
     * 광고 크레딧 충전 이벤트 발행
     * 
     * @param employerId employer.employer_id
     * @param userId member.member_id
     * @param amount 충전 금액
     * @param newBalance 새 잔액
     */
    public void publishAdCreditChargedEvent(Long employerId, Integer userId, int amount, int newBalance) {
        log.info("[Outbox] 광고 크레딧 충전 이벤트 - employerId: {}, userId: {}, amount: {}, balance: {}", 
                employerId, userId, amount, newBalance);

        Map<String, Object> payload = new HashMap<>();
        payload.put("employerId", employerId);
        payload.put("userId", userId != null ? userId.longValue() : null);
        payload.put("amount", amount);
        payload.put("newBalance", newBalance);

        outboxService.publishEvent("AD_CREDIT_CHARGED", "WALLET", employerId, payload);
    }

    /**
     * 결제 완료 이벤트 발행
     * 
     * ERD 매핑:
     * - aggregate_type = "PAYMENT"
     * - aggregate_id = payment.payment_id
     * - event_type = "PAYMENT_COMPLETED"
     * 
     * @param paymentId payment.payment_id
     * @param userId member.member_id
     * @param productName product.name
     * @param amount 결제 금액
     */
    public void publishPaymentCompletedEvent(Long paymentId, Long userId, String productName, String amount) {
        log.info("[Outbox] 결제 완료 이벤트 - paymentId: {}, userId: {}, product: {}, amount: {}", 
                paymentId, userId, productName, amount);

        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", paymentId);
        payload.put("userId", userId);
        payload.put("productName", productName);
        payload.put("amount", amount);

        outboxService.publishEvent("PAYMENT_COMPLETED", "PAYMENT", paymentId, payload);
    }

    /**
     * 결제 실패 이벤트 발행
     * 
     * @param paymentId payment.payment_id
     * @param userId member.member_id
     * @param reason 실패 사유
     */
    public void publishPaymentFailedEvent(Long paymentId, Long userId, String reason) {
        log.info("[Outbox] 결제 실패 이벤트 - paymentId: {}, userId: {}, reason: {}", 
                paymentId, userId, reason);

        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", paymentId);
        payload.put("userId", userId);
        payload.put("reason", reason);

        outboxService.publishEvent("PAYMENT_FAILED", "PAYMENT", paymentId, payload);
    }
}
