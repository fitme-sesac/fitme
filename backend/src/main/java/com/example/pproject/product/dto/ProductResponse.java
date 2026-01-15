package com.example.pproject.product.dto;

import com.example.pproject.Constant.ProductType;
import com.example.pproject.Constant.SaleStatus;
import com.example.pproject.product.entity.Product;

import java.math.BigDecimal;
//import java.time.Instant;
import java.time.LocalDateTime;

public record ProductResponse(
        Long productId,
        String productCode,
        String name,
        BigDecimal priceAmount, // Money -> BigDecimal 평탄화
        String currency,
        ProductType productType,
        SaleStatus saleStatus,
        Integer creditAmount,
        String planTier,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Create a ProductResponse DTO from a Product entity.
     *
     * @param product the source Product entity to convert
     * @return a ProductResponse populated with the product's fields (price flattened to amount and currency)
     */
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getProductId(),
                product.getProductCode(),
                product.getName(),
                product.getPrice().getAmount(),   // VO에서 값 꺼내기
                product.getPrice().getCurrency(), // VO에서 값 꺼내기
                product.getProductType(),
                product.getSaleStatus(),
                product.getCreditAmount(),
                product.getPlanTier(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}