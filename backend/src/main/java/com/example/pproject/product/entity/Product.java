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
@SQLDelete(sql = "UPDATE product SET deleted_at = now() WHERE product_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Product extends BaseSoftDeleteEntity {

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

    /**
     * Creates a new Product with the given identity, metadata, pricing, and type-specific configuration.
     *
     * <p>Initializes the product's sale status to ON_SALE.</p>
     *
     * @param productCode unique code for the product; must be a non-empty string
     * @param name display name for the product; must be a non-empty string
     * @param price monetary price for the product; must not be null
     * @param productType type of the product; must not be null
     * @param creditAmount credit quantity provided by the product; required and must be > 0 when {@code productType} is {@code ONE_TIME}; may be null otherwise
     * @param planTier subscription plan tier; required and must be a non-empty string when {@code productType} is {@code SUBSCRIPTION}; may be null otherwise
     * @throws IllegalArgumentException if any required argument is missing or invalid:
     *                                  - {@code productCode} or {@code name} is empty,
     *                                  - {@code price} or {@code productType} is null,
     *                                  - {@code productType} is {@code SUBSCRIPTION} and {@code planTier} is empty,
     *                                  - {@code productType} is {@code ONE_TIME} and {@code creditAmount} is null or <= 0
     */
    @Builder
    public Product(String productCode, String name, Money price, ProductType productType, Integer creditAmount, String planTier) {

        // 1. 공통 필수값 검증
        Assert.hasText(productCode, "상품 코드는 필수입니다.");
        Assert.hasText(name, "상품명은 필수입니다.");
        Assert.notNull(price, "가격은 필수입니다.");
        Assert.notNull(productType, "상품 유형은 필수입니다.");

        if (productType == ProductType.SUBSCRIPTION) {
            Assert.hasText(planTier, "구독 상품은 플랜 등급(planTier)이 필수입니다.");
        }
        if (productType == ProductType.ONE_TIME) {
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

    /**
     * Change the product's price to the provided Money value.
     *
     * @param newPrice the new price to set; must not be null and must use the same currency as the current price
     * @throws IllegalStateException if the product is in the STOPPED state and cannot be modified
     * @throws IllegalArgumentException if {@code newPrice} is null or its currency is incompatible with the current price
     */

    public void changePrice(Money newPrice) {
        verifyActiveOrPaused(); // STOPPED 상태인지 공통 검증
        Assert.notNull(newPrice, "변경할 가격은 필수입니다.");

        this.price.checkCurrency(newPrice);
        this.price = newPrice;
    }

    /**
     * Pause the product's sale status.
     *
     * Sets the product's saleStatus to PAUSED.
     *
     * @throws IllegalStateException if the product is already paused or if the product is stopped and cannot be modified.
     */
    public void pause() {
        verifyActiveOrPaused(); // STOPPED 상태인지 공통 검증
        if (this.saleStatus == SaleStatus.PAUSED) {

            throw new IllegalStateException("이미 일시 정지된 상품입니다.");
        }
        this.saleStatus = SaleStatus.PAUSED;
    }

    /**
     * Sets the product's sale status to ON_SALE.
     *
     * @throws IllegalStateException if the product is STOPPED or already ON_SALE.
     */
    public void resume() {
        verifyActiveOrPaused(); // STOPPED 상태인지 공통 검증
        if (this.saleStatus == SaleStatus.ON_SALE) {
            throw new IllegalStateException("이미 판매 중인 상품입니다.");
        }
        this.saleStatus = SaleStatus.ON_SALE;
    }

    /**
     * Transitions the product into the STOPPED sale status.
     *
     * This operation is idempotent: if the product is already STOPPED it makes no changes.
     */
    public void stop() {
        // 이미 STOPPED인 경우 굳이 에러낼 필요 없음 (멱등성)
        if (this.saleStatus == SaleStatus.STOPPED) {
            return;
        }
        this.saleStatus = SaleStatus.STOPPED;
    }

    // === 내부 헬퍼 메서드 ===

    /**
     * Ensures the product is not in the STOPPED sale status.
     *
     * @throws IllegalStateException if the product's `saleStatus` is `STOPPED` (message: "판매가 종료된 상품은 상태를 변경할 수 없습니다.")
     */
    private void verifyActiveOrPaused() {
        if (this.saleStatus == SaleStatus.STOPPED) {
            throw new IllegalStateException("판매가 종료된 상품은 상태를 변경할 수 없습니다.");
        }
    }
}