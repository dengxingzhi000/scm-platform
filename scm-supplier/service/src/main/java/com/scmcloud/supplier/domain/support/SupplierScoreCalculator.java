package com.scmcloud.supplier.domain.support;

import com.scmcloud.supplier.domain.entity.SupSupplierEvaluation;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 供应商评分口径：质量 30%、交付 30%、服务 20%、价格 20%。
 *
 * <p>只对已填写的维度按权重归一化，避免缺失维度被当作 0 分拉低总分。</p>
 */
@Slf4j
public final class SupplierScoreCalculator {

    public static final BigDecimal WEIGHT_QUALITY = new BigDecimal("0.30");
    public static final BigDecimal WEIGHT_DELIVERY = new BigDecimal("0.30");
    public static final BigDecimal WEIGHT_SERVICE = new BigDecimal("0.20");
    public static final BigDecimal WEIGHT_PRICE = new BigDecimal("0.20");

    public static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    public static final BigDecimal MAX_SCORE = new BigDecimal("100");

    /** 供应商分级阈值：战略 ≥90，优选 ≥80，合格 ≥70，其余备选。 */
    public static final BigDecimal LEVEL_STRATEGIC = new BigDecimal("90");
    public static final BigDecimal LEVEL_PREFERRED = new BigDecimal("80");
    public static final BigDecimal LEVEL_QUALIFIED = new BigDecimal("70");

    private SupplierScoreCalculator() {
    }

    public static BigDecimal weightedTotal(SupSupplierEvaluation evaluation) {
        if (evaluation == null) {
            return BigDecimal.ZERO;
        }
        return weightedTotal(
                evaluation.getQualityScore(),
                evaluation.getDeliveryScore(),
                evaluation.getServiceScore(),
                evaluation.getPriceScore());
    }

    /**
     * 按权重计算 0-100 的综合得分；未填写的维度不参与归一化。
     */
    public static BigDecimal weightedTotal(BigDecimal qualityScore,
                                           BigDecimal deliveryScore,
                                           BigDecimal serviceScore,
                                           BigDecimal priceScore) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal weightSum = BigDecimal.ZERO;

        if (qualityScore != null) {
            weightedSum = weightedSum.add(qualityScore.multiply(WEIGHT_QUALITY));
            weightSum = weightSum.add(WEIGHT_QUALITY);
        }
        if (deliveryScore != null) {
            weightedSum = weightedSum.add(deliveryScore.multiply(WEIGHT_DELIVERY));
            weightSum = weightSum.add(WEIGHT_DELIVERY);
        }
        if (serviceScore != null) {
            weightedSum = weightedSum.add(serviceScore.multiply(WEIGHT_SERVICE));
            weightSum = weightSum.add(WEIGHT_SERVICE);
        }
        if (priceScore != null) {
            weightedSum = weightedSum.add(priceScore.multiply(WEIGHT_PRICE));
            weightSum = weightSum.add(WEIGHT_PRICE);
        }

        if (weightSum.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return clamp(weightedSum.divide(weightSum, 2, RoundingMode.HALF_UP));
    }

    /**
     * 根据综合得分推导供应商分级（1-战略,2-优选,3-合格,4-备选）。
     */
    public static int levelFor(BigDecimal overallScore) {
        if (overallScore == null) {
            return 4;
        }
        if (overallScore.compareTo(LEVEL_STRATEGIC) >= 0) {
            return 1;
        }
        if (overallScore.compareTo(LEVEL_PREFERRED) >= 0) {
            return 2;
        }
        if (overallScore.compareTo(LEVEL_QUALIFIED) >= 0) {
            return 3;
        }
        return 4;
    }

    /**
     * 计算维度均值，无有效值时返回 null。
     */
    public static BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (BigDecimal value : values) {
            if (value != null) {
                sum = sum.add(value);
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal clamp(BigDecimal score) {
        if (score == null) {
            return null;
        }
        BigDecimal bounded = score;
        if (bounded.compareTo(MIN_SCORE) < 0) {
            bounded = MIN_SCORE;
        }
        if (bounded.compareTo(MAX_SCORE) > 0) {
            bounded = MAX_SCORE;
        }
        return bounded.setScale(2, RoundingMode.HALF_UP);
    }
}
