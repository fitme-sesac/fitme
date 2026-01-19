package com.example.pproject.payment.dto.webhook;

import java.math.BigDecimal;

/**
 * [Toss -> Server] 웹훅 요청 DTO
 * - https://docs.tosspayments.com/reference#%EC%9B%B9%ED%9B%85
 */
public record TossWebhookRequest(
        String eventType, // PAYMENT_STATUS_CHANGED, VIRTUAL_ACCOUNT_CREATED 등
        String createdAt,
        TossWebhookData data
) {
    public record TossWebhookData(
            String paymentKey,
            String orderId,
            String status, // DONE, CANCELED, WAITING_FOR_DEPOSIT 등
            String transactionKey,
            String method,
            BigDecimal totalAmount,
            BigDecimal balanceAmount,
            String approvedAt,
            String requestedAt
            // 필요한 필드 추가 가능
    ) {}
}
