package com.cognora.platform.inventory.service.batch;

import java.util.List;

/**
 * FEFO 分配计划。
 *
 * <p>{@code batchManaged=false} 表示该 SKU/仓库没有任何批次记录，调用方沿用
 * 台账整批扣减；只要存在批次记录，就必须按批次足额分配，否则整单拒绝。
 */
public record FefoAllocationPlan(
    boolean batchManaged,
    boolean expiredSkipped,
    List<String> expiredBatchNos,
    int requested,
    int allocated,
    List<FefoAllocation> allocations,
    String message) {

  public FefoAllocationPlan {
    expiredBatchNos = expiredBatchNos == null ? List.of() : List.copyOf(expiredBatchNos);
    allocations = allocations == null ? List.of() : List.copyOf(allocations);
  }

  public int shortfall() {
    return Math.max(0, requested - allocated);
  }

  public boolean sufficient() {
    return batchManaged && allocated >= requested;
  }

  public static FefoAllocationPlan notBatchManaged(int requested) {
    return new FefoAllocationPlan(false, false, List.of(), requested, 0, List.of(),
        "无批次记录，回退台账扣减");
  }
}
