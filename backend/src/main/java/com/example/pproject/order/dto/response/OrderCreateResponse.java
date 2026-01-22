package com.example.pproject.order.dto.response;

import com.example.pproject.order.entity.Orders;

import java.util.UUID;

public record OrderCreateResponse(
        Long orderId,
        UUID orderUid,
        String orderName,
        Long amount,
        String buyerName,
        String buyerEmail
) {
    public static OrderCreateResponse from(Orders order, String buyerName, String buyerEmail) {
        return new OrderCreateResponse(
                order.getOrderId(),
                order.getOrderUid(),
                order.getOrderName(),
                order.getOrderAmount().getAmount().longValue(),
                buyerName,
                buyerEmail
        );
    }
}
