package com.example.pproject.subscription.dto;

import com.example.pproject.Constant.CreditStatus;
import com.example.pproject.Constant.PaymentStatus;
import com.example.pproject.subscription.entity.SubscriptionBillingCycle;

import java.time.Instant;
import java.time.LocalDate;

public record SubscriptionBillingCycleResponse(
        Long subscriptionBillingCycleId,
        Long subscriptionId,
        LocalDate billingMonth,
        Long paymentId,
        Integer creditGrant,
        Long ledgerId,
        PaymentStatus paymentStatus,
        CreditStatus creditStatus,
        Instant createdAt
) {
    public static SubscriptionBillingCycleResponse from(SubscriptionBillingCycle cycle) {
        return new SubscriptionBillingCycleResponse(
                cycle.getSubscriptionBillingCycleId(),
                cycle.getSubscription() != null ? cycle.getSubscription().getSubscriptionId() : null,
                cycle.getBillingMonth(),
                cycle.getPayment() != null ? cycle.getPayment().getPaymentId() : null,
                cycle.getCreditGrant(),
                cycle.getWalletLedger() != null ? cycle.getWalletLedger().getLedgerId() : null,
                cycle.getPaymentStatus(),
                cycle.getCreditStatus(),
                cycle.getCreatedAt()
        );
    }
}
