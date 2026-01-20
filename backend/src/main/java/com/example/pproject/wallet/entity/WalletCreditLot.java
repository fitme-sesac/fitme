package com.example.pproject.wallet.entity;

import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.common.vo.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.springframework.util.Assert;

@Entity
@Table(
        name = "wallet_credit_lot",
        indexes = {
                // FIFO 차감 + 남은 lot 조회 최적화
                @Index(name = "idx_lot_wallet_remain_created", columnList = "wallet_id, remaining_credit, created_at"),
                // 결제 멱등성 (unique를 DB에 잡아두는 게 제일 안전)
                @Index(name = "uq_lot_payment_id", columnList = "payment_id", unique = true)
        }
)
@Check(constraints = "remaining_credit >= 0 AND granted_credit > 0")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletCreditLot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lot_id")
    private Long lotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    // 결제 기반 충전이면 보통 NOT NULL이 더 안전하지만
    // 관리자 지급 등도 고려하면 nullable 유지 가능
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "granted_credit", nullable = false)
    private long grantedCredit;

    @Column(name = "remaining_credit", nullable = false)
    private long remainingCredit;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "unit_price_amount", nullable = false)),
            @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 10))
    })
    private Money price;

    @Builder
    public WalletCreditLot(Wallet wallet, Long paymentId, long grantedCredit, Money price) {
        Assert.notNull(wallet, "지갑 정보는 필수입니다.");
        Assert.isTrue(grantedCredit > 0, "제공 크레딧은 0보다 커야 합니다.");
        Assert.notNull(price, "가격 정보는 필수입니다.");

        this.wallet = wallet;
        this.paymentId = paymentId;
        this.grantedCredit = grantedCredit;
        this.remainingCredit = grantedCredit;
        this.price = price;
    }

    public void consume(long amount) {
        Assert.isTrue(amount > 0, "소비할 크레딧은 0보다 커야 합니다.");

        if (this.remainingCredit < amount) {
            throw new IllegalStateException("잔여 크레딧이 부족합니다.");
        }
        this.remainingCredit -= amount;
    }

    public boolean isRefundable() {
        return this.remainingCredit == this.grantedCredit;
    }

    public boolean isExhausted() {
        return this.remainingCredit <= 0;
    }
}
