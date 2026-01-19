package com.example.pproject.Constant;

public enum PaymentAppStatus {
    REQUESTED,          // 결제 요청
    APPROVED,           // 승인 완료
    FAILED,             // 승인/요청 실패
    CANCELED,           // 전체 취소
    PARTIAL_CANCELED    // 부분 취소
}
