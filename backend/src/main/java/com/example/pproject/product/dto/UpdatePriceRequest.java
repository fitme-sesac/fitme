package com.example.pproject.product.dto;

import com.example.pproject.common.vo.Money;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdatePriceRequest(
        @NotNull(message = "가격은 필수")
        @Positive(message = "가격은 0보다 커야 합니다.")
        BigDecimal newPriceAmount
) {

    public Money toMoney() {
        return Money.wons(newPriceAmount);
    }
}
