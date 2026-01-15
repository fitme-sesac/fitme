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

    /**
     * Converts this request's price value to a Money instance denominated in Korean wons.
     *
     * @return the Money representation of the record's price in wons
     */
    public Money toMoney() {
        return Money.wons(newPriceAmount);
    }
}