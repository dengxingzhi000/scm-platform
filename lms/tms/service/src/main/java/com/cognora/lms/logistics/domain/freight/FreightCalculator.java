package com.cognora.lms.logistics.domain.freight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 运费计算引擎（批次 2）。
 *
 * <p>计费口径：</p>
 * <ol>
 *   <li>体积重 = 体积(cm³) / 体积除数（默认 6000），保留 6 位中间精度；</li>
 *   <li>计费重 = max(实重, 体积重)，按续重单位向上进位（CEILING）；</li>
 *   <li>运费 = 首重费 + 续重单位数 × 续重单价 + 区域附加费；</li>
 *   <li>总额不低于最低收费，金额按 HALF_UP 保留 2 位小数。</li>
 * </ol>
 *
 * <p>纯函数实现，不依赖数据库，便于单测覆盖边界与对账复用同一口径。</p>
 */
public final class FreightCalculator {

    private static final int MONEY_SCALE = 2;
    private static final int WEIGHT_SCALE = 3;
    private static final int INTERMEDIATE_SCALE = 6;

    private FreightCalculator() {
    }

    public static FreightQuote calculate(FreightRule rule, BigDecimal actualWeight, BigDecimal volume) {
        Objects.requireNonNull(rule, "freight rule is required");
        BigDecimal firstWeightKg = positive(rule.getFirstWeightKg(), "首重重量");
        BigDecimal unitKg = positive(rule.getAdditionalUnitKg(), "续重单位");
        BigDecimal divisor = positive(rule.getVolumeDivisor(), "体积重除数");

        BigDecimal weight = nonNegative(actualWeight, "实重");
        BigDecimal cube = nonNegative(volume, "体积");

        BigDecimal volumetric = cube.divide(divisor, INTERMEDIATE_SCALE, RoundingMode.HALF_UP)
                .setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
        BigDecimal chargeable = weight.max(volumetric).setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);

        // 计费重按续重单位向上进位：不足一个单位按一个单位计。
        BigDecimal billed = chargeable.divide(unitKg, 0, RoundingMode.CEILING)
                .multiply(unitKg)
                .setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);

        BigDecimal firstFee = money(rule.getFirstWeightFee());
        BigDecimal additionalFee = BigDecimal.ZERO.setScale(MONEY_SCALE);
        if (billed.compareTo(firstWeightKg) > 0) {
            BigDecimal units = billed.subtract(firstWeightKg).divide(unitKg, 0, RoundingMode.CEILING);
            additionalFee = units.multiply(money(rule.getAdditionalUnitFee())).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal surcharge = money(rule.getRegionSurcharge());
        BigDecimal subtotal = firstFee.add(additionalFee).add(surcharge).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal minCharge = money(rule.getMinCharge());
        BigDecimal total = subtotal.max(minCharge).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return new FreightQuote(weight, volumetric, chargeable, billed,
                firstFee, additionalFee, surcharge, total);
    }

    private static BigDecimal positive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(field + "必须大于 0");
        }
        return value;
    }

    private static BigDecimal nonNegative(BigDecimal value, String field) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(WEIGHT_SCALE);
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException(field + "不能为负数");
        }
        return value;
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE);
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("费用金额不能为负数");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
