package com.example.pproject.product.entity;

import com.example.pproject.product.entity.enumtype.PlanTier;
import com.example.pproject.product.entity.enumtype.ProductType;
import com.example.pproject.product.entity.enumtype.SaleStatus;
import com.example.pproject.product.entity.vo.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private long productId;

    @Column(name = "product_code", nullable = false, length = 40, unique = true)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 20)
    private ProductType productType;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", nullable = false, length = 20)
    private SaleStatus saleStatus;

    @Embedded
    private Money price_amount;

    @Column(name = "credit_amount")
    private Integer creditAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_tier", length = 20)
    private PlanTier planTier;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updateAt;

    @Column(name = "deleted_at")
    private Instant deleteAt;
}
