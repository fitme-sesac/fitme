package com.example.pproject.payment.port;

import com.example.pproject.payment.dto.toss.TossBillingResponse;
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

    /**
     * 빌링키 발급 요청
     * @param authKey 인증 키 (토스 위젯/창에서 발급)
     * @param customerKey 고객 식별 키
     * @return 빌링키 발급 결과
     */
    TossBillingResponse issueBillingKey(String authKey, String customerKey);

    /**
     * 빌링키 결제 승인 요청 (자동 결제)
     * @param billingKey 발급받은 빌링키
     * @param orderId 주문 ID
     * @param amount 결제 금액
     * @return 결제 승인 결과
     */
    TossPaymentResponse confirmBilling(String billingKey, String orderId, BigDecimal amount);
}
