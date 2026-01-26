package com.example.pproject.subscription.dto;

public record SubscriptionCreateRequest(
        Long employerId,
        Long productId,
        String customerKey,
        String billingKey
) {
}
