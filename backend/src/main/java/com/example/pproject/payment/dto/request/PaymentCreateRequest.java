package com.example.pproject.payment.dto.request;

import com.example.pproject.Constant.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * [Client -> Server] 결제 생성 요청
 * - 사용자가 '결제하기' 버튼을 눌렀을 때, 주문 정보를 서버에 미리 등록하기 위한 DTO
 */
public record PaymentCreateRequest(
        @NotNull(message = "결제 금액은 필수입니다.")
        @Min(value = 100, message = "결제 금액은 최소 100원 이상이어야 합니다.")
        BigDecimal amount,

        @NotBlank(message = "주문명은 필수입니다.")
        String orderName,

        // 선택 사항: 위젯 방식이면 null일 수 있음
        PaymentMethod method
) {
}
