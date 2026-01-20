package com.example.pproject.product.dto;

public record UpdateProductInfoRequest(
        String name,
        Integer creditAmount,
        String planTier
) {

}
