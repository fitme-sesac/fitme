package com.example.pproject.product.dto;

import com.example.pproject.common.vo.Money;

import java.math.BigDecimal;

public record UpdatePriceRequest(BigDecimal newPriceAmount) {

    public Money toMoney() {
        return Money.wons(newPriceAmount);
    }
}
