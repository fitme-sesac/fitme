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
     * 서킷 브레이커가 열렸거나(Open), 재시도 횟수를 초과했을 때 실행되는 Fallback 메서드
     */
    public TossPaymentResponse fallbackConfirm(String paymentKey, String orderId, BigDecimal amount, Throwable t) {
        log.error("토스 결제 승인 요청 실패 (Fallback 실행). paymentKey={}, orderId={}, error={}", paymentKey, orderId, t.getMessage());
        
        if (t instanceof CallNotPermittedException) {
            throw new IllegalStateException("현재 결제 시스템이 불안정하여 잠시 후 다시 시도해주세요. (Circuit Open)");
        }
        
        throw new IllegalStateException("결제 승인 요청 중 오류가 발생했습니다: " + t.getMessage());
    }

    public TossPaymentResponse fallbackCancel(String paymentKey, String cancelReason, Throwable t) {
        log.error("토스 결제 취소 요청 실패 (Fallback 실행). paymentKey={}, reason={}, error={}", paymentKey, cancelReason, t.getMessage());

        if (t instanceof CallNotPermittedException) {
            throw new IllegalStateException("현재 결제 시스템이 불안정하여 잠시 후 다시 시도해주세요. (Circuit Open)");
        }

        throw new IllegalStateException("결제 취소 요청 중 오류가 발생했습니다: " + t.getMessage());
    }
}
