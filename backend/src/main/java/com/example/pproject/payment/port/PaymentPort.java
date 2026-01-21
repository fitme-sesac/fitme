package com.example.pproject.payment.port;

import com.example.pproject.payment.dto.toss.TossPaymentResponse;

import java.math.BigDecimal;

/**
 * 결제 시스템 연동을 위한 포트 (Port)
 * - 외부 결제 시스템(토스 등)과의 통신을 추상화합니다.
 * - 도메인 로직은 이 인터페이스에 의존하며, 구체적인 구현체(Adapter)는 알 필요가 없습니다.
 */
public interface PaymentPort {

    /**
     * 결제 승인 요청
     * @param paymentKey 토스 결제 키
     * @param orderId 주문 ID
     * @param amount 결제 금액
     * @return 승인 결과
     */
    TossPaymentResponse confirm(String paymentKey, String orderId, BigDecimal amount);

    /**
     * 결제 취소 요청
     * @param paymentKey 토스 결제 키
     * @param cancelReason 취소 사유
     * @return 취소 결과
     */
    TossPaymentResponse cancel(String paymentKey, String cancelReason);
}
