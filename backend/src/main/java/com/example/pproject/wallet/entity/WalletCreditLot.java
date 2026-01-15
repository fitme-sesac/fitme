package com.example.pproject.wallet.entity;

import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.common.vo.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

/**
 * 구매한 크레딧 묶음(Lot) 엔티티.
 * <p>
 * 사용자가 구매한 크레딧의 원본 양(grantedCredit)과 잔여 양(remainingCredit)을 관리합니다.
 * 크레딧은 선입선출(FIFO) 또는 만료일 정책에 따라 개별 Lot에서 차감됩니다.
 * </p>
 */
@Entity
@Table(name = "wallet_credit_lot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletCreditLot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lot_id")
    private Long lotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "granted_credit", nullable = false)
    private Long grantedCredit;

    @Column(name = "remaining_credit", nullable = false)
    private Long remainingCredit;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "unit_price_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money price;

    /**
     * Creates a WalletCreditLot representing a purchased bundle of credits tied to the given wallet.
     *
     * Initializes the lot with the specified granted credits and sets remaining credits equal to the granted amount.
     *
     * @param wallet the owning Wallet; must not be null
     * @param grantedCredit the number of credits granted for this lot; must be greater than 0
     * @param price the monetary price for this lot; must not be null
     * @throws IllegalArgumentException if {@code wallet} or {@code price} is null, or if {@code grantedCredit} is null or not greater than 0
     */
    @Builder
    public WalletCreditLot(Wallet wallet, Long grantedCredit, Money price) {
        Assert.notNull(wallet, "지갑 정보는 필수입니다.");
        Assert.isTrue(grantedCredit != null && grantedCredit > 0, "제공 크레딧은 0보다 커야 합니다.");
        Assert.notNull(price, "가격 정보는 필수입니다.");

        this.wallet = wallet;
        this.grantedCredit = grantedCredit;
        this.remainingCredit = grantedCredit;
        this.price = price;
    }

    // === 비즈니스 로직 ===

    /**
     * Subtracts the specified amount of credits from this lot.
     *
     * @param amount the number of credits to consume; must be greater than 0
     * @throws IllegalArgumentException if {@code amount} is less than or equal to 0
     * @throws IllegalStateException if the lot's remaining credits are less than {@code amount}
     */
    public void consume(long amount) {
        Assert.isTrue(amount > 0, "소비할 크레딧은 0보다 커야 합니다.");
        
        if (this.remainingCredit < amount) {
            throw new IllegalStateException("잔여 크레딧이 부족합니다.");
        }
        this.remainingCredit -= amount;
    }

    /**
     * Determine whether this credit lot is eligible for refund.
     *
     * The lot is refundable only when no credits have been consumed (remaining credit equals granted credit).
     *
     * @return `true` if no credits have been consumed (remaining credit equals granted credit), `false` otherwise.
     */
    public boolean isRefundable() {
        return this.remainingCredit.equals(this.grantedCredit);
    }
    
    /**
     * Determines whether the lot has no remaining credits.
     *
     * @return `true` if the remaining credit is less than or equal to 0, `false` otherwise.
     */
    public boolean isExhausted() {
        return this.remainingCredit <= 0;
    }
}