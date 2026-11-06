package com.cognora.platform.inventory.dto;

import com.cognora.platform.inventory.service.batch.FefoAllocation;
import com.cognora.platform.inventory.service.batch.FefoAllocationPlan;

import java.time.LocalDate;
import java.util.List;

/**
 * FEFO 分配预览响应。
 *
 * <p>{@code batchManaged=false} 表示该 SKU/仓库没有批次记录，实际出库会回退到
 * 台账整批扣减；{@code expiredSkipped=true} 表示分配序列中跳过了过期批次，
 * {@code message} 会给出批次号。
 */
public record FefoPreviewResponse(
    String skuId,
    String warehouseId,
    int requested,
    boolean batchManaged,
    boolean expiredSkipped,
    boolean sufficient,
    int allocated,
    int shortfall,
    String message,
    List<BatchAllocation> allocations) {

  public static FefoPreviewResponse from(FefoPreviewRequest request, FefoAllocationPlan plan) {
    List<BatchAllocation> allocations = plan.allocations().stream()
        .map(BatchAllocation::from)
        .toList();
    return new FefoPreviewResponse(
        request.getSkuId(),
        request.getWarehouseId(),
        plan.requested(),
        plan.batchManaged(),
        plan.expiredSkipped(),
        plan.sufficient(),
        plan.allocated(),
        plan.shortfall(),
        plan.message(),
        allocations);
  }

  /**
   * 单个批次的预览分配结果。
   */
  public record BatchAllocation(
      String batchId,
      String batchNo,
      LocalDate expiryDate,
      int quantity,
      int availableBefore,
      int availableAfter) {

    static BatchAllocation from(FefoAllocation allocation) {
      return new BatchAllocation(
          allocation.batchId(),
          allocation.batchNo(),
          allocation.expiryDate(),
          allocation.quantity(),
          allocation.availableBefore(),
          allocation.availableAfter());
    }
  }
}
