package com.cognora.platform.inventory.service.cost;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 一次成本移动（入库或出库）的计价结果。
 *
 * <p>数量、单价、金额均为正数，方向由 {@code direction} 表达。
 * 入库会改变均价；出库不改变均价，因此出入库的 before/after 均价相同。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Data
@Builder
public class CostMovement {

  public static final String DIRECTION_IN = "IN";

  public static final String DIRECTION_OUT = "OUT";

  private String direction;

  private int quantity;

  private BigDecimal unitPrice;

  private BigDecimal amount;

  private BigDecimal beforeAverageCost;

  private BigDecimal afterAverageCost;

  private Integer beforeBalanceQty;

  private Integer afterBalanceQty;
}
