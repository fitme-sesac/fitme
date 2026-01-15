package com.example.pproject.product.dto;

import com.example.pproject.Constant.ProductType;
import com.example.pproject.common.vo.Money;
import com.example.pproject.product.entity.Product;

import java.math.BigDecimal;

public record CreateSubscriptionRequest(
        String productCode,
        String name,
        BigDecimal priceAmount,
        String planTier // ★ 필수
) {
    /**
     * Converts this request into a Product entity representing a subscription.
     *
     * The resulting Product has productCode and name from the request, price set via
     * Money.wons(priceAmount), productType fixed to ProductType.SUBSCRIPTION, planTier
     * from the request, and creditAmount left null.
     *
     * @return the built Product entity
     */
    public Product toEntity() {
        return Product.builder()
                .productCode(productCode)
                .name(name)
                .price(Money.wons(priceAmount))
                .productType(ProductType.SUBSCRIPTION) // 타입 고정
                .planTier(planTier)
                // creditAmount는 null (구독 시 매달 지급 로직은 별도 Subscription 도메인에서 처리)
                .build();
    }
}