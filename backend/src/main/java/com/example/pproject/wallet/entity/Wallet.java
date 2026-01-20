package com.example.pproject.wallet.entity;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.Constant.WalletStatus;
import com.example.pproject.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Check;
import org.springframework.util.Assert;

@Entity
@DynamicUpdate
@Table(
        name = "wallet",
        indexes = {
                @Index(name = "uq_wallet_member_one", columnList = "member_id", unique = true),
                @Index(name = "uq_wallet_employer_one", columnList = "employer_id", unique = true)
        }
)
// DB가 CHECK 제약 지원 시 강력 추천
@Check(constraints = """
        (owner_type = 'CANDIDATE' AND member_id IS NOT NULL AND employer_id IS NULL)
        OR
        (owner_type = 'EMPLOYER' AND employer_id IS NOT NULL AND member_id IS NULL)
        """)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private RoleType ownerType;

    @Column(name = "member_id")
    private Long member;

    @Column(name = "employer_id")
    private Long employer;

    @Column(name = "balance", nullable = false)
    private long balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WalletStatus status;

    @Builder
    public Wallet(RoleType ownerType, Long member, Long employer) {
        validateOwner(ownerType, member, employer);
        this.ownerType = ownerType;
        this.member = member;
        this.employer = employer;
        this.balance = 0L;
        this.status = WalletStatus.ACTIVE;
    }

    public void charge(long amount) {
        verifyActive();
        Assert.isTrue(amount > 0, "충전액은 0보다 커야 합니다.");

        if (Long.MAX_VALUE - this.balance < amount) {
            throw new IllegalStateException("지갑 보유 한도를 초과했습니다.");
        }
        this.balance += amount;
    }

    public void use(long amount) {
        verifyActive();
        Assert.isTrue(amount > 0, "사용액은 0보다 커야 합니다.");

        if (this.balance < amount) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }
        this.balance -= amount;
    }

    public void resume() {
        if (this.status != WalletStatus.ACTIVE) {
            this.status = WalletStatus.ACTIVE;
        }
    }

    public void suspend() {
        if (this.status != WalletStatus.INACTIVE) {
            this.status = WalletStatus.INACTIVE;
        }
    }

    private void verifyActive() {
        if (this.status != WalletStatus.ACTIVE) {
            throw new IllegalStateException("정지된 지갑은 사용할 수 없습니다.");
        }
    }

    private void validateOwner(RoleType ownerType, Long member, Long employer) {
        Assert.notNull(ownerType, "지갑 소유자 타입은 필수입니다.");

        if (ownerType == RoleType.CANDIDATE) {
            Assert.notNull(member, "일반 회원 지갑은 memberId가 필수입니다.");
            Assert.isNull(employer, "일반 회원 지갑에는 employerId가 없어야 합니다.");
        } else if (ownerType == RoleType.EMPLOYER) {
            Assert.notNull(employer, "기업 지갑은 employerId가 필수입니다.");
            Assert.isNull(member, "기업 지갑에는 memberId가 없어야 합니다.");
        } else {
            throw new IllegalArgumentException("지갑은 CANDIDATE 또는 EMPLOYER만 소유할 수 있습니다.");
        }
    }
}
