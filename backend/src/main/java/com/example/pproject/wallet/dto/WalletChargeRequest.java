package com.example.pproject.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 충전 요청 RequestDTO
public record WalletChargeRequest(
        @NotNull(message = "금액은 필수입니다.")
        @Min(value = 100, message = "충전 금액은 최소 100원 이상이어야 합니다.")
        Long amount,

        @NotBlank(message = "사유를 입력해주세요.") // 예: "이벤트 지급", "결제 충전"
        String memo
) {
}
