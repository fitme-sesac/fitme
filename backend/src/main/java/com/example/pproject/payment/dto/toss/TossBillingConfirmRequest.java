package com.example.pproject.payment.dto.toss;

import java.math.BigDecimal;

public record TossBillingConfirmRequest(
        String customerKey,
        String orderId,
        BigDecimal amount
) {
}
