package com.cognora.platform.inventory.service.cost;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 移动加权平均成本计算口径用例。
 */
class MovingAverageCostCalculatorTest {

  @Test
  @DisplayName("两次不同单价入库后按加权平均重算")
  void twoDifferentInboundPricesUseWeightedAverage() {
    BigDecimal first = MovingAverageCostCalculator.inboundAverage(
        BigDecimal.ZERO, 0, 10, new BigDecimal("10.00"));
    assertEquals(new BigDecimal("10.000000"), first);

    BigDecimal second = MovingAverageCostCalculator.inboundAverage(
        first, 10, 10, new BigDecimal("20.00"));
    assertEquals(new BigDecimal("15.000000"), second);

    BigDecimal third = MovingAverageCostCalculator.inboundAverage(
        new BigDecimal("9.90"), 3, 7, new BigDecimal("12.35"));
    // (3*9.90 + 7*12.35) / 10 = 11.615
    assertEquals(new BigDecimal("11.615000"), third);
  }

  @Test
  @DisplayName("首次入库（现有数量为 0）直接取入库单价")
  void firstInboundIntoEmptyStockTakesUnitPrice() {
    assertEquals(new BigDecimal("8.500000"), MovingAverageCostCalculator.inboundAverage(
        null, 0, 10, new BigDecimal("8.5")));
    assertEquals(new BigDecimal("8.500000"), MovingAverageCostCalculator.inboundAverage(
        BigDecimal.ZERO, 0, 10, new BigDecimal("8.5")));
  }

  @Test
  @DisplayName("入库数量为 0：库存也为 0 时初始化单价，库存大于 0 时均价不变")
  void zeroInboundQuantityBoundary() {
    // 零数量首次入库：没有数量可加权，按入库单价初始化
    assertEquals(new BigDecimal("3.333333"), MovingAverageCostCalculator.inboundAverage(
        null, 0, 0, new BigDecimal("3.3333334")));

    // 已有库存的零数量入库：不产生成本移动，保持原均价，避免除零
    assertEquals(new BigDecimal("5.500000"), MovingAverageCostCalculator.inboundAverage(
        new BigDecimal("5.5"), 10, 0, new BigDecimal("99.00")));
  }

  @Test
  @DisplayName("出库按当时均价计价且不改变均价")
  void outboundDoesNotChangeAverage() {
    CostMovement movement = MovingAverageCostService.outboundMovement(
        new BigDecimal("15.000000"), 20, 5);

    assertEquals("OUT", movement.getDirection());
    assertEquals(5, movement.getQuantity());
    assertEquals(new BigDecimal("15.000000"), movement.getUnitPrice());
    assertEquals(new BigDecimal("75.00"), movement.getAmount());
    assertEquals(new BigDecimal("15.000000"), movement.getBeforeAverageCost());
    assertEquals(new BigDecimal("15.000000"), movement.getAfterAverageCost());
    assertEquals(20, movement.getBeforeBalanceQty());
    assertEquals(15, movement.getAfterBalanceQty());
  }

  @Test
  @DisplayName("连续出入库后结存金额始终等于均价×结存数量")
  void continuousInboundAndOutboundKeepsBalanceConsistent() {
    BigDecimal average = BigDecimal.ZERO;
    int balance = 0;

    average = MovingAverageCostCalculator.inboundAverage(average, balance, 10, new BigDecimal("10"));
    balance += 10;
    assertBalanceAmount(average, balance, "100.00");

    average = MovingAverageCostCalculator.inboundAverage(average, balance, 10, new BigDecimal("20"));
    balance += 10;
    assertBalanceAmount(average, balance, "300.00");

    CostMovement out1 = MovingAverageCostService.outboundMovement(average, balance, 5);
    balance -= 5;
    assertEquals(new BigDecimal("15.000000"), average);
    assertEquals(new BigDecimal("75.00"), out1.getAmount());
    assertBalanceAmount(average, balance, "225.00");

    average = MovingAverageCostCalculator.inboundAverage(average, balance, 5, new BigDecimal("30"));
    balance += 5;
    assertBalanceAmount(average, balance, "375.00");

    CostMovement out2 = MovingAverageCostService.outboundMovement(average, balance, 8);
    balance -= 8;
    assertEquals(new BigDecimal("18.750000"), average);
    assertEquals(new BigDecimal("150.00"), out2.getAmount());
    assertBalanceAmount(average, balance, "225.00");
  }

  @Test
  @DisplayName("金额落库 2 位 HALF_UP，舍入边界不提前发生")
  void amountRoundsHalfUpOnlyOnPersist() {
    assertEquals(new BigDecimal("0.01"), MovingAverageCostCalculator.amount(
        new BigDecimal("0.005"), 1));
    assertEquals(new BigDecimal("0.00"), MovingAverageCostCalculator.amount(
        new BigDecimal("0.0049"), 1));
    // 2.345 * 3 = 7.035 -> 7.04
    assertEquals(new BigDecimal("7.04"), MovingAverageCostCalculator.amount(
        new BigDecimal("2.345"), 3));
    assertEquals(new BigDecimal("0.10"), MovingAverageCostCalculator.amount(
        new BigDecimal("0.10"), 1));
  }

  @Test
  @DisplayName("均价中间高精度、落库才舍入到 6 位")
  void averageRoundsOnlyOnStorage() {
    // (3*1.000000 + 3*1.000001) / 6 = 1.0000005 -> 6 位 HALF_UP = 1.000001
    assertEquals(new BigDecimal("1.000001"), MovingAverageCostCalculator.inboundAverage(
        new BigDecimal("1.000000"), 3, 3, new BigDecimal("1.000001")));

    // (2*10 + 1*20) / 3 = 13.33333333... 只在最后舍入到 6 位
    assertEquals(new BigDecimal("13.333333"), MovingAverageCostCalculator.inboundAverage(
        new BigDecimal("10.000000"), 2, 1, new BigDecimal("20")));
  }

  @Test
  @DisplayName("负数数量直接拒绝，避免反向记账")
  void negativeQuantityIsRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> MovingAverageCostCalculator.inboundAverage(BigDecimal.ONE, 1, -1, BigDecimal.TEN));
    assertThrows(IllegalArgumentException.class,
        () -> MovingAverageCostCalculator.amount(BigDecimal.ONE, -1));
  }

  private static void assertBalanceAmount(BigDecimal average, int balance, String expected) {
    BigDecimal value = average.multiply(BigDecimal.valueOf(balance))
        .setScale(MovingAverageCostCalculator.AMOUNT_SCALE, RoundingMode.HALF_UP);
    assertEquals(new BigDecimal(expected), value);
  }
}
