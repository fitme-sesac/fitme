package com.example.pproject.subscription.entity;

import com.example.pproject.Constant.PlanTier;
import com.example.pproject.Constant.SubscriptionStatus;
import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.common.vo.Money;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Subscription extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long subscriptionId;

    @ManyToOne
    @JoinColumn(name = "employer_id")
    private EmployerEntity employer;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SubscriptionStatus status;

    @Column(name = "started_at")
    private Instant staredAt;

    @Column(name = "next_billing_at")
    private Instant nextBillingAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "customer_key")
    private String customerKey;

    @Column(name = "billing_key")
    private String billingKey;

    @Column(name = "card_company")
    private String cardCompany;

    @Column(name = "card_number")
    private String cardNumber;

}
