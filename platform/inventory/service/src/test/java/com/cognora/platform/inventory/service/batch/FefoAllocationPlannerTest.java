package com.cognora.platform.inventory.service.batch;

import com.cognora.platform.inventory.domain.entity.InvBatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FEFO 分配器纯函数用例：跨效期顺序、同效期按入库时间、部分分配、过期跳过、量不足。
 */
class FefoAllocationPlannerTest {

  private static final String SKU = "22222222-2222-2222-2222-222222222222";
  private static final String WH = "33333333-3333-3333-3333-333333333333";
  private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

  private final FefoAllocationPlanner planner = new FefoAllocationPlanner();

  @Test
  @DisplayName("多批次跨效期：先到期先出，耗尽前一批才进入下一批")
  void allocatesEarliestExpiryFirst() {
    InvBatch late = batch("B-LATE", 10, TODAY.plusDays(120), LocalDateTime.of(2026, 6, 1, 0, 0));
    InvBatch earliest = batch("B-EARLY", 5, TODAY.plusDays(20), LocalDateTime.of(2026, 9, 1, 0, 0));
    InvBatch middle = batch("B-MID", 10, TODAY.plusDays(60), LocalDateTime.of(2026, 7, 1, 0, 0));

    FefoAllocationPlan plan = planner.plan(List.of(late, middle, earliest), 6, TODAY);

    assertEquals(2, plan.allocations().size());
    assertEquals("B-EARLY", plan.allocations().get(0).batchNo());
    assertEquals(5, plan.allocations().get(0).quantity());
    assertEquals("B-MID", plan.allocations().get(1).batchNo());
    assertEquals(1, plan.allocations().get(1).quantity());
    assertTrue(plan.sufficient());
    assertEquals(0, plan.shortfall());
  }

  @Test
  @DisplayName("同效期按入库时间升序；入库时间相同再按主键保证稳定")
  void sameExpiryOrdersByInboundTime() {
    InvBatch later = batch("B-LATER", 3, TODAY.plusDays(30), LocalDateTime.of(2026, 10, 2, 0, 0));
    InvBatch earlier = batch("B-EARLIER", 3, TODAY.plusDays(30), LocalDateTime.of(2026, 10, 1, 0, 0));

    FefoAllocationPlan plan = planner.plan(List.of(later, earlier), 4, TODAY);

    assertEquals(List.of("B-EARLIER", "B-LATER"),
        plan.allocations().stream().map(FefoAllocation::batchNo).toList());
    assertEquals(3, plan.allocations().get(0).quantity());
    assertEquals(1, plan.allocations().get(1).quantity());
  }

  @Test
  @DisplayName("无有效期批次排在最后，不与有有效期批次抢顺序")
  void nullExpirySortedLast() {
    InvBatch withExpiry = batch("B-DATED", 5, TODAY.plusDays(90), LocalDateTime.of(2026, 5, 1, 0, 0));
    InvBatch noExpiry = batch("B-NODATE", 5, null, LocalDateTime.of(2026, 1, 1, 0, 0));

    FefoAllocationPlan plan = planner.plan(List.of(noExpiry, withExpiry), 6, TODAY);

    assertEquals("B-DATED", plan.allocations().get(0).batchNo());
    assertEquals(5, plan.allocations().get(0).quantity());
    assertEquals("B-NODATE", plan.allocations().get(1).batchNo());
    assertEquals(1, plan.allocations().get(1).quantity());
  }

  @Test
  @DisplayName("部分分配跨两个批次：批次快照逐条扣减，缺口为 0")
  void partialAllocationAcrossTwoBatches() {
    InvBatch first = batch("B-1", 5, TODAY.plusDays(10), LocalDateTime.of(2026, 10, 1, 0, 0));
    InvBatch second = batch("B-2", 5, TODAY.plusDays(20), LocalDateTime.of(2026, 10, 2, 0, 0));

    FefoAllocationPlan plan = planner.plan(List.of(second, first), 8, TODAY);

    assertEquals(2, plan.allocations().size());
    FefoAllocation a1 = plan.allocations().get(0);
    assertEquals("B-1", a1.batchNo());
    assertEquals(5, a1.quantity());
    assertEquals(5, a1.availableBefore());
    assertEquals(0, a1.availableAfter());
    assertEquals(5, a1.totalBefore());
    assertEquals(0, a1.totalAfter());

    FefoAllocation a2 = plan.allocations().get(1);
    assertEquals("B-2", a2.batchNo());
    assertEquals(3, a2.quantity());
    assertEquals(5, a2.availableBefore());
    assertEquals(2, a2.availableAfter());
    assertEquals(5, a2.totalBefore());
    assertEquals(2, a2.totalAfter());

    assertEquals(8, plan.allocated());
    assertTrue(plan.sufficient());
  }

  @Test
  @DisplayName("过期批次（效期早于今天）跳过且告警，绝不参与分配")
  void expiredBatchSkippedAndReported() {
    InvBatch expired = batch("B-EXPIRED", 10, TODAY.minusDays(1), LocalDateTime.of(2026, 1, 1, 0, 0));
    InvBatch valid = batch("B-VALID", 6, TODAY.plusDays(30), LocalDateTime.of(2026, 10, 1, 0, 0));

    FefoAllocationPlan plan = planner.plan(List.of(expired, valid), 4, TODAY);

    assertTrue(plan.expiredSkipped());
    assertEquals(List.of("B-EXPIRED"), plan.expiredBatchNos());
    assertEquals(1, plan.allocations().size());
    assertEquals("B-VALID", plan.allocations().get(0).batchNo());
    assertEquals(4, plan.allocations().get(0).quantity());
    assertTrue(plan.sufficient());
  }

  @Test
  @DisplayName("status=3 即使没有效期也按过期跳过")
  void statusExpiredWithoutDateIsSkipped() {
    InvBatch expired = batch("B-STATUS-3", 4, null, LocalDateTime.of(2026, 1, 1, 0, 0));
    expired.setStatus(3);
    InvBatch valid = batch("B-VALID", 3, TODAY.plusDays(30), LocalDateTime.of(2026, 10, 1, 0, 0));

    FefoAllocationPlan plan = planner.plan(List.of(expired, valid), 5, TODAY);

    assertTrue(plan.expiredSkipped());
    assertEquals(List.of("B-STATUS-3"), plan.expiredBatchNos());
    assertEquals(3, plan.allocated());
    assertFalse(plan.sufficient());
  }

  @Test
  @DisplayName("合格批次不足：返回缺口并标记不足，调用方据此整单拒绝")
  void insufficientValidStockReportsShortfall() {
    InvBatch expired = batch("B-EXPIRED", 5, TODAY.minusDays(2), LocalDateTime.of(2026, 1, 1, 0, 0));
    InvBatch valid = batch("B-VALID", 6, TODAY.plusDays(30), LocalDateTime.of(2026, 10, 1, 0, 0));

    FefoAllocationPlan plan = planner.plan(List.of(expired, valid), 10, TODAY);

    assertEquals(6, plan.allocated());
    assertEquals(4, plan.shortfall());
    assertFalse(plan.sufficient());
    assertTrue(plan.expiredSkipped());
    assertTrue(plan.message().contains("B-EXPIRED"));
  }

  @Test
  @DisplayName("待检/不合格/零可用批次不参与分配，也不计入过期列表")
  void ineligibleBatchesIgnored() {
    InvBatch pending = batch("B-PENDING", 5, TODAY.plusDays(5), LocalDateTime.of(2026, 10, 1, 0, 0));
    pending.setQualityStatus(2);
    InvBatch empty = batch("B-EMPTY", 0, TODAY.plusDays(5), LocalDateTime.of(2026, 10, 2, 0, 0));

    FefoAllocationPlan plan = planner.plan(List.of(pending, empty), 2, TODAY);

    assertEquals(0, plan.allocated());
    assertFalse(plan.expiredSkipped());
    assertFalse(plan.sufficient());
    assertEquals(2, plan.shortfall());
  }

  @Test
  @DisplayName("空候选列表视为批次已管理但全部不可用")
  void emptyCandidatesReportShortfall() {
    FefoAllocationPlan plan = planner.plan(List.of(), 3, TODAY);

    assertTrue(plan.batchManaged());
    assertEquals(0, plan.allocated());
    assertEquals(3, plan.shortfall());
    assertFalse(plan.sufficient());
  }

  @Test
  @DisplayName("分配数量必须大于 0")
  void zeroRequestedRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> planner.plan(List.of(batch("B-1", 5, TODAY.plusDays(1), LocalDateTime.now())), 0, TODAY));
  }

  private static InvBatch batch(String code, int available, LocalDate expiry, LocalDateTime createTime) {
    InvBatch batch = new InvBatch();
    batch.setId("id-" + code);
    batch.setBatchCode(code);
    batch.setSkuId(SKU);
    batch.setWarehouseId(WH);
    batch.setExpiryDate(expiry);
    batch.setTotalQuantity(available);
    batch.setAvailableQuantity(available);
    batch.setLockedQuantity(0);
    batch.setDamagedQuantity(0);
    batch.setQualityStatus(1);
    batch.setStatus(1);
    batch.setCreateTime(createTime);
    return batch;
  }
}
