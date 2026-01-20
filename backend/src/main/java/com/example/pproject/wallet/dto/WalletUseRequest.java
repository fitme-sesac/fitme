package com.example.pproject.wallet.dto;

import com.example.pproject.Constant.SourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * [요청 DTO] 크레딧 사용(차감) 요청
 * - 상품 구매나 서비스 이용 시 크레딧을 차감할 때 사용합니다.
 */
public record WalletUseRequest(
        @NotNull(message = "사용할 크레딧 양은 필수입니다.")
        @Min(value = 1, message = "사용 크레딧은 1 이상이어야 합니다.")
        Long amount,

        @NotNull(message = "주문 ID(멱등키)는 필수입니다.")
        String orderId,

        @NotNull(message = "사용처(SourceType)는 필수입니다.")
        SourceType sourceType // AI, AD_CLICK 등
) {
}
