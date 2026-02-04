package com.example.pproject.payment.dto.response;

import com.example.pproject.Constant.PaymentAppStatus;
import com.example.pproject.Constant.PaymentMethod;
import com.example.pproject.payment.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * [Server -> Client] 결제 정보 응답
 */
public record PaymentResponse(
        Long paymentId,
        String orderId,
        String orderName,
        PaymentMethod method,
        BigDecimal totalAmount,
        Integer creditAmount, // 충전된 크레딧 수량
        String status,
        LocalDateTime approvedAt,
        String receiptUrl // 토스 영수증 URL (pgPayload에서 추출)
) {
    public static PaymentResponse from(Payment payment) {
        // pgPayload에서 영수증 URL 추출
        String receiptUrl = null;
        Map<String, Object> payload = payment.getPgPayload();

        if (payload != null && payload.containsKey("receipt")) {
            Object receiptObj = payload.get("receipt");
            if (receiptObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> receiptMap = (Map<String, Object>) receiptObj;
                Object urlObj = receiptMap.get("url");
                if (urlObj instanceof String) {
                    receiptUrl = (String) urlObj;
                }
            }
        }

        // 크레딧 수량 (상품에서 가져옴)
        Integer creditAmount = null;
        if (payment.getOrder() != null && payment.getOrder().getProduct() != null) {
            creditAmount = payment.getOrder().getProduct().getCreditAmount();
        }

        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getOrder().getOrderUid().toString(),
                payment.getOrderName(),
                payment.getMethod(),
                payment.getPaidAmount().getAmount(),
                creditAmount,
                payment.getAppStatus().getDescription(),
                payment.getApprovedAt(),
                receiptUrl);
    }
}
