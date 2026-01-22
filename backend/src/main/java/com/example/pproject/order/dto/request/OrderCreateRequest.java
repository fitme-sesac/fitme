package com.example.pproject.order.dto.request;

import com.example.pproject.Constant.RoleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderCreateRequest(
        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,

        @NotNull(message = "구매자 타입은 필수입니다.")
        RoleType buyerType,

        @NotNull(message = "주문 금액은 필수입니다.")
        @Min(value = 100, message = "주문 금액은 최소 100원 이상이어야 합니다.")
        Long amount
) {
}
