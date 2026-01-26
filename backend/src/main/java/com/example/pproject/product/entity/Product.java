package com.example.pproject.product.entity;

import com.example.pproject.Constant.BuyerType;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Builder
    public Product(String productCode, String name, Money price, ProductType productType, Integer creditAmount,
            String planTier) {
        validateCommonFields(productCode, name, price, productType);
        validateProductTypeSpecificFields(productType, creditAmount, planTier);

        this.productCode = productCode;
        this.name = name;
        this.price = price;
        this.productType = productType;
        this.creditAmount = creditAmount;
        this.planTier = planTier;
        this.saleStatus = SaleStatus.ON_SALE;
    }

    // ===========================================
    // 생성자 검증 로직 (Constructor Validation)
    // ===========================================

    private void validateCommonFields(String productCode, String name, Money price, ProductType productType) {
        Assert.hasText(productCode, "상품 코드는 필수입니다.");
        Assert.hasText(name, "상품명은 필수입니다.");
        Assert.notNull(price, "가격은 필수입니다.");
        Assert.notNull(productType, "상품 유형은 필수입니다.");
    }

    private void validateProductTypeSpecificFields(ProductType productType, Integer creditAmount, String planTier) {
        if (productType == ProductType.SUBSCRIPTION) {
            Assert.hasText(planTier, "구독 상품은 플랜 등급(planTier)이 필수입니다.");
        }
        if (productType == ProductType.ONE_TIME) {
            if (creditAmount == null || creditAmount <= 0) {
                throw new IllegalArgumentException("단건 상품은 크레딧 제공량이 필수입니다.");
            }
        }
    }

    // ===========================================
    // 구매자 자격 검증 (Buyer Eligibility)
    // ===========================================

    /**
     * 특정 구매자 유형이 이 상품을 구매할 수 있는지 확인
     *
     * @param buyerType 구매자 유형
     * @return 구매 가능 여부
     */
    public boolean isAvailableFor(BuyerType buyerType) {
        if (this.productType == ProductType.SUBSCRIPTION) {
            return buyerType == BuyerType.EMPLOYER;
        }
        // ONE_TIME 상품은 누구나 구매 가능
        return true;
    }

    /**
     * 구매자 자격 검증 (구매 불가 시 예외 발생)
     *
     * @param buyerType 구매자 유형
     * @throws IllegalStateException 구매 자격이 없는 경우
     */
    public void validateBuyerEligibility(BuyerType buyerType) {
        if (!isAvailableFor(buyerType)) {
            throw new IllegalStateException("구독 상품은 기업 회원만 구매할 수 있습니다.");
        }
    }

    // ===========================================
    // 상태 전이 가능 여부 확인 (State Transition Guards)
    // ===========================================

    /**
     * 일시정지 가능 여부 확인
     */
    public boolean canPause() {
        return this.saleStatus == SaleStatus.ON_SALE;
    }

    /**
     * 재개 가능 여부 확인
     */
    public boolean canResume() {
        return this.saleStatus == SaleStatus.PAUSED;
    }

    /**
     * 판매 종료 가능 여부 확인
     */
    public boolean canStop() {
        return this.saleStatus != SaleStatus.STOPPED;
    }

    /**
     * 가격 변경 가능 여부 확인
     */
    public boolean canChangePrice() {
        return this.saleStatus != SaleStatus.STOPPED;
    }

    // ===========================================
    // 비즈니스 로직 (State Transition Logic)
    // ===========================================

    /**
     * 상품 가격 변경
     */
    public void changePrice(Money newPrice) {
        if (!canChangePrice()) {
            throw new IllegalStateException("판매가 종료된 상품은 가격을 변경할 수 없습니다.");
        }
        Assert.notNull(newPrice, "변경할 가격은 필수입니다.");
        this.price.checkCurrency(newPrice);
        this.price = newPrice;
    }

    /**
     * 상품 일시정지
     */
    public void pause() {
        // 멱등성: 이미 일시정지 상태면 무시
        if (this.saleStatus == SaleStatus.PAUSED) {
            return;
        }
        if (!canPause()) {
            throw new IllegalStateException("판매 중인 상품만 일시정지할 수 있습니다.");
        }
        this.saleStatus = SaleStatus.PAUSED;
    }

    /**
     * 상품 판매 재개
     */
    public void resume() {
        // 멱등성: 이미 판매 중이면 무시
        if (this.saleStatus == SaleStatus.ON_SALE) {
            return;
        }
        if (!canResume()) {
            throw new IllegalStateException("일시정지된 상품만 재개할 수 있습니다.");
        }
        this.saleStatus = SaleStatus.ON_SALE;
    }

    /**
     * 상품 판매 종료
     */
    public void stop() {
        // 멱등성: 이미 STOPPED인 경우 무시
        if (this.saleStatus == SaleStatus.STOPPED) {
            return;
        }
        this.saleStatus = SaleStatus.STOPPED;
    }

    // ===========================================
    // 상태 확인 메서드 (Query Methods)
    // ===========================================

    /**
     * 구독 상품인지 확인
     */
    public boolean isSubscription() {
        return this.productType == ProductType.SUBSCRIPTION;
    }

    /**
     * 단건 상품인지 확인
     */
    public boolean isOneTime() {
        return this.productType == ProductType.ONE_TIME;
    }

    /**
     * 판매 중인지 확인
     */
    public boolean isOnSale() {
        return this.saleStatus == SaleStatus.ON_SALE;
    }

    /**
     * 일시정지 상태인지 확인
     */
    public boolean isPaused() {
        return this.saleStatus == SaleStatus.PAUSED;
    }

    /**
     * 판매 종료 상태인지 확인
     */
    public boolean isStopped() {
        return this.saleStatus == SaleStatus.STOPPED;
    }

    /**
     * 구매 가능한 상태인지 확인 (판매 중인 경우)
     */
    public boolean isPurchasable() {
        return isOnSale();
    }
}
