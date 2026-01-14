package com.example.pproject.wallet.entity;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.Constant.WalletStatus;
import com.example.pproject.common.entity.BaseSoftDeleteEntity;
import com.example.pproject.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "wallet")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type")
    private RoleType ownerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private UserEntity member; // memberId -> member 로 변경

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "employer_id")
//    private Employer employer;

    @Column(name = "balance")
    private int balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WalletStatus status;

    @Version // 낙관적 락을 위한 버전 필드 추가
    private Long version;
}
