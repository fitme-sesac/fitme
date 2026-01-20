package com.example.pproject.order.entity;

import com.example.pproject.Constant.OrderStatus;
import com.example.pproject.Constant.RoleType;
import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.common.vo.Money;
import com.example.pproject.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Orders extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_uid", nullable = false, updatable = false)
    private UUID orderUid;

    @Column(name = "buyer_type", nullable = false, length = 20)
    private RoleType buyerType;
    
    @Column(name = "buyer_member_id")
    private Long buyerMemberId;
    
    @Column(name = "buyer_employer_id")
    private Long buyerEmployerId;
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    
    @Column(name = "order_amount")
    private Money orderAmount;
    
    @Column(name = "status")
    private OrderStatus status;
    
    @Column(name = "idempotency_key")
    private String idempotencyKey;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
