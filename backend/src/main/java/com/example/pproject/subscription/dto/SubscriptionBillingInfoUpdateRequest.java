package com.example.pproject.subscription.dto;

public record SubscriptionBillingInfoUpdateRequest(
        String billingKey,
        String customerKey,
        String cardCompany,
        String cardNumber
) {
}
