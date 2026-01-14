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

    @Column(name = "owner_type")
    private RoleType ownerType;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private UserEntity memberId;

//    @ManyToOne                            // 추후 추가 예정
//    @Column(name = "employer_id")
//    private Employer employer;

    @Column(name = "balance")
    private int balance;

    @Column(name = "status")
    private WalletStatus status;

}
