package com.example.pproject.payment.controller;

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
@PreAuthorize("hasRole('ADMIN')") // 관리자 전용
public class AdminPaymentController {

    private final PaymentService paymentService;

    /**
     * 웹훅 수신 이력 조회
     */
    @GetMapping("/webhooks")
    public ResponseEntity<Page<PgWebhookInboxResponse>> getWebhooks(
            @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(paymentService.getWebhooks(pageable));
    }

    /**
     * 실패한 웹훅 재처리
     */
    @PostMapping("/webhooks/{inboxId}/retry")
    public ResponseEntity<Void> retryWebhook(@PathVariable Long inboxId) {
        paymentService.retryWebhook(inboxId);
        return ResponseEntity.ok().build();
    }
}
