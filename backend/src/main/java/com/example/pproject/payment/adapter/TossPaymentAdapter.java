package com.example.pproject.payment.adapter;

import com.example.pproject.payment.dto.toss.*;
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

    @Override
    @CircuitBreaker(name = TOSS_PAYMENT, fallbackMethod = "fallbackIssueBillingKey")
    @Retry(name = TOSS_PAYMENT)
    public TossBillingResponse issueBillingKey(String authKey, String customerKey) {
        return tossRestClient.post()
                .uri("/v1/billing/authorizations/issue")
                .body(new TossBillingIssueRequest(authKey, customerKey))
                .retrieve()
                .body(TossBillingResponse.class);
    }

    @Override
    @CircuitBreaker(name = TOSS_PAYMENT, fallbackMethod = "fallbackConfirmBilling")
    @Retry(name = TOSS_PAYMENT)
    public TossPaymentResponse confirmBilling(String billingKey, String customerKey, String orderId,
            BigDecimal amount) {
        log.info("빌링키 결제 요청: billingKey={}, customerKey={}, orderId={}", billingKey, customerKey, orderId);

        // 빌링키에 특수문자(==)가 포함될 수 있으므로, 경로 변수 치환을 사용하여 RestClient가 인코딩하도록 함
        return tossRestClient.post()
                .uri("/v1/billing/{billingKey}", billingKey)
                .body(new TossBillingConfirmRequest(customerKey, orderId, amount)) // customerKey는 선택사항
                .retrieve()
                .body(TossPaymentResponse.class);
    }

    // === Fallback Methods ===

    /**
     * 서킷 브레이커 Open 또는 타임아웃/에러 발생 시 실행
     * null을 반환하거나 특수 상태 객체를 반환하여 Service 계층에서 후처리(재시도/대기)를 할 수 있게 함.
     */
    public TossPaymentResponse fallbackConfirm(String paymentKey, String orderId, BigDecimal amount, Throwable t) {
        log.error("토스 결제 승인 요청 실패 (Fallback 실행). paymentKey={}, orderId={}, error={}", paymentKey, orderId,
                t.getMessage());
        return createUnknownResponse(paymentKey, orderId, amount);
    }

    public TossPaymentResponse fallbackCancel(String paymentKey, String cancelReason, Throwable t) {
        log.error("토스 결제 취소 요청 실패 (Fallback 실행). paymentKey={}, reason={}, error={}", paymentKey, cancelReason,
                t.getMessage());
        return createUnknownResponse(paymentKey, null, null);
    }

    public TossBillingResponse fallbackIssueBillingKey(String authKey, String customerKey, Throwable t) {
        log.error("토스 빌링키 발급 요청 실패 (Fallback 실행). authKey={}, customerKey={}, error={}", authKey, customerKey,
                t.getMessage());
        // 빌링키 발급 실패는 즉시 에러를 던지는 것이 나을 수 있음 (사용자 인터랙션 중이므로)
        throw new RuntimeException("빌링키 발급 중 오류가 발생했습니다.", t);
    }

    public TossPaymentResponse fallbackConfirmBilling(String billingKey, String customerKey, String orderId,
            BigDecimal amount,
            Throwable t) {
        log.info("토스 빌링키 결제 승인 요청 실패 (Fallback 실행). billingKey={}, customerKey={}, orderId={}, error={}", billingKey,
                customerKey, orderId,
                t.getMessage());
        return createUnknownResponse(null, orderId, amount);
    }

    private TossPaymentResponse createUnknownResponse(String paymentKey, String orderId, BigDecimal amount) {
        return new TossPaymentResponse(
                paymentKey, // paymentKey
                orderId, // orderId
                null, // orderName
                "UNKNOWN", // status
                null, // transactionKey
                null, // lastTransactionKey
                null, // requestedAt
                null, // approvedAt
                amount, // totalAmount
                null, // balanceAmount
                null, // method
                null, // receipt
                null, // cancels
                null, // card
                null // virtualAccount
        );
    }
}
