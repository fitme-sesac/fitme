package com.example.pproject.Constant;

/**
 * 아웃박스 이벤트 상태
 * - PENDING: 처리 대기
 * - PROCESSING: 처리 중
 * - SUCCEEDED: 성공
 * - FAILED: 실패
 */
public enum OutboxStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED
}
