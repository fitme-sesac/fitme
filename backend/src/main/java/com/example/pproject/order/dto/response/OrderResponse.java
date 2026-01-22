package com.example.pproject.order.dto.response;

import com.example.pproject.Constant.OrderStatus;
import com.example.pproject.order.entity.Orders;

import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        Long orderId,
        UUID orderUid,
        String productName,
        Long amount,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(Orders order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getOrderUid(),
                order.getOrderName(),
                order.getOrderAmount().getAmount().longValue(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
