package com.example.pproject.payment.controller;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.payment.dto.response.PgWebhookInboxResponse;
import com.example.pproject.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final PaymentService paymentService;

    /**
     * [관리자] 총 수익 조회
     * - 결제 완료된 모든 건의 금액 합계
     */
    @GetMapping("/revenue")
    public ResponseEntity<Long> getTotalRevenue() {
        return ResponseEntity.ok(paymentService.getTotalRevenue());
    }

    /**
     * [관리자] 구매자 유형별 총 수익 조회
     * - buyerType: MEMBER(일반), EMPLOYER(기업)
     */
    @GetMapping("/revenue/by-type")
    public ResponseEntity<Long> getTotalRevenueByBuyerType(@RequestParam BuyerType buyerType) {
        return ResponseEntity.ok(paymentService.getTotalRevenueByBuyerType(buyerType));
    }

    /**
     * [관리자] 웹훅 이력 조회
     */
    @GetMapping("/webhooks")
    public ResponseEntity<Page<PgWebhookInboxResponse>> getWebhooks(
            @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.getWebhooks(pageable));
    }

    /**
     * [관리자] 웹훅 재처리
     */
    @PostMapping("/webhooks/{inboxId}/retry")
    public ResponseEntity<Void> retryWebhook(@PathVariable Long inboxId) {
        paymentService.retryWebhook(inboxId);
        return ResponseEntity.ok().build();
    }
}
