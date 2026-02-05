package com.example.pproject.subscription.dto;

public record SubscriptionCreateRequest(
                Long employerId,
                Long productId,
                String billingKey, // 기존 키 사용 시
                String authKey, // 새로운 키 발급 시 (billingKey가 없으면 필수)
                String customerKey) {
}
