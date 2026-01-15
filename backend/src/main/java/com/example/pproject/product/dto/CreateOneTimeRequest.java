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
    /**
     * Converts this DTO into a Product entity for a one-time product.
     *
     * The resulting Product uses Money.wons(priceAmount) for its price, has productType
     * set to ProductType.ONE_TIME, and leaves planTier null.
     *
     * @return the constructed Product entity reflecting this request
     */
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