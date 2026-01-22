package com.example.pproject.Constant;

public enum OrderStatus {

    CREATED,    // 주문 생성됨 (결제 대기)
    PAID,       // 결제 완료
    CANCELED,   // 주문 취소됨
    FAILED      // 결제 실패
}
