package com.example.pproject.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * [관리자 요청 DTO] 크레딧 수동 지급
 * - 관리자가 CS 처리, 이벤트 등으로 특정 지갑에 크레딧을 직접 지급할 때 사용합니다.
 */
public record WalletManualChargeRequest(
        @NotNull(message = "지급할 크레딧 양은 필수입니다.")
        @Min(value = 1, message = "지급 크레딧은 1 이상이어야 합니다.")
        Long amount,

        @NotBlank(message = "지급 사유(메모)는 필수입니다.")
        String memo
) {
}
