package com.example.pproject.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * [Client -> Server] 결제 승인 요청
 * - 토스 결제창 인증 성공 후, successUrl로 리다이렉트될 때 전달받은 파라미터들을 서버로 전송
 */
public record PaymentConfirmRequest(
        @NotBlank(message = "paymentKey는 필수입니다.")
        String paymentKey,

        @NotBlank(message = "orderId는 필수입니다.")
        String orderId,

        @NotNull(message = "amount는 필수입니다.")
        @Min(value = 100, message = "결제 금액은 최소 100원 이상이어야 합니다.")
        BigDecimal amount
) {
}
