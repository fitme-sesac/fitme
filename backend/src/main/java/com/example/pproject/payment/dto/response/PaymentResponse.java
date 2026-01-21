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
        String orderId, // Payment 엔티티에 orderId 필드가 없어서 주석 처리된 상태라면 확인 필요
        String orderName, // Payment 엔티티에 orderName 필드가 없다면 추가 필요
        PaymentMethod method,
        BigDecimal totalAmount,
        PaymentAppStatus status,
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

        return new PaymentResponse(
                payment.getPaymentId(),
                "ORDER_ID_PLACEHOLDER", // TODO: Payment 엔티티에 orderId 매핑 필요
                "ORDER_NAME_PLACEHOLDER", // TODO: Payment 엔티티에 orderName 매핑 필요
                payment.getMethod(),
                payment.getPaidAmount().getAmount(),
                payment.getAppStatus(),
                payment.getApprovedAt(),
                receiptUrl
        );
    }
}
