package com.example.pproject.wallet.dto;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.common.vo.Money;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * [요청 DTO] 크레딧 충전 요청
 * - 결제 완료 후, 지갑에 크레딧을 지급할 때 사용합니다.
 */
public record WalletChargeRequest(
        @NotNull(message = "충전할 크레딧 양은 필수입니다.") @Min(value = 1, message = "충전 크레딧은 1 이상이어야 합니다.") Long amount,

        @NotNull(message = "결제 금액은 필수입니다.") BigDecimal priceAmount,

        @NotNull(message = "통화 정보는 필수입니다.") String currency, // "KRW", "USD"

        @NotNull(message = "결제 ID는 필수입니다.") Long paymentId,

        @NotNull(message = "구매자 타입은 필수입니다.") BuyerType buyerType) {
    public Money toMoney() {
        return new Money(priceAmount, currency);
    }
}
