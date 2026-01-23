package com.example.pproject.payment.dto.toss;

import com.example.pproject.payment.dto.webhook.TossWebhookRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * [Toss API -> Server] 결제 API 공통 응답
 * - 필요한 필드만 매핑 (전체 필드는 Map으로 받아도 됨)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentResponse(
        String paymentKey,
        String orderId,
        String orderName,
        String status, // READY, IN_PROGRESS, WAITING_FOR_DEPOSIT, DONE, CANCELED, PARTIAL_CANCELED, ABORTED, EXPIRED
        String transactionKey, // 추가됨: 토스 트랜잭션 키
        String lastTransactionKey,
        String requestedAt,
        String approvedAt,
        BigDecimal totalAmount,
        BigDecimal balanceAmount,
        String method, // 카드, 가상계좌 등
        Map<String, Object> receipt, // 영수증 정보
        List<TossCancel> cancels, // 취소 이력
        Map<String, Object> card, // 카드 정보
        Map<String, Object> virtualAccount // 가상계좌 정보
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TossCancel(
            String transactionKey,
            String cancelStatus,
            BigDecimal cancelAmount,
            String cancelReason,
            OffsetDateTime canceledAt
    ) {}

    /**
     * 웹훅 요청을 결제 응답 DTO로 변환하는 팩토리 메서드
     */
    public static TossPaymentResponse from(TossWebhookRequest request, String orderName) {
        return new TossPaymentResponse(
                request.data().paymentKey(),
                request.data().orderId(),
                orderName,
                request.data().status(),
                request.data().transactionKey(),
                null, // lastTransactionKey
                request.data().requestedAt(),
                request.data().approvedAt(),
                request.data().totalAmount(),
                request.data().balanceAmount(),
                request.data().method(),
                null, // receipt
                null, // cancels
                null, // card
                null  // virtualAccount
        );
    }
}
