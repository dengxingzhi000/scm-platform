package com.scmcloud.common.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Money value object. Immutable, scale-2, null-safe arithmetic.
 *
 * <p>Replaces raw {@code BigDecimal} for all monetary fields.
 * All operations return new instances. {@code null} inputs are treated as zero.</p>
 *
 * <pre>
 * Money total = Money.of(item1.getSubtotal()).add(item2.getSubtotal());
 * Money payable = total.subtract(discount).add(freight);
 * boolean sufficient = paid.compareTo(payable) >= 0;
 * </pre>
 */
public final class Money implements Comparable<Money>, Serializable {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public static Money of(String amount) {
        if (amount == null || amount.isBlank()) {
            return ZERO;
        }
        return new Money(new BigDecimal(amount));
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Money add(Money other) {
        if (other == null || other.isZero()) return this;
        return new Money(this.amount.add(other.amount));
    }

    public Money add(BigDecimal other) {
        if (other == null || other.signum() == 0) return this;
        return new Money(this.amount.add(other));
    }

    public Money subtract(Money other) {
        if (other == null || other.isZero()) return this;
        return new Money(this.amount.subtract(other.amount));
    }

    public Money subtract(BigDecimal other) {
        if (other == null || other.signum() == 0) return this;
        return new Money(this.amount.subtract(other));
    }

    public Money multiply(int multiplier) {
        if (multiplier == 1) return this;
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)));
    }

    public Money multiply(BigDecimal multiplier) {
        if (multiplier == null) return ZERO;
        return new Money(this.amount.multiply(multiplier));
    }

    public Money divide(BigDecimal divisor, RoundingMode roundingMode) {
        if (divisor == null || divisor.signum() == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return new Money(this.amount.divide(divisor, 2, roundingMode));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public Money negate() {
        return new Money(amount.negate());
    }

    public Money abs() {
        return new Money(amount.abs());
    }

    @Override
    public int compareTo(Money other) {
        if (other == null) return 1;
        return this.amount.compareTo(other.amount);
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return compareTo(other) >= 0;
    }

    /**
     * Unwrap to BigDecimal for persistence / Redis / serialization boundaries.
     */
    public BigDecimal toBigDecimal() {
        return amount;
    }

    /**
     * Unwrap to plain string for JSON serialization.
     */
    public String toPlainString() {
        return amount.toPlainString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return "¥" + amount.toPlainString();
    }
}
