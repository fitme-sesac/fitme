package com.example.pproject.order.dto.response;

import com.example.pproject.Constant.OrderStatus;
import com.example.pproject.order.entity.Orders;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        Long orderId,
        UUID orderUid,
        String productName,
        Long amount,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
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
