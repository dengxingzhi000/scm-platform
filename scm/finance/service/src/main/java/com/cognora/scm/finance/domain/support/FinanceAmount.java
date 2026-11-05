package com.cognora.scm.finance.domain.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 财务金额口径。
 *
 * <p>财务表全部是 {@code DECIMAL(15,2)} / {@code DECIMAL(12,2)} / {@code DECIMAL(10,2)}，
 * 因此金额只允许两位小数，超过两位一律拒绝而不是静默四舍五入——静默取整会让
 * 「应付 100.00、实付 100.005」这种分录凭空多出半分钱且无法对账。</p>
 *
 * <p>允许的业务计算中间值（例如按比例分摊）在写入前仍需经过这里做一次
 * {@link #normalize(BigDecimal, String)}，保证落库只有两位小数。</p>
 */
public final class FinanceAmount {

    /** 财务金额的小数位。 */
    public static final int SCALE = 2;

    /** 税率字段（DECIMAL(5,4)）的小数位。 */
    public static final int RATE_SCALE = 4;

    /** 单笔金额上限，与支付渠道口径一致；超过它只可能是录入错误。 */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private FinanceAmount() {
    }

    /**
     * 必填金额：非空、非负、最多两位小数。
     */
    public static BigDecimal required(BigDecimal value, String field) {
        return nonNegative(value, field);
    }

    /**
     * 必填金额且必须大于 0（付款、开票金额）。
     */
    public static BigDecimal positive(BigDecimal value, String field) {
        BigDecimal normalized = nonNegative(value, field);
        if (normalized.signum() == 0) {
            throw new IllegalArgumentException(field + "必须大于0");
        }
        return normalized;
    }

    /**
     * 非空且非负的金额。
     */
    public static BigDecimal nonNegative(BigDecimal value, String field) {
        BigDecimal normalized = normalize(value, field);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(field + "不能为负数: " + value.toPlainString());
        }
        return normalized;
    }

    /**
     * 可正可负的金额（结算调整额这类允许冲减的字段）。
     */
    public static BigDecimal signed(BigDecimal value, String field) {
        return normalize(value, field);
    }

    /**
     * 可选金额：为空时返回 {@code null}。
     */
    public static BigDecimal optional(BigDecimal value, String field) {
        return value == null ? null : normalize(value, field);
    }

    /**
     * 可选金额：为空时按 0.00 处理（数据库列普遍带 DEFAULT 0）。
     */
    public static BigDecimal optionalOrZero(BigDecimal value, String field) {
        return value == null ? ZERO : normalize(value, field);
    }

    /**
     * 税率：0..1、最多四位小数。
     */
    public static BigDecimal rate(BigDecimal value, String field) {
        if (value == null) {
            return null;
        }
        if (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(field + "必须在 0 到 1 之间: " + value.toPlainString());
        }
        if (scaleOf(value) > RATE_SCALE) {
            throw new IllegalArgumentException(field + "最多允许" + RATE_SCALE + "位小数: " + value.toPlainString());
        }
        return value.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 统一到两位小数；超过两位有效小数直接拒绝。
     */
    public static BigDecimal normalize(BigDecimal value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        if (scaleOf(value) > SCALE) {
            throw new IllegalArgumentException(field + "最多允许" + SCALE + "位小数: " + value.toPlainString());
        }
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException(field + "超出上限" + MAX_AMOUNT.toPlainString() + ": " + value.toPlainString());
        }
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 有效小数位：{@code 1.500} 视为两位，避免因尾随零误拒。
     */
    private static int scaleOf(BigDecimal value) {
        return value.stripTrailingZeros().scale();
    }
}
