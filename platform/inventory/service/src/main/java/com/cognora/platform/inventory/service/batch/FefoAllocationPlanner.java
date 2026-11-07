package com.cognora.platform.inventory.service.batch;

import com.cognora.platform.inventory.domain.entity.InvBatch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * FEFO 分配器（纯函数，不依赖数据库）。
 *
 * <p>顺序：效期升序，空效期排最后；同效期按入库时间（create_time）升序，
 * 再按主键升序保证结果稳定。
 *
 * <p>过期语义（本批采用“告警跳过、绝不扣减”）：
 * <ul>
 *   <li>{@code status=3} 或 {@code expiry_date < 今天} 视为已过期；</li>
 *   <li>过期批次不参与分配，但会在计划中标记 {@code expiredSkipped} 并记录批次号；</li>
 *   <li>跳过过期批次后合格可用量仍不足时，整单拒绝（不做部分扣减）。</li>
 * </ul>
 *
 * <p>选择理由：过期库存应由报损/隔离流程处理，直接整单拒绝会因一条历史过期批次
 * 阻塞所有正常出库；跳过并告警既能保证绝不发出过期商品，也不影响有效批次的正常发货。
 */
public final class FefoAllocationPlanner {

  private static final Comparator<InvBatch> FEFO_ORDER =
      Comparator.comparing(InvBatch::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(InvBatch::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(InvBatch::getId, Comparator.nullsLast(Comparator.naturalOrder()));

  public FefoAllocationPlan plan(List<InvBatch> candidates, int requested, LocalDate today) {
    if (requested <= 0) {
      throw new IllegalArgumentException("FEFO 分配数量必须大于 0: " + requested);
    }

    List<InvBatch> ordered = new ArrayList<>(candidates == null ? List.of() : candidates);
    ordered.sort(FEFO_ORDER);

    int remaining = requested;
    List<FefoAllocation> allocations = new ArrayList<>();
    List<String> expiredBatchNos = new ArrayList<>();

    for (InvBatch batch : ordered) {
      if (remaining <= 0) {
        break;
      }
      if (batch.getQualityStatus() != null && batch.getQualityStatus() != 1) {
        continue;
      }
      int available = batch.getAvailableQuantity() == null ? 0 : batch.getAvailableQuantity();
      if (available <= 0) {
        continue;
      }
      if (isExpired(batch, today)) {
        expiredBatchNos.add(batch.getBatchCode());
        continue;
      }

      int take = Math.min(remaining, available);
      int total = batch.getTotalQuantity() == null ? available : batch.getTotalQuantity();
      allocations.add(new FefoAllocation(
          batch.getId(),
          batch.getBatchCode(),
          batch.getExpiryDate(),
          take,
          total,
          total - take,
          available,
          available - take,
          batch.getLockedQuantity() == null ? 0 : batch.getLockedQuantity(),
          batch.getDamagedQuantity() == null ? 0 : batch.getDamagedQuantity()));
      remaining -= take;
    }

    int allocated = requested - remaining;
    return new FefoAllocationPlan(
        true,
        !expiredBatchNos.isEmpty(),
        expiredBatchNos,
        requested,
        allocated,
        allocations,
        buildMessage(requested, allocated, expiredBatchNos));
  }

  private boolean isExpired(InvBatch batch, LocalDate today) {
    if (batch.getStatus() != null && batch.getStatus() == 3) {
      return true;
    }
    return batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(today);
  }

  private String buildMessage(int requested, int allocated, List<String> expiredBatchNos) {
    boolean expired = !expiredBatchNos.isEmpty();
    if (allocated >= requested) {
      return expired
          ? "已跳过过期批次 " + expiredBatchNos + "，其余合格批次可满足出库"
          : "FEFO 分配可满足出库";
    }
    if (expired) {
      return "已跳过过期批次 " + expiredBatchNos + "，合格批次可用量不足：需 "
          + requested + "，可用 " + allocated;
    }
    return "合格批次可用量不足：需 " + requested + "，可用 " + allocated;
  }

  /**
   * 仅用于测试/诊断：返回排序后的候选批次。
   */
  List<InvBatch> order(List<InvBatch> candidates) {
    List<InvBatch> ordered = new ArrayList<>(candidates == null ? List.of() : candidates);
    ordered.sort(FEFO_ORDER);
    return ordered;
  }
}
