package com.example.pproject.subscription.entity;

import com.example.pproject.Constant.CreditStatus;
import com.example.pproject.Constant.PaymentStatus;
import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.payment.entity.Payment;
import com.example.pproject.wallet.entity.WalletLedger;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "subscription_billing_cycle")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SubscriptionBillingCycle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cycle_id")
    private Long subscriptionBillingCycleId;

    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "billing_month")
    private LocalDate billingMonth;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "credit_grant")
    private Integer creditGrant;

    @ManyToOne
    @JoinColumn(name = "ledger_id")
    private WalletLedger walletLedger;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "credit_status")
    private CreditStatus creditStatus;
}
