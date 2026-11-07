package com.cognora.platform.inventory.service.cost;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.domain.entity.InvCostLog;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.mapper.InvCostLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 库存成本流水写入服务。
 *
 * <p>{@link Propagation#REQUIRED}：与库存数量变更同事务，业务失败时成本流水一起回滚。
 * 幂等键与 {@code inv_log} 使用同一业务键；成本流水表上的
 * {@code (tenant_id, idempotency_key)} 唯一索引负责去重。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryCostLogService {

  private final InvCostLogMapper costLogMapper;

  /**
   * 写一条成本流水。
   *
   * @return true-实际写入；false-重复幂等键或缺少租户
   */
  @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
  public boolean record(Inventory inventory, CostMovement movement, CostContext context,
                        String tenantId) {
    if (movement == null) {
      throw new IllegalArgumentException("成本移动内容不能为空");
    }
    if (context == null) {
      throw new IllegalArgumentException("成本流水上下文不能为空");
    }

    String tenant = resolveTenant(tenantId);
    if (tenant == null) {
      log.warn("缺少租户上下文，跳过成本流水: businessType={}, businessKey={}",
          safeForLog(context.getBusinessType()), safeForLog(context.getBusinessKey()));
      return false;
    }

    InvCostLog entity = toEntity(UUID.randomUUID().toString(), tenant, inventory, movement, context);
    int inserted = costLogMapper.insertLog(entity);
    return inserted > 0;
  }

  private static InvCostLog toEntity(String logId, String tenant, Inventory inventory,
                                     CostMovement movement, CostContext context) {
    InvCostLog log = new InvCostLog();
    log.setId(logId);
    log.setTenantId(tenant);
    if (inventory != null) {
      log.setInventoryId(inventory.getId());
      log.setSkuId(inventory.getSkuId());
      log.setWarehouseId(inventory.getWarehouseId());
    }
    log.setLegacySkuId(context.getLegacySkuId());
    log.setWarehouseCode(context.getWarehouseCode());
    log.setBatchNo(context.getBatchNo());
    log.setExpiryDate(context.getExpiryDate());
    log.setDirection(movement.getDirection());
    log.setQuantity(movement.getQuantity());
    log.setUnitPrice(MovingAverageCostCalculator.priceForStorage(movement.getUnitPrice()));
    log.setAmount((movement.getAmount() == null ? BigDecimal.ZERO : movement.getAmount())
        .setScale(MovingAverageCostCalculator.AMOUNT_SCALE, java.math.RoundingMode.HALF_UP));
    log.setBeforeAverageCost(scaleAverage(movement.getBeforeAverageCost()));
    log.setAfterAverageCost(scaleAverage(movement.getAfterAverageCost()));
    log.setBeforeBalanceQty(movement.getBeforeBalanceQty());
    log.setAfterBalanceQty(movement.getAfterBalanceQty());
    log.setBusinessType(context.getBusinessType());
    log.setBusinessKey(context.getBusinessKey());
    log.setRequestId(context.getRequestId());
    log.setReferenceType(context.getReferenceType());
    log.setReferenceId(context.getReferenceId());
    log.setReferenceNo(context.getReferenceNo());
    log.setCounterpartKey(context.getCounterpartKey());
    log.setOperatorId(context.getOperatorId());
    log.setOperatorName(context.getOperatorName());
    log.setLedgerSource(isBlank(context.getLedgerSource()) ? "DB" : context.getLedgerSource());
    log.setIdempotencyKey(context.getIdempotencyKey());
    log.setRemark(context.getRemark());
    log.setExtraData(isBlank(context.getExtraData()) ? "{}" : context.getExtraData());
    log.setCreateTime(LocalDateTime.now());
    return log;
  }

  private static BigDecimal scaleAverage(BigDecimal value) {
    return value == null ? null : MovingAverageCostCalculator.priceForStorage(value);
  }

  private static String resolveTenant(String tenantId) {
    if (!isBlank(tenantId)) {
      return tenantId;
    }
    UUID current = TenantContextHolder.getTenantId();
    return current == null ? null : current.toString();
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  /** 日志脱敏：去掉用户可控字段中的换行，避免日志伪造。 */
  private static String safeForLog(String value) {
    if (value == null) {
      return "";
    }
    return value.replace('\r', '_').replace('\n', '_');
  }
}
