package com.example.pproject.payment.dto.toss;

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
}
