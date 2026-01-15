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

    /**
     * Creates a Money value while enforcing amount and currency invariants.
     *
     * @param amount   the monetary amount; must not be null and must be greater than or equal to zero
     * @param currency the ISO currency code to use; if null or blank, defaults to "KRW"
     * @throws IllegalArgumentException if {@code amount} is null or is less than zero
     */
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

    /**
     * Create a Money instance representing the given amount in Korean won.
     *
     * @param amount the monetary amount in whole won
     * @return the Money instance for the specified amount in KRW
     */
    public static Money wons(long amount) {
        return new Money(BigDecimal.valueOf(amount), "KRW");
    }

    /**
     * Create a Money instance denominated in South Korean won (KRW) with the specified amount.
     *
     * @param amount the monetary amount in KRW
     * @return a Money instance representing the given amount in KRW
     */
    public static Money wons(BigDecimal amount) {
        return new Money(amount, "KRW");
    }

    /**
     * Validates that this Money uses the same currency as the provided Money.
     *
     * @param other the Money whose currency is compared against this instance
     * @throws IllegalArgumentException if the currencies do not match
     */
    public void checkCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("통화가 일치하지 않습니다.");
        }
    }
}