package com.cognora.platform.inventory.service.cost;

import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 移动加权平均成本的库存侧更新。
 *
 * <p>入库通过 {@code version} 条件更新同时写数量与均价：
 * {@code UPDATE ... SET average_cost = ?, version = version + 1 WHERE version = ?}。
 * 并发入库时后到的事务 CAS 失败并重读重试，不会把均价更新覆盖丢失。
 *
 * <p>出库只按当时均价计价，不修改 {@code average_cost}。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovingAverageCostService {

  private static final int MAX_CAS_RETRIES = 3;

  private final InvInventoryMapper inventoryMapper;

  /**
   * 入库：数量与均价在同一条条件 UPDATE 内原子变更。
   *
   * @param unitPrice 入库单价；为空时按当前均价入账（均价不变）
   * @return 本次成本移动（含变更前后均价与结存数量）
   */
  @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
  public CostMovement applyInbound(String inventoryId, int quantity, BigDecimal unitPrice,
                                   int adjustType, String operatorId) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("入库数量必须大于0");
    }

    for (int attempt = 0; attempt < MAX_CAS_RETRIES; attempt++) {
      Inventory current = inventoryMapper.selectById(inventoryId);
      if (current == null) {
        throw new IllegalStateException("入库前未找到库存台账记录: id=" + inventoryId);
      }

      int beforeQuantity = nvl(current.getTotalStock());
      if (beforeQuantity < 0) {
        throw new IllegalStateException("库存台账数量为负，拒绝入库: id=" + inventoryId);
      }

      BigDecimal oldAverage = current.getAverageCost();
      BigDecimal newAverage = MovingAverageCostCalculator.inboundAverage(
          oldAverage, beforeQuantity, quantity, unitPrice);
      int expectedVersion = current.getVersion() == null ? 0 : current.getVersion();

      int updated = inventoryMapper.applyInboundWithCost(
          inventoryId, quantity, newAverage, expectedVersion, adjustType, operatorId);
      if (updated == 1) {
        return inboundMovement(oldAverage, newAverage, unitPrice, beforeQuantity, quantity);
      }

      log.warn("入库成本版本冲突，重读后台账: id={}, expectedVersion={}, attempt={}",
          inventoryId, expectedVersion, attempt + 1);
    }

    throw new IllegalStateException("库存成本并发更新冲突，请重试: id=" + inventoryId);
  }

  /**
   * 出库计价：按传入的当时均价计算金额，不改变均价。
   *
   * @param balanceBefore 变更前结存数量；为空时（Redis 路径）只记金额不记结存
   */
  public static CostMovement outboundMovement(BigDecimal averageCost, Integer balanceBefore,
                                              int quantity) {
    if (quantity < 0) {
      throw new IllegalArgumentException("出库数量不能为负数: " + quantity);
    }
    BigDecimal average = MovingAverageCostCalculator.priceForStorage(averageCost);
    return CostMovement.builder()
        .direction(CostMovement.DIRECTION_OUT)
        .quantity(quantity)
        .unitPrice(average)
        .amount(MovingAverageCostCalculator.amount(average, quantity))
        .beforeAverageCost(average)
        .afterAverageCost(average)
        .beforeBalanceQty(balanceBefore)
        .afterBalanceQty(balanceBefore == null ? null : Math.subtractExact(balanceBefore, quantity))
        .build();
  }

  private static CostMovement inboundMovement(BigDecimal oldAverage, BigDecimal newAverage,
                                              BigDecimal unitPrice, int beforeQuantity,
                                              int quantity) {
    BigDecimal oldAverageStored = MovingAverageCostCalculator.priceForStorage(oldAverage);
    BigDecimal effectivePrice = unitPrice == null
        ? oldAverageStored : MovingAverageCostCalculator.priceForStorage(unitPrice);
    return CostMovement.builder()
        .direction(CostMovement.DIRECTION_IN)
        .quantity(quantity)
        .unitPrice(effectivePrice)
        .amount(MovingAverageCostCalculator.amount(effectivePrice, quantity))
        .beforeAverageCost(oldAverageStored)
        .afterAverageCost(newAverage)
        .beforeBalanceQty(beforeQuantity)
        .afterBalanceQty(Math.addExact(beforeQuantity, quantity))
        .build();
  }

  private static int nvl(Integer value) {
    return value == null ? 0 : value;
  }
}
