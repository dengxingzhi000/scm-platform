package com.cognora.platform.inventory.service.batch;

import com.cognora.platform.common.exception.ServiceException;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.inventory.domain.entity.InvBatch;
import com.cognora.platform.inventory.mapper.InvBatchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * FEFO 批次分配服务。
 *
 * <p>只读预览不抛业务异常，返回计划与缺口供前端展示；实际扣减在
 * {@link #allocate} 内完成，任何批次条件 UPDATE 失败都会让整单事务回滚，
 * 保证并发下不会重复分配。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FefoAllocationService {

  private final InvBatchMapper invBatchMapper;
  private final FefoAllocationPlanner planner = new FefoAllocationPlanner();

  /**
   * 只读预览：不扣减任何库存。
   */
  public FefoAllocationPlan preview(String tenantId, String skuId, String warehouseId, int quantity) {
    validate(tenantId, skuId, warehouseId, quantity);

    if (invBatchMapper.countManagedBatches(tenantId, skuId, warehouseId) == 0) {
      return FefoAllocationPlan.notBatchManaged(quantity);
    }

    List<InvBatch> candidates = invBatchMapper.selectFefoCandidates(tenantId, skuId, warehouseId);
    FefoAllocationPlan plan = planner.plan(candidates, quantity, LocalDate.now());
    if (plan.expiredSkipped()) {
      log.warn("FEFO 跳过过期批次 tenant={} sku={} warehouse={} batches={}",
          tenantId, skuId, warehouseId, plan.expiredBatchNos());
    }
    return plan;
  }

  /**
   * 按 FEFO 扣减批次可用量；无任何批次记录时返回 {@code batchManaged=false}，
   * 由调用方回退台账整批扣减。
   *
   * @throws ServiceException 合格批次不足、或批次可用量被并发事务改小
   */
  @Transactional(rollbackFor = Exception.class)
  public FefoAllocationPlan allocate(String tenantId, String skuId, String warehouseId, int quantity) {
    FefoAllocationPlan plan = preview(tenantId, skuId, warehouseId, quantity);
    if (!plan.batchManaged()) {
      return plan;
    }
    if (!plan.sufficient()) {
      throw new ServiceException(ResultCode.BAD_REQUEST.getCode(), HttpStatus.BAD_REQUEST,
          plan.message());
    }

    for (FefoAllocation allocation : plan.allocations()) {
      int updated = invBatchMapper.deductAvailable(allocation.batchId(), tenantId, allocation.quantity());
      if (updated != 1) {
        throw new ServiceException(ResultCode.CONFLICT.getCode(), HttpStatus.CONFLICT,
            "批次库存并发变更，请重试：batch=" + allocation.batchNo());
      }
    }
    return plan;
  }

  private void validate(String tenantId, String skuId, String warehouseId, int quantity) {
    if (tenantId == null || tenantId.isBlank()) {
      throw new ServiceException(ResultCode.BAD_REQUEST.getCode(), HttpStatus.BAD_REQUEST,
          "租户不能为空");
    }
    if (skuId == null || skuId.isBlank()) {
      throw new ServiceException(ResultCode.BAD_REQUEST.getCode(), HttpStatus.BAD_REQUEST,
          "SKU 不能为空");
    }
    if (warehouseId == null || warehouseId.isBlank()) {
      throw new ServiceException(ResultCode.BAD_REQUEST.getCode(), HttpStatus.BAD_REQUEST,
          "仓库不能为空");
    }
    if (quantity <= 0) {
      throw new ServiceException(ResultCode.BAD_REQUEST.getCode(), HttpStatus.BAD_REQUEST,
          "分配数量必须大于 0");
    }
  }
}
