package com.example.pproject.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WalletUseRequest(
        @NotNull(message = "금액은 필수입니다.")
        @Min(value = 1, message = "사용 금액은 0보다 커야 합니다.")
        Long amount,

        @NotNull(message = "사용처 코드는 필수입니다.")
        String serviceCode, // 예: "AI_SUMMARY", "AD_CLICK"

        String referenceId  // 추적용 ID (job_id, resume_id 등) - 없으면 null 가능
) {
}
