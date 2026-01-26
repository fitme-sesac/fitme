package com.example.pproject.payment.dto.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossBillingResponse(
        String mId,
        String customerKey,
        String authenticatedAt,
        String method,
        String billingKey,
        Card card
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Card(
            String issuerCode,
            String acquirerCode,
            String number,
            String cardType,
            String ownerType
    ) {}
}
