package com.example.pproject.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * [관리자 요청 DTO] 크레딧 수동 차감(회수)
 * - 관리자가 오지급 회수, 제재 등으로 특정 지갑에서 크레딧을 직접 차감할 때 사용합니다.
 */
public record WalletManualDeductRequest(
        @NotNull(message = "차감할 크레딧 양은 필수입니다.")
        @Min(value = 1, message = "차감 크레딧은 1 이상이어야 합니다.")
        Long amount,

        @NotBlank(message = "차감 사유(메모)는 필수입니다.")
        String memo
) {
}
