package com.example.pproject.subscription.dto;

import com.example.pproject.Constant.SubscriptionStatus;
import com.example.pproject.product.dto.ProductResponse;
import com.example.pproject.subscription.entity.Subscription;

import java.time.Instant;

public record SubscriptionResponse(
        Long subscriptionId,
        Long employerId,
        ProductResponse product,
        SubscriptionStatus status,
        Instant startedAt,
        Instant nextBillingAt,
        Instant endedAt,
        String cardCompany,
        String cardNumber
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getSubscriptionId(),
                subscription.getEmployer() != null ? subscription.getEmployer().getId() : null,
                subscription.getProduct() != null ? ProductResponse.from(subscription.getProduct()) : null,
                subscription.getStatus(),
                subscription.getStartedAt(),
                subscription.getNextBillingAt(),
                subscription.getEndedAt(),
                subscription.getCardCompany(),
                subscription.getCardNumber()
        );
    }
}
