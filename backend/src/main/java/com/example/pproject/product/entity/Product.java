package com.example.pproject.product.entity;

import com.example.pproject.Constant.ProductType;
import com.example.pproject.Constant.SaleStatus;
import com.example.pproject.common.vo.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class) // 생성/수정 시간 자동 관리
// PostgreSQL의 now()는 TIMESTAMPTZ를 반환하므로 호환됨
@SQLDelete(sql = "UPDATE product SET deleted_at = now() WHERE product_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_code", nullable = false, unique = true)
    private String productCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", nullable = false)
    private SaleStatus saleStatus;

    // Money Value Object (기존 유지)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "price_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money price;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type")
    private ProductType productType;

    // === ★ Instant로 변경된 Auditing Fields ===

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // TIMESTAMPTZ 매핑

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt; // TIMESTAMPTZ 매핑

    @Column(name = "deleted_at")
    private Instant deletedAt; // TIMESTAMPTZ 매핑

    @Builder
    public Product(String productCode, String name, Money price, ProductType productType) {
        this.productCode = productCode;
        this.name = name;
        this.price = price;
        this.productType = productType;
        this.saleStatus = SaleStatus.ON_SALE;
    }

    // === 비즈니스 로직 ===

    public void changePrice(Money newPrice) {
        if (this.saleStatus == SaleStatus.STOPPED) {
            throw new IllegalStateException("판매 종료된 상품입니다.");
        }
        this.price.checkCurrency(newPrice);
        this.price = newPrice;
        // updatedAt은 @LastModifiedDate에 의해 트랜잭션 커밋 시 자동 갱신되지만,
        // 명시적으로 갱신하고 싶다면 아래처럼 할 수 있음 (보통은 생략 가능)
        // this.updatedAt = Instant.now();
    }

    public void pause() {
        this.saleStatus = SaleStatus.PAUSED;
    }
}