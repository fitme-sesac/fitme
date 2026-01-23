package com.example.pproject.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * [Client -> Server] 결제 취소 요청
 */
public record PaymentCancelRequest(
        @NotBlank(message = "취소 사유는 필수입니다.")
        String cancelReason,

        // 부분 취소 시 사용 (null이면 전액 취소)
        BigDecimal cancelAmount
) {
}
