package com.example.pproject.wallet.entity;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.Constant.WalletStatus;
import com.example.pproject.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

/**
 * 사용자 또는 고용주의 지갑 엔티티.
 * <p>
 * 잔액(Balance)을 관리하며, 충전(Charge) 및 사용(Use) 기능을 제공합니다.
 * 동시성 제어를 위해 낙관적 락(@Version)을 사용합니다.
 * </p>
 */
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
    private Long member;

    @Column(name = "employer_id")
    private Long employer;

    @Column(name = "balance", nullable = false)
    private Long balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WalletStatus status;

    @Version
    private Long version;

    @Builder
    public Wallet(RoleType ownerType, Long member, Long employer) {
        validateOwner(ownerType, member, employer);

        this.ownerType = ownerType;
        this.member = member;
        this.employer = employer;
        this.balance = 0L;
        this.status = WalletStatus.ACTIVE;
    }

    // === 비즈니스 로직 ===

    /**
     * 지갑 잔액을 충전합니다.
     *
     * @param amount 충전할 금액 (0보다 커야 함)
     * @throws IllegalStateException 지갑이 정지 상태이거나, 보유 한도(Long.MAX_VALUE)를 초과할 경우
     * @throws IllegalArgumentException 충전액이 0 이하일 경우
     */
    public void charge(long amount) {
        verifyActive();
        Assert.isTrue(amount > 0, "충전액은 0보다 커야 합니다.");
        
        if (Long.MAX_VALUE - this.balance < amount) {
            throw new IllegalStateException("지갑 보유 한도를 초과했습니다.");
        }
        
        this.balance += amount;
    }

    /**
     * 지갑 잔액을 사용합니다.
     *
     * @param amount 사용할 금액 (0보다 커야 함)
     * @throws IllegalStateException 지갑이 정지 상태이거나, 잔액이 부족할 경우
     * @throws IllegalArgumentException 사용액이 0 이하일 경우
     */
    public void use(long amount) {
        verifyActive();
        Assert.isTrue(amount > 0, "사용액은 0보다 커야 합니다.");
        
        if (this.balance < amount) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }
        this.balance -= amount;
    }

    /**
     * 정지된 지갑을 다시 활성화(재개)합니다.
     * 이미 활성 상태라면 아무 동작도 하지 않습니다.
     */
    public void resume() {
        if (this.status != WalletStatus.ACTIVE) {
            this.status = WalletStatus.ACTIVE;
        }
    }

    /**
     * 지갑을 일시 정지(동결)시킵니다.
     * 정지된 지갑은 충전 및 사용이 불가능합니다.
     */
    public void suspend() {
        if (this.status != WalletStatus.INACTIVE) {
            this.status = WalletStatus.INACTIVE;
        }
    }

    // === 내부 헬퍼 메서드 ===

    /**
     * 지갑이 활성 상태인지 검증합니다.
     */
    private void verifyActive() {
        if (this.status != WalletStatus.ACTIVE) {
            throw new IllegalStateException("정지된 지갑은 사용할 수 없습니다.");
        }
    }

    /**
     * 지갑 소유자 정보의 유효성을 검증합니다.
     * RoleType에 따라 memberId 또는 employerId 중 하나가 필수여야 합니다.
     */
    private void validateOwner(RoleType ownerType, Long member, Long employer) {
        Assert.notNull(ownerType, "지갑 소유자 타입은 필수입니다.");

        if (ownerType == RoleType.CANDIDATE) {
            Assert.notNull(member, "후보자 지갑은 memberId가 필수입니다.");
            Assert.isNull(employer, "후보자 지갑에는 employerId가 없어야 합니다.");
        } else if (ownerType == RoleType.EMPLOYER) {
            Assert.notNull(employer, "고용주 지갑은 employerId가 필수입니다.");
            Assert.isNull(member, "고용주 지갑에는 memberId가 없어야 합니다.");
        } else {
            if (member == null && employer == null) {
                throw new IllegalArgumentException("지갑 소유자 ID가 필요합니다.");
            }
        }
    }
}
