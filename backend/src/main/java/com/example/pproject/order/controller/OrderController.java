package com.example.pproject.order.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.Constant.BuyerType;
import com.example.pproject.Constant.RoleType;
import com.example.pproject.common.vo.Money;
import com.example.pproject.order.dto.request.OrderCreateRequest;
import com.example.pproject.order.dto.response.OrderCreateResponse;
import com.example.pproject.order.dto.response.OrderResponse;
import com.example.pproject.order.entity.Orders;
import com.example.pproject.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성
     */
    @PostMapping
    public ResponseEntity<OrderCreateResponse> createOrder(
            @AuthenticationPrincipal JwtUserPrincipal userPrincipal,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        Long buyerId = Long.parseLong(userPrincipal.getUserid());
        
        Orders order = orderService.createOrder(
                buyerId,
                request.buyerType(),
                request.productCode(),
                request.idempotencyKey()
        );

        String buyerName = "";
        String buyerEmail = "";

        if (request.buyerType() == BuyerType.MEMBER) {
            if (order.getBuyerMember() != null) {
                buyerName = order.getBuyerMember().getUsername();
                buyerEmail = order.getBuyerMember().getEmail();
            }
        } else if (request.buyerType() == BuyerType.EMPLOYER) {
            if (order.getBuyerEmployer() != null) {
                buyerName = order.getBuyerEmployer().getName();
                buyerEmail = order.getBuyerEmployer().getContactEmail();
            }
        }

        return ResponseEntity.ok(OrderCreateResponse.from(order, buyerName, buyerEmail));
    }

    /**
     * 주문 조회
     */
    @GetMapping("/{orderUid}")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal JwtUserPrincipal userPrincipal,
            @PathVariable UUID orderUid
    ) {
        Long userId = Long.parseLong(userPrincipal.getUserid());
        Orders order = orderService.getOrder(orderUid, userId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    /**
     * 주문 완료 (결제 성공)
     */
    @PostMapping("/{orderUid}/complete")
    public ResponseEntity<Void> completeOrder(
            @PathVariable UUID orderUid,
            @RequestBody Map<String, BigDecimal> request
    ) {
        BigDecimal amount = request.get("amount");
        if (amount == null) {
            throw new IllegalArgumentException("결제 금액은 필수입니다.");
        }
        
        orderService.completeOrder(orderUid, Money.wons(amount));
        return ResponseEntity.ok().build();
    }

    /**
     * 주문 취소
     */
    @PostMapping("/{orderUid}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @AuthenticationPrincipal JwtUserPrincipal userPrincipal,
            @PathVariable UUID orderUid
    ) {
        Long userId = Long.parseLong(userPrincipal.getUserid());
        orderService.cancelOrder(orderUid, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 주문 실패
     */
    @PostMapping("/{orderUid}/fail")
    public ResponseEntity<Void> failOrder(
            @PathVariable UUID orderUid
    ) {
        orderService.failOrder(orderUid);
        return ResponseEntity.ok().build();
    }
}
