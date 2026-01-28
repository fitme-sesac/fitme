package com.example.pproject.common.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {

    public static final Money ZERO = Money.wons(0);

    private BigDecimal amount;
    private String currency;

    // 생성자: 여기서 "돈"에 대한 무결성을 보장합니다.
    public Money(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("금액은 필수 값입니다.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 0보다 작을 수 없습니다.");
        }
        this.amount = amount;
        this.currency = (currency == null || currency.isBlank()) ? "KRW" : currency;
    }

    public static Money wons(long amount) {
        return new Money(BigDecimal.valueOf(amount), "KRW");
    }

    public static Money wons(BigDecimal amount) {
        return new Money(amount, "KRW");
    }

    // 통화 일치 검증 로직
    public void checkCurrency(Money other) {
        if (other == null) {
            throw new IllegalArgumentException("비교 대상 금액은 필수 입니다.");
        }
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("통화가 일치하지 않습니다.");
        }
    }

    // BigDecimal의 scale 차이로 인한 비교 문제 해결
    // 5000.00과 5000은 값이 같으므로 compareTo()로 비교
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Money money = (Money) o;
        return this.amount.compareTo(money.amount) == 0
                && Objects.equals(this.currency, money.currency);
    }

    @Override
    public int hashCode() {
        // BigDecimal의 hashCode는 scale에 따라 다르므로,
        // stripTrailingZeros()로 정규화하여 해시코드 생성
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return String.format("Money{amount=%s, currency='%s'}", amount, currency);
    }
}
