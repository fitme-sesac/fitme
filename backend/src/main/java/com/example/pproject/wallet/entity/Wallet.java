package com.example.pproject.wallet.entity;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.Constant.WalletStatus;
import com.example.pproject.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "wallet",
        indexes = {
                @Index(name = "uq_wallet_member_one", columnList = "member_id", unique = true),
                @Index(name = "uq_wallet_employer_one", columnList = "employer_id", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    private RoleType ownerType;

    @Column(name = "member_id")
    private Long member; // memberId

//    @Column(name = "employer_id")
//    private Long employer;

    @Column(name = "balance")
    private Long balance;   // 확장성을 위해 int 대신 Long으로 설정

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WalletStatus status;
}
