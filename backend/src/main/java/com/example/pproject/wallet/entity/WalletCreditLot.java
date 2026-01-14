package com.example.pproject.wallet.entity;

import com.example.pproject.common.entity.BaseSoftDeleteEntity;
import com.example.pproject.common.vo.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "wallet_credit_lot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletCreditLot extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lotId;

    @ManyToOne
    @JoinColumn(name = "wallet_id")
    private Wallet walletId;

//    @ManyToOne
//    @JoinColumn(name = "payment_id")
//    private Payment paymentId;

    @Column(name = "granted_credit")
    private int grantedCredit;

    @Column(name = "remaining_credit")
    private int remainingCredit;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "unit_price_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money price;
}
