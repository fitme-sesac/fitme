package com.example.pproject.payment.dto.request;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.Constant.PaymentMethod;
import com.example.pproject.Constant.RoleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * [Client -> Server] 결제 생성 요청
 * - 사용자가 '결제하기' 버튼을 눌렀을 때, 주문 정보를 서버에 미리 등록하기 위한 DTO
 */
public record PaymentCreateRequest(
                @NotNull(message = "결제 금액은 필수입니다.") @Min(value = 100, message = "결제 금액은 최소 100원 이상이어야 합니다.") BigDecimal amount,

                @NotBlank(message = "주문명은 필수입니다.") String orderName,

                // 선택 사항: 위젯 방식이면 null일 수 있음
                PaymentMethod method,

                // 구매자 타입 (CANDIDATE / EMPLOYER) - 필수
                @NotNull(message = "구매자 타입은 필수입니다.") BuyerType buyerType,

                @NotBlank(message = "상품 코드는 필수입니다.") String productCode,

                // 멱등성 키 (선택)
                String idempotencyKey,

                // 고객 키 (선택 - 빌링키 결제 등에서 사용)
                String customerKey) {
}
