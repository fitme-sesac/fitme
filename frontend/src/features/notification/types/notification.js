/**
 * 알림 이벤트 타입
 */
export const NotificationEventType = {
    // 지원 관련
    APPLICATION_SUBMITTED: "APPLICATION_SUBMITTED",
    APPLICATION_VIEWED: "APPLICATION_VIEWED",
    APPLICATION_STATUS_CHANGED: "APPLICATION_STATUS_CHANGED",
    
    // 면접 관련
    INTERVIEW_SCHEDULED: "INTERVIEW_SCHEDULED",
    INTERVIEW_REMINDER: "INTERVIEW_REMINDER",
    INTERVIEW_CANCELED: "INTERVIEW_CANCELED",
    INTERVIEW_RESULT: "INTERVIEW_RESULT",
    
    // 채용 결과 관련
    HIRED: "HIRED",
    REJECTED: "REJECTED",
    
    // 결제 관련
    PAYMENT_COMPLETED: "PAYMENT_COMPLETED",
    PAYMENT_FAILED: "PAYMENT_FAILED",
    PAYMENT_REFUNDED: "PAYMENT_REFUNDED",
    
    // 구독 관련
    SUBSCRIPTION_STARTED: "SUBSCRIPTION_STARTED",
    SUBSCRIPTION_RENEWED: "SUBSCRIPTION_RENEWED",
    SUBSCRIPTION_EXPIRING: "SUBSCRIPTION_EXPIRING",
    SUBSCRIPTION_CANCELED: "SUBSCRIPTION_CANCELED",
    
    // 크레딧 관련
    CREDIT_CHARGED: "CREDIT_CHARGED",
    CREDIT_USED: "CREDIT_USED",
    CREDIT_LOW: "CREDIT_LOW",
    
    // 채용공고 관련 (기업용)
    NEW_APPLICATION_RECEIVED: "NEW_APPLICATION_RECEIVED",
    JOB_POSTING_APPROVED: "JOB_POSTING_APPROVED",
    JOB_POSTING_REJECTED: "JOB_POSTING_REJECTED",
    JOB_POSTING_EXPIRED: "JOB_POSTING_EXPIRED",
    
    // 시스템
    SYSTEM_NOTICE: "SYSTEM_NOTICE"
};

/**
 * 이벤트 타입별 아이콘 클래스
 */
export const getNotificationIcon = (eventType) => {
    const iconMap = {
        // 지원 관련
        APPLICATION_SUBMITTED: "bi bi-send-check",
        APPLICATION_VIEWED: "bi bi-eye",
        APPLICATION_STATUS_CHANGED: "bi bi-arrow-repeat",
        
        // 면접 관련
        INTERVIEW_SCHEDULED: "bi bi-calendar-check",
        INTERVIEW_REMINDER: "bi bi-alarm",
        INTERVIEW_CANCELED: "bi bi-calendar-x",
        INTERVIEW_RESULT: "bi bi-clipboard-check",
        
        // 채용 결과
        HIRED: "bi bi-trophy",
        REJECTED: "bi bi-x-circle",
        
        // 결제 관련
        PAYMENT_COMPLETED: "bi bi-credit-card-2-front",
        PAYMENT_FAILED: "bi bi-exclamation-triangle",
        PAYMENT_REFUNDED: "bi bi-arrow-counterclockwise",
        
        // 구독 관련
        SUBSCRIPTION_STARTED: "bi bi-star",
        SUBSCRIPTION_RENEWED: "bi bi-arrow-clockwise",
        SUBSCRIPTION_EXPIRING: "bi bi-clock-history",
        SUBSCRIPTION_CANCELED: "bi bi-x-octagon",
        
        // 크레딧 관련
        CREDIT_CHARGED: "bi bi-coin",
        CREDIT_USED: "bi bi-cash",
        CREDIT_LOW: "bi bi-exclamation-circle",
        
        // 채용공고 관련
        NEW_APPLICATION_RECEIVED: "bi bi-person-plus",
        JOB_POSTING_APPROVED: "bi bi-check-circle",
        JOB_POSTING_REJECTED: "bi bi-x-circle",
        JOB_POSTING_EXPIRED: "bi bi-hourglass",
        
        // 시스템
        SYSTEM_NOTICE: "bi bi-megaphone"
    };
    
    return iconMap[eventType] || "bi bi-bell";
};

/**
 * 이벤트 타입별 배경색 클래스
 */
export const getNotificationBgColor = (eventType) => {
    const colorMap = {
        // 성공/긍정
        APPLICATION_SUBMITTED: "bg-primary",
        HIRED: "bg-success",
        PAYMENT_COMPLETED: "bg-success",
        SUBSCRIPTION_STARTED: "bg-success",
        CREDIT_CHARGED: "bg-success",
        JOB_POSTING_APPROVED: "bg-success",
        
        // 정보
        APPLICATION_VIEWED: "bg-info",
        APPLICATION_STATUS_CHANGED: "bg-info",
        INTERVIEW_SCHEDULED: "bg-info",
        INTERVIEW_REMINDER: "bg-warning",
        NEW_APPLICATION_RECEIVED: "bg-primary",
        SUBSCRIPTION_RENEWED: "bg-info",
        
        // 경고/주의
        SUBSCRIPTION_EXPIRING: "bg-warning",
        CREDIT_LOW: "bg-warning",
        JOB_POSTING_EXPIRED: "bg-warning",
        
        // 실패/부정
        REJECTED: "bg-secondary",
        PAYMENT_FAILED: "bg-danger",
        INTERVIEW_CANCELED: "bg-danger",
        SUBSCRIPTION_CANCELED: "bg-secondary",
        JOB_POSTING_REJECTED: "bg-danger",
        
        // 기타
        SYSTEM_NOTICE: "bg-dark"
    };
    
    return colorMap[eventType] || "bg-secondary";
};

/**
 * 상대적 시간 표시 (예: "5분 전")
 */
export const getRelativeTime = (dateString) => {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now - date;
    
    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    
    if (seconds < 60) {
        return "방금 전";
    } else if (minutes < 60) {
        return `${minutes}분 전`;
    } else if (hours < 24) {
        return `${hours}시간 전`;
    } else if (days < 7) {
        return `${days}일 전`;
    } else {
        return date.toLocaleDateString("ko-KR", {
            month: "short",
            day: "numeric"
        });
    }
};
