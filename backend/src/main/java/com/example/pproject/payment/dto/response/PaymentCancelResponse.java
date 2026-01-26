package com.example.pproject.payment.dto.response;

import com.example.pproject.payment.entity.PaymentCancel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCancelResponse(
        Long cancelId,
        BigDecimal cancelAmount,
        String cancelReason,
        String cancelStatus,
        LocalDateTime canceledAt,
        String transactionKey
) {
    public static PaymentCancelResponse from(PaymentCancel cancel) {
        return new PaymentCancelResponse(
                cancel.getCancelId(),
                cancel.getCancelAmount().getAmount(),
                cancel.getCancelReason(),
                cancel.getCancelStatus(),
                cancel.getCanceledAt(),
                cancel.getTossTransactionKey()
        );
    }
}
