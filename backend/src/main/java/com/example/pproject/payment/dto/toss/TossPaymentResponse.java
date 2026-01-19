package com.example.pproject.payment.dto.toss;

import java.math.BigDecimal;
import java.util.Map;

/**
 * [Toss API -> Server] 결제 API 공통 응답
 * - 필요한 필드만 매핑 (전체 필드는 Map으로 받아도 됨)
 */
public record TossPaymentResponse(
        String paymentKey,
        String orderId,
        String orderName,
        String status, // READY, IN_PROGRESS, WAITING_FOR_DEPOSIT, DONE, CANCELED, PARTIAL_CANCELED, ABORTED, EXPIRED
        String requestedAt,
        String approvedAt,
        BigDecimal totalAmount,
        BigDecimal balanceAmount,
        String method, // 카드, 가상계좌 등
        Map<String, Object> receipt, // 영수증 정보
        Map<String, Object> cancels, // 취소 이력
        Map<String, Object> card, // 카드 정보
        Map<String, Object> virtualAccount // 가상계좌 정보
) {
}
