package com.example.pproject.outbox.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Outbox 패턴 기반 이벤트 발행 클래스
 * - 알림 이벤트를 Outbox 테이블에 저장하여 비동기 처리
 * - 실제 알림 발송은 별도의 Consumer에서 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventProducer {

    // TODO: OutboxRepository 주입 필요 (Outbox 엔티티 구현 후)
    // private final OutboxRepository outboxRepository;

    /**
     * 채용공고 등록 이벤트 발행
     */
    public void publishJobPostingCreatedEvent(Long jobId, Long userId, String jobTitle, 
                                               String companyName, String status) {
        log.info("[Outbox] 채용공고 등록 이벤트 - jobId: {}, userId: {}, title: {}, company: {}, status: {}", 
                jobId, userId, jobTitle, companyName, status);
        // TODO: Outbox 테이블에 저장
    }

    /**
     * 채용공고 수정 이벤트 발행
     */
    public void publishJobPostingUpdatedEvent(Long jobId, Long userId, String jobTitle, String companyName) {
        log.info("[Outbox] 채용공고 수정 이벤트 - jobId: {}, userId: {}, title: {}, company: {}", 
                jobId, userId, jobTitle, companyName);
    }

    /**
     * 채용공고 상태 변경 이벤트 발행
     */
    public void publishJobPostingStatusChangedEvent(Long jobId, Long userId, String jobTitle, 
                                                     String companyName, String oldStatus, String newStatus) {
        log.info("[Outbox] 채용공고 상태 변경 이벤트 - jobId: {}, userId: {}, title: {}, {} -> {}", 
                jobId, userId, jobTitle, oldStatus, newStatus);
    }

    /**
     * 채용공고 삭제 이벤트 발행
     */
    public void publishJobPostingDeletedEvent(Long jobId, Long userId, String jobTitle, String companyName) {
        log.info("[Outbox] 채용공고 삭제 이벤트 - jobId: {}, userId: {}, title: {}, company: {}", 
                jobId, userId, jobTitle, companyName);
    }

    /**
     * 지원서 제출 이벤트 발행
     */
    public void publishApplicationSubmittedEvent(Long applicationId, Long jobId, Integer userId, 
                                                  String jobTitle, String companyName) {
        log.info("[Outbox] 지원서 제출 이벤트 - applicationId: {}, jobId: {}, userId: {}, title: {}", 
                applicationId, jobId, userId, jobTitle);
    }

    /**
     * 지원서 상태 변경 이벤트 발행 (EmployerService용)
     */
    public void publishApplicationStatusChangedEvent(Long applicationId, Long candidateMemberId,
                                                      String jobTitle, String companyName, String newStatus) {
        log.info("[Outbox] 지원서 상태 변경 이벤트 - applicationId: {}, candidateMemberId: {}, jobTitle: {}, status: {}", 
                applicationId, candidateMemberId, jobTitle, newStatus);
    }

    /**
     * 면접 일정 등록 이벤트 발행
     */
    public void publishInterviewScheduledEvent(Long interviewId, Long applicationId, Integer userId,
                                                String jobTitle, String interviewDate) {
        log.info("[Outbox] 면접 일정 등록 이벤트 - interviewId: {}, applicationId: {}, userId: {}, date: {}", 
                interviewId, applicationId, userId, interviewDate);
    }
    
    /**
     * 면접 일정 생성 이벤트 발행
     */
    public void publishInterviewCreatedEvent(Long interviewId, Long applicationId, Long candidateMemberId,
                                              String jobTitle, String companyName, String startAt, String location) {
        log.info("[Outbox] 면접 일정 생성 이벤트 - interviewId: {}, applicationId: {}, candidateMemberId: {}, date: {}, location: {}", 
                interviewId, applicationId, candidateMemberId, startAt, location);
    }
    
    /**
     * 면접 일정 수정 이벤트 발행
     */
    public void publishInterviewUpdatedEvent(Long interviewId, Long applicationId, Long candidateMemberId,
                                              String jobTitle, String companyName, String startAt) {
        log.info("[Outbox] 면접 일정 수정 이벤트 - interviewId: {}, applicationId: {}, candidateMemberId: {}, date: {}", 
                interviewId, applicationId, candidateMemberId, startAt);
    }
    
    /**
     * 면접 일정 취소 이벤트 발행
     */
    public void publishInterviewCanceledEvent(Long interviewId, Long applicationId, Long candidateMemberId,
                                               String jobTitle, String companyName) {
        log.info("[Outbox] 면접 일정 취소 이벤트 - interviewId: {}, applicationId: {}, candidateMemberId: {}", 
                interviewId, applicationId, candidateMemberId);
    }

    /**
     * 기업 프로필 등록 이벤트 발행
     */
    public void publishEmployerCreatedEvent(Long employerId, Integer userId, String companyName) {
        log.info("[Outbox] 기업 등록 이벤트 - employerId: {}, userId: {}, name: {}", 
                employerId, userId, companyName);
    }

    /**
     * 기업 프로필 수정 이벤트 발행
     */
    public void publishEmployerUpdatedEvent(Long employerId, Integer userId, String companyName) {
        log.info("[Outbox] 기업 수정 이벤트 - employerId: {}, userId: {}, name: {}", 
                employerId, userId, companyName);
    }

    /**
     * 광고 충전 이벤트 발행
     */
    public void publishAdCreditChargedEvent(Long employerId, Integer userId, int amount, int newBalance) {
        log.info("[Outbox] 광고 크레딧 충전 이벤트 - employerId: {}, userId: {}, amount: {}, balance: {}", 
                employerId, userId, amount, newBalance);
    }
}
