package com.scmcloud.common.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Quantity value object. Immutable, non-negative, null-safe.
 *
 * <p>Replaces raw {@code Integer} for stock, order quantity, and similar fields.
 * Supports both integer quantities (most cases) and fractional quantities (purchase module).</p>
 *
 * <pre>
 * Quantity stock = Quantity.of(100);
 * Quantity reserved = Quantity.of(10);
 * Quantity available = stock.subtract(reserved);
 * boolean sufficient = available.isGreaterThanOrEqual(needed);
 * </pre>
 */
public final class Quantity implements Comparable<Quantity>, Serializable {

    public static final Quantity ZERO = new Quantity(0);
    public static final Quantity ONE = new Quantity(1);

    private final int value;

    private Quantity(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + value);
        }
        this.value = value;
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public static Quantity ofNullable(Integer value) {
        return value == null ? ZERO : new Quantity(value);
    }

    public static Quantity ofDecimal(BigDecimal value) {
        if (value == null || value.signum() <= 0) return ZERO;
        return new Quantity(value.setScale(0, RoundingMode.CEILING).intValue());
    }

    public int getValue() {
        return value;
    }

    public Quantity add(Quantity other) {
        if (other == null || other.isZero()) return this;
        return new Quantity(this.value + other.value);
    }

    public Quantity add(int other) {
        if (other == 0) return this;
        return new Quantity(this.value + other);
    }

    public Quantity subtract(Quantity other) {
        if (other == null || other.isZero()) return this;
        int result = this.value - other.value;
        if (result < 0) {
            throw new ArithmeticException("Quantity underflow: " + this.value + " - " + other.value);
        }
        return new Quantity(result);
    }

    public Quantity subtract(int other) {
        int result = this.value - other;
        if (result < 0) {
            throw new ArithmeticException("Quantity underflow: " + this.value + " - " + other);
        }
        return new Quantity(result);
    }

    public Quantity multiply(int multiplier) {
        if (multiplier == 1) return this;
        return new Quantity(this.value * multiplier);
    }

    public boolean isZero() {
        return value == 0;
    }

    public boolean isPositive() {
        return value > 0;
    }

    @Override
    public int compareTo(Quantity other) {
        if (other == null) return 1;
        return Integer.compare(this.value, other.value);
    }

    public boolean isGreaterThan(Quantity other) {
        return compareTo(other) > 0;
    }

    public boolean isLessThan(Quantity other) {
        return compareTo(other) < 0;
    }

    public boolean isGreaterThanOrEqual(Quantity other) {
        return compareTo(other) >= 0;
    }

    /**
     * Unwrap to int for persistence / Redis Lua scripts / DTO boundaries.
     */
    public int toInt() {
        return value;
    }

    /**
     * Unwrap to Integer for MyBatis-Plus mapping.
     */
    public Integer toInteger() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quantity quantity)) return false;
        return value == quantity.value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
