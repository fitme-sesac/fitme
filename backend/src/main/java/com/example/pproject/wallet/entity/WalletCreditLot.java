package com.example.pproject.wallet.entity;

import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.common.vo.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @JoinColumn(name = "wallet_id")
    private Wallet wallet; // walletId -> wallet 로 변경

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "payment_id")
//    private Payment payment;

    @Column(name = "granted_credit")
    private int grantedCredit;

    @Column(name = "remaining_credit")
    private int remainingCredit;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "unit_price_amount")),  // 크래딧 단가 금액
            @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money price;
}
