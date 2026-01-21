package com.example.pproject.Constant;

/**
 * 알림 발송 상태 (ERD 기준: PENDING, SENT, FAILED)
 * - PENDING: 발송 대기
 * - SENT: 발송 완료
 * - FAILED: 발송 실패
 * 
 * 참고: 읽음 여부는 payload.readAt으로 관리
 */
public enum NotificationDeliveryStatus {
    PENDING,
    SENT,
    FAILED
}
