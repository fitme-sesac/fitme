package com.example.pproject.payment.dto.toss;

import java.math.BigDecimal;

/**
 * [Server -> Toss API] 결제 승인 요청 Body
 * - https://docs.tosspayments.com/reference#%EA%B2%B0%EC%A0%9C-%EC%8A%B9%EC%9D%B8
 */
public record TossPaymentConfirmRequest(
        String paymentKey,
        String orderId,
        BigDecimal amount
) {
}
