package com.example.pproject.order.dto.request;

import com.example.pproject.Constant.OrderStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record OrderSearchRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime startDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime endDate,

        OrderStatus status
) {
}
