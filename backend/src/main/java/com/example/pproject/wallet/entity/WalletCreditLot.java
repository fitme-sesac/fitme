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
     * 크레딧을 소비(차감)합니다.
     *
     * @param amount 소비할 크레딧 양 (0보다 커야 함)
     * @throws IllegalStateException 잔여 크레딧이 부족할 경우
     * @throws IllegalArgumentException 소비량이 0 이하일 경우
     */
    public void consume(long amount) {
        Assert.isTrue(amount > 0, "소비할 크레딧은 0보다 커야 합니다.");
        
        if (this.remainingCredit < amount) {
            throw new IllegalStateException("잔여 크레딧이 부족합니다.");
        }
        this.remainingCredit -= amount;
    }

    /**
     * 환불 가능 여부를 확인합니다.
     * <p>
     * 크레딧을 전혀 사용하지 않은 상태(잔여량 == 제공량)여야 환불이 가능합니다.
     * </p>
     * @return 환불 가능하면 true, 아니면 false
     */
    public boolean isRefundable() {
        return this.remainingCredit.equals(this.grantedCredit);
    }
    
    /**
     * 크레딧이 모두 소진되었는지 확인합니다.
     * @return 잔여량이 0 이하면 true
     */
    public boolean isExhausted() {
        return this.remainingCredit <= 0;
    }
}
