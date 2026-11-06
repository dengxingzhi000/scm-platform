package com.cognora.lms.logistics.domain.freight;

import java.math.BigDecimal;

/**
 * 运费对账差异分类（批次 2）。
 *
 * <p>差异 = 承运商账单金额 - 系统应收金额；容差内视为一致，
 * 超出容差按多收/少收归类，便于后续索赔或补付。</p>
 */
public final class FreightDiffClassifier {

    /** 默认容差 0.01 元，覆盖分位舍入。 */
    public static final BigDecimal DEFAULT_TOLERANCE = new BigDecimal("0.01");

    public static final String MATCH = "MATCH";
    public static final String OVERCHARGE = "OVERCHARGE";
    public static final String UNDERCHARGE = "UNDERCHARGE";

    private FreightDiffClassifier() {
    }

    public static BigDecimal diff(BigDecimal billedAmount, BigDecimal expectedAmount) {
        BigDecimal billed = amount(billedAmount, "账单金额");
        BigDecimal expected = amount(expectedAmount, "应收金额");
        return billed.subtract(expected).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public static String classify(BigDecimal diffAmount) {
        return classify(diffAmount, DEFAULT_TOLERANCE);
    }

    public static String classify(BigDecimal diffAmount, BigDecimal tolerance) {
        BigDecimal diff = amount(diffAmount, "差异金额");
        BigDecimal limit = tolerance == null ? DEFAULT_TOLERANCE : tolerance.abs();
        if (diff.abs().compareTo(limit) <= 0) {
            return MATCH;
        }
        return diff.signum() > 0 ? OVERCHARGE : UNDERCHARGE;
    }

    private static BigDecimal amount(BigDecimal value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        return value;
    }
}
