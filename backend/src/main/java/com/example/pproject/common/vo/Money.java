package com.example.pproject.common.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
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
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("통화가 일치하지 않습니다.");
        }
    }
}
