package com.cognora.platform.inventory.service.cost;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 移动加权平均成本计算。
 *
 * <p>口径：
 * <ul>
 *   <li>维度：{@code (tenant_id, sku_id, warehouse_id)}，由 {@code inv_inventory}
 *       行唯一键保证；均价存在该行的 {@code average_cost}。</li>
 *   <li>入库：{@code newAvg = (现有数量×原均价 + 入库数量×入库单价) / (现有数量+入库数量)}；
 *       现有数量为 0 时直接取入库单价。</li>
 *   <li>出库：按当时均价计价，不改变均价。</li>
 * </ul>
 *
 * <p>精度：中间计算使用 {@link MathContext#DECIMAL128}，只在落库时舍入——
 * 单价 6 位、金额 2 位，均 {@link RoundingMode#HALF_UP}。金额口径与
 * {@code com.cognora.platform.common.domain.Money} 保持一致（2 位）。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
public final class MovingAverageCostCalculator {

  /** 均价落库精度：至少 4 位，这里取 6 位。 */
  public static final int AVERAGE_SCALE = 6;

  /** 金额口径：与财务 Money 一致，2 位。 */
  public static final int AMOUNT_SCALE = 2;

  private static final MathContext CALCULATION_CONTEXT = MathContext.DECIMAL128;

  private MovingAverageCostCalculator() {
  }

  /**
   * 入库后的新均价。
   *
   * <p>入库前数量为 0 时取入库单价；入库数量为 0 且现有数量也为 0 时取入库单价，
   * 现有数量大于 0 时保持原均价（零入库不产生成本移动，也避免除零）。
   */
  public static BigDecimal inboundAverage(BigDecimal oldAverage, int oldQuantity,
                                         int inboundQuantity, BigDecimal unitPrice) {
    if (inboundQuantity < 0) {
      throw new IllegalArgumentException("入库数量不能为负数: " + inboundQuantity);
    }
    if (oldQuantity < 0) {
      throw new IllegalStateException("现有库存数量不能为负数: " + oldQuantity);
    }

    BigDecimal oldAvg = oldAverage == null ? BigDecimal.ZERO : oldAverage;
    BigDecimal price = unitPrice == null ? oldAvg : unitPrice;

    if (inboundQuantity == 0) {
      return oldQuantity == 0 ? priceForStorage(price) : priceForStorage(oldAvg);
    }
    if (oldQuantity == 0) {
      return priceForStorage(price);
    }

    int newQuantity = Math.addExact(oldQuantity, inboundQuantity);
    BigDecimal oldValue = BigDecimal.valueOf(oldQuantity).multiply(oldAvg, CALCULATION_CONTEXT);
    BigDecimal inboundValue = BigDecimal.valueOf(inboundQuantity)
        .multiply(price, CALCULATION_CONTEXT);
    BigDecimal weighted = oldValue.add(inboundValue, CALCULATION_CONTEXT)
        .divide(BigDecimal.valueOf(newQuantity), CALCULATION_CONTEXT);
    return weighted.setScale(AVERAGE_SCALE, RoundingMode.HALF_UP);
  }

  /**
   * 金额 = 单价 × 数量，落库时保留 2 位。
   */
  public static BigDecimal amount(BigDecimal unitPrice, int quantity) {
    if (quantity < 0) {
      throw new IllegalArgumentException("计价数量不能为负数: " + quantity);
    }
    BigDecimal price = unitPrice == null ? BigDecimal.ZERO : unitPrice;
    return price.multiply(BigDecimal.valueOf(quantity), CALCULATION_CONTEXT)
        .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
  }

  public static BigDecimal priceForStorage(BigDecimal price) {
    return (price == null ? BigDecimal.ZERO : price)
        .setScale(AVERAGE_SCALE, RoundingMode.HALF_UP);
  }
}
