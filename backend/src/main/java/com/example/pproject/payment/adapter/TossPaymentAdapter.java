package com.example.pproject.payment.adapter;

import com.example.pproject.payment.dto.toss.TossPaymentCancelRequest;
import com.example.pproject.payment.dto.toss.TossPaymentConfirmRequest;
import com.example.pproject.payment.dto.toss.TossPaymentResponse;
import com.example.pproject.payment.port.PaymentPort;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentAdapter implements PaymentPort {

    private final RestClient tossRestClient;

    private static final String TOSS_PAYMENT = "tossPayment";

    @Override
    @CircuitBreaker(name = TOSS_PAYMENT, fallbackMethod = "fallbackConfirm")
    @Retry(name = TOSS_PAYMENT)
    public TossPaymentResponse confirm(String paymentKey, String orderId, BigDecimal amount) {
        return tossRestClient.post()
                .uri("/v1/payments/confirm")
                .body(new TossPaymentConfirmRequest(paymentKey, orderId, amount))
                .retrieve()
                .body(TossPaymentResponse.class);
    }

    @Override
    @CircuitBreaker(name = TOSS_PAYMENT, fallbackMethod = "fallbackCancel")
    @Retry(name = TOSS_PAYMENT)
    public TossPaymentResponse cancel(String paymentKey, String cancelReason) {
        return tossRestClient.post()
                .uri("/v1/payments/" + paymentKey + "/cancel")
                .body(new TossPaymentCancelRequest(cancelReason, null))
                .retrieve()
                .body(TossPaymentResponse.class);
    }

    // === Fallback Methods ===

    /**
     * 서킷 브레이커 Open 또는 타임아웃/에러 발생 시 실행
     * null을 반환하거나 특수 상태 객체를 반환하여 Service 계층에서 후처리(재시도/대기)를 할 수 있게 함.
     */
    public TossPaymentResponse fallbackConfirm(String paymentKey, String orderId, BigDecimal amount, Throwable t) {
        log.error("토스 결제 승인 요청 실패 (Fallback 실행). paymentKey={}, orderId={}, error={}", paymentKey, orderId, t.getMessage());
        
        // 서킷 브레이커가 열려있거나, 일시적 네트워크 오류인 경우
        // "UNKNOWN" 상태의 응답을 만들어 반환 -> Service에서 이를 감지하고 "결제 대기" 상태로 처리
        return new TossPaymentResponse(
                paymentKey, // paymentKey
                orderId,    // orderId
                null,       // orderName
                "UNKNOWN",  // status
                null,       // transactionKey
                null,       // lastTransactionKey
                null,       // requestedAt
                null,       // approvedAt
                amount,     // totalAmount
                null,       // balanceAmount
                null,       // method
                null,       // receipt
                null,       // cancels
                null,       // card
                null        // virtualAccount
        );
    }

    public TossPaymentResponse fallbackCancel(String paymentKey, String cancelReason, Throwable t) {
        log.error("토스 결제 취소 요청 실패 (Fallback 실행). paymentKey={}, reason={}, error={}", paymentKey, cancelReason, t.getMessage());

        // 취소 실패 시에도 UNKNOWN 상태 반환 -> 추후 배치로 재시도하거나 수동 처리 유도
        return new TossPaymentResponse(
                paymentKey, // paymentKey
                null,       // orderId
                null,       // orderName
                "UNKNOWN",  // status
                null,       // transactionKey
                null,       // lastTransactionKey
                null,       // requestedAt
                null,       // approvedAt
                null,       // totalAmount
                null,       // balanceAmount
                null,       // method
                null,       // receipt
                null,       // cancels
                null,       // card
                null        // virtualAccount
        );
    }
}
