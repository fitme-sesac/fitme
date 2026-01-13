package com.example.pproject.product.dto;

import com.example.pproject.Constant.ProductType;
import com.example.pproject.common.vo.Money;
import com.example.pproject.product.entity.Product;

import java.math.BigDecimal;

public record CreateOneTimeRequest(
        String productCode,
        String name,
        BigDecimal priceAmount,
        Integer creditAmount // ★ 필수
) {
    public Product toEntity() {
        return Product.builder()
                .productCode(productCode)
                .name(name)
                .price(Money.wons(priceAmount))
                .productType(ProductType.ONE_TIME) // 타입 고정
                .creditAmount(creditAmount)
                // planTier는 null
                .build();
    }
}
