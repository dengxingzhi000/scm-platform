package com.cognora.platform.inventory.service.batch;

import java.time.LocalDate;

/**
 * 单个批次的 FEFO 分配结果。
 *
 * @param batchId        批次主键
 * @param batchNo        批次号
 * @param expiryDate     效期，可为空（非保质期批次）
 * @param quantity       本次分配的扣减数量
 * @param totalBefore    分配前批次总量
 * @param totalAfter     分配后批次总量
 * @param availableBefore 分配前批次可用量
 * @param availableAfter  分配后批次可用量
 * @param lockedQuantity 批次锁定量（出库不改变）
 * @param damagedQuantity 批次损坏量（出库不改变）
 */
public record FefoAllocation(
    String batchId,
    String batchNo,
    LocalDate expiryDate,
    int quantity,
    int totalBefore,
    int totalAfter,
    int availableBefore,
    int availableAfter,
    int lockedQuantity,
    int damagedQuantity) {
}
