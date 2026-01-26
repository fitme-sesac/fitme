package com.example.pproject.payment.dto.toss;

public record TossBillingIssueRequest(
        String authKey,
        String customerKey
) {
}
