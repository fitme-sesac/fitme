package com.example.pproject.payment.dto.toss;

import java.math.BigDecimal;

/**
 * [Server -> Toss API] 결제 취소 요청 Body
 * - https://docs.tosspayments.com/reference#%EA%B2%B0%EC%A0%9C-%EC%B7%A8%EC%86%8C
 */
public record TossPaymentCancelRequest(
        String cancelReason,
        BigDecimal cancelAmount
) {
}
