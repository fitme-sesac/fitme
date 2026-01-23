package com.example.pproject.order.dto.request;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.Constant.RoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderCreateRequest(
        @NotBlank(message = "상품 코드는 필수입니다.")
        String productCode,

        @NotNull(message = "구매자 타입은 필수입니다.")
        BuyerType buyerType,

        // 멱등성 키 (선택)
        String idempotencyKey
) {
}
