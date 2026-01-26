package com.example.pproject.subscription.controller;

import com.example.pproject.subscription.dto.SubscriptionBillingCycleResponse;
import com.example.pproject.subscription.service.SubscriptionBillingCycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscription-billing-cycles")
@RequiredArgsConstructor
public class SubscriptionBillingCycleController {

    private final SubscriptionBillingCycleService billingCycleService;

    // =============================================================================================
    // [일반 유저 기능]
    // =============================================================================================

    /**
     * 내 구독의 결제/청구 이력 조회
     */
    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<List<SubscriptionBillingCycleResponse>> getBillingCycles(@PathVariable Long subscriptionId) {
        List<SubscriptionBillingCycleResponse> responses = billingCycleService.getBillingCycles(subscriptionId);
        return ResponseEntity.ok(responses);
    }
}
