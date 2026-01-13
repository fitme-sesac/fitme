package com.example.pproject.product.entity;

import com.example.pproject.Constant.ProductType;
import com.example.pproject.Constant.SaleStatus;
import com.example.pproject.common.entity.BaseSoftDeleteEntity;
import com.example.pproject.common.vo.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.util.Assert;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// @EntityListeners(AuditingEntityListener.class) // BaseTimeEntity에 포함되어 있으므로 제거
@SQLDelete(sql = "UPDATE product SET deleted_at = now() WHERE product_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Product extends BaseSoftDeleteEntity { // 상속 추가

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

    // Money Value Object
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "price_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money price;


    @Enumerated(EnumType.STRING)
    @Column(name = "product_type")
    private ProductType productType;

    @Column(name = "credit_amount")
    private Integer creditAmount;

    @Column(name = "plan_tier", length = 20)
    private String planTier;

    // createdAt, updatedAt, deletedAt 필드는 BaseSoftDeleteEntity(및 BaseTimeEntity)로 이동되어 삭제함

    @Builder
    public Product(String productCode, String name, Money price, ProductType productType, Integer creditAmount, String planTier) {

        // 1. 공통 필수값 검증
        Assert.hasText(productCode, "상품 코드는 필수입니다.");
        Assert.hasText(name, "상품명은 필수입니다.");
        Assert.notNull(price, "가격은 필수입니다.");
        Assert.notNull(productType, "상품 유형은 필수입니다.");

        // 2. ★ 타입별 특화 검증 (비즈니스 규칙)
        if (productType == ProductType.SUBSCRIPTION) {
            Assert.hasText(planTier, "구독 상품은 플랜 등급(planTier)이 필수입니다.");
        }
        if (productType == ProductType.ONE_TIME) {
            // (선택사항) 단건 결제는 보통 크레딧 충전용이므로 크레딧 필수 체크
            if (creditAmount == null || creditAmount <= 0) {
                throw new IllegalArgumentException("단건 상품은 크레딧 제공량이 필수입니다.");
            }
        }

        this.productCode = productCode;
        this.name = name;
        this.price = price;
        this.productType = productType;
        this.creditAmount = creditAmount;
        this.planTier = planTier;
        this.saleStatus = SaleStatus.ON_SALE;
    }

    // === 비즈니스 로직 (State Transition Logic) ===

    public void changePrice(Money newPrice) {
        verifyActiveOrPaused(); // [3] STOPPED 상태인지 공통 검증
        Assert.notNull(newPrice, "변경할 가격은 필수입니다.");

        this.price.checkCurrency(newPrice);
        this.price = newPrice;
    }

    public void pause() {
        verifyActiveOrPaused(); // STOPPED이면 못 바꿈
        if (this.saleStatus == SaleStatus.PAUSED) {

            throw new IllegalStateException("이미 일시 정지된 상품입니다.");
        }
        this.saleStatus = SaleStatus.PAUSED;
    }

    public void resume() {
        verifyActiveOrPaused(); // STOPPED이면 못 바꿈
        if (this.saleStatus == SaleStatus.ON_SALE) {
            throw new IllegalStateException("이미 판매 중인 상품입니다.");
        }
        this.saleStatus = SaleStatus.ON_SALE;
    }

    public void stop() {
        // 이미 STOPPED인 경우 굳이 에러낼 필요 없음 (멱등성)
        if (this.saleStatus == SaleStatus.STOPPED) {
            return;
        }
        this.saleStatus = SaleStatus.STOPPED;
    }

    // === 내부 헬퍼 메서드 ===

    // "판매 종료(STOPPED) 상태에서는 아무것도 못한다"는 규칙을 한곳에서 관리
    private void verifyActiveOrPaused() {
        if (this.saleStatus == SaleStatus.STOPPED) {
            throw new IllegalStateException("판매가 종료된 상품은 상태를 변경할 수 없습니다.");
        }
    }
}