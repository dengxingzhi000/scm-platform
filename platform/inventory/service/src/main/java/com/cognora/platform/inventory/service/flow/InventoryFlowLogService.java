package com.cognora.platform.inventory.service.flow;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.domain.entity.InvLog;
import com.cognora.platform.inventory.mapper.InvLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 库存流水写入服务。
 *
 * <p>与调用方的业务事务同事务（{@link Propagation#REQUIRED}）：业务更新失败会连同
 * 流水与去重键一起回滚，不会出现“有流水没业务”。
 *
 * <p>去重：{@code idempotencyKey} 非空时先占 {@code inv_log_idempotency} 的
 * {@code (tenant_id, idempotency_key)}，已被占用说明该业务动作已经记过流水，直接返回。
 *
 * <p>Redis（Dubbo）路径不在 DB 事务内，调用方需自行补偿；本服务不跨账本对账。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryFlowLogService {

  private final InvLogMapper invLogMapper;

  /**
   * 写一条库存流水。
   *
   * @return true-实际写入；false-重复幂等键，未重复写入
   */
  @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
  public boolean record(FlowLogEntry entry) {
    if (entry == null) {
      throw new IllegalArgumentException("流水内容不能为空");
    }
    if (entry.getOperationType() == null) {
      throw new IllegalArgumentException("流水 operationType 不能为空");
    }

    String tenantId = resolveTenant(entry.getTenantId());
    if (tenantId == null) {
      log.warn("缺少租户上下文，跳过库存流水: businessType={}, businessKey={}",
          entry.getBusinessType(), entry.getBusinessKey());
      return false;
    }

    String logId = UUID.randomUUID().toString();
    if (!isBlank(entry.getIdempotencyKey())) {
      int claimed = invLogMapper.claimIdempotency(
          logId, tenantId, entry.getIdempotencyKey(), logId);
      if (claimed == 0) {
        log.debug("流水幂等键已存在，跳过重复写入: key={}", entry.getIdempotencyKey());
        return false;
      }
    }

    invLogMapper.insertLog(toEntity(logId, tenantId, entry));
    return true;
  }

  private static String resolveTenant(String tenantId) {
    if (!isBlank(tenantId)) {
      return tenantId;
    }
    UUID current = TenantContextHolder.getTenantId();
    return current == null ? null : current.toString();
  }

  private static InvLog toEntity(String logId, String tenantId, FlowLogEntry entry) {
    InvLog log = new InvLog();
    log.setId(logId);
    log.setTenantId(tenantId);
    log.setSkuId(entry.getSkuId());
    log.setWarehouseId(entry.getWarehouseId());
    log.setLegacySkuId(entry.getLegacySkuId());
    log.setWarehouseCode(entry.getWarehouseCode());
    log.setOperationType(entry.getOperationType());
    log.setBusinessType(entry.getBusinessType());
    log.setQuantity(entry.getChangeQuantity() == null ? 0 : entry.getChangeQuantity());
    log.setBeforeStock(entry.getBeforeAvailable());
    log.setAfterStock(entry.getAfterAvailable());
    log.setBeforeTotal(entry.getBeforeTotal());
    log.setBeforeAvailable(entry.getBeforeAvailable());
    log.setBeforeLocked(entry.getBeforeLocked());
    log.setBeforeDamaged(entry.getBeforeDamaged());
    log.setAfterTotal(entry.getAfterTotal());
    log.setAfterAvailable(entry.getAfterAvailable());
    log.setAfterLocked(entry.getAfterLocked());
    log.setAfterDamaged(entry.getAfterDamaged());
    log.setReason(entry.getReason());
    log.setBusinessKey(entry.getBusinessKey());
    log.setRequestId(entry.getRequestId());
    log.setReferenceType(entry.getReferenceType());
    log.setReferenceId(entry.getReferenceId());
    log.setReferenceNo(entry.getReferenceNo());
    log.setOperatorId(entry.getOperatorId());
    log.setOperatorName(entry.getOperatorName());
    log.setLedgerSource(isBlank(entry.getLedgerSource()) ? "DB" : entry.getLedgerSource());
    log.setExtraData(isBlank(entry.getExtraData()) ? "{}" : entry.getExtraData());
    log.setCreateTime(LocalDateTime.now());
    return log;
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
