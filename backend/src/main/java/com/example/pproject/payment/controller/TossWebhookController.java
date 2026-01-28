package com.example.pproject.payment.controller;

import com.example.pproject.payment.dto.webhook.TossWebhookRequest;
import com.example.pproject.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
public class TossWebhookController {

    private final PaymentService paymentService;

    /**
     * 토스 페이먼츠 웹훅 수신
     * - 결제 상태 변경, 가상계좌 입금 등 이벤트 발생 시 토스 서버에서 호출
     * - 인증 없이 호출 가능해야 함 (Security 설정에서 permitAll 필요)
     */
    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody TossWebhookRequest request) {
        paymentService.handleWebhook(request);
        return ResponseEntity.ok().build();
    }
}
