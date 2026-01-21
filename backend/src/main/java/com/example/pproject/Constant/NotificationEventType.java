package com.example.pproject.Constant;

/**
 * 알림 이벤트 타입
 */
public enum NotificationEventType {
    // 지원 관련
    APPLICATION_SUBMITTED("채용공고에 지원이 완료되었습니다."),
    APPLICATION_VIEWED("기업에서 이력서를 열람했습니다."),
    APPLICATION_STATUS_CHANGED("지원 상태가 변경되었습니다."),

    // 면접 관련
    INTERVIEW_SCHEDULED("면접 일정이 확정되었습니다."),
    INTERVIEW_REMINDER("면접 일정이 다가왔습니다."),
    INTERVIEW_CANCELED("면접 일정이 취소되었습니다."),
    INTERVIEW_RESULT("면접 결과가 등록되었습니다."),

    // 채용 결과 관련
    HIRED("축하합니다! 합격하셨습니다."),
    REJECTED("아쉽게도 불합격 결과가 전달되었습니다."),

    // 결제 관련
    PAYMENT_COMPLETED("결제가 완료되었습니다."),
    PAYMENT_FAILED("결제가 실패했습니다."),
    PAYMENT_REFUNDED("환불이 완료되었습니다."),

    // 구독 관련
    SUBSCRIPTION_STARTED("구독이 시작되었습니다."),
    SUBSCRIPTION_RENEWED("구독이 갱신되었습니다."),
    SUBSCRIPTION_EXPIRING("구독이 곧 만료됩니다."),
    SUBSCRIPTION_CANCELED("구독이 취소되었습니다."),

    // 크레딧 관련
    CREDIT_CHARGED("크레딧이 충전되었습니다."),
    CREDIT_USED("크레딧이 사용되었습니다."),
    CREDIT_LOW("크레딧 잔액이 부족합니다."),

    // 채용공고 관련 (기업용)
    NEW_APPLICATION_RECEIVED("새로운 지원자가 있습니다."),
    JOB_POSTING_APPROVED("채용공고가 승인되었습니다."),
    JOB_POSTING_REJECTED("채용공고가 반려되었습니다."),
    JOB_POSTING_EXPIRED("채용공고 기간이 만료되었습니다."),

    // 회원 활동 관련
    PROFILE_UPDATED("회원 정보가 수정되었습니다."),
    PASSWORD_CHANGED("비밀번호가 변경되었습니다."),
    
    // 시스템
    SYSTEM_NOTICE("시스템 공지사항입니다.");

    private final String defaultMessage;

    NotificationEventType(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
