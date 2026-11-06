package com.cognora.platform.inventory.dto;

import com.cognora.platform.inventory.domain.entity.InvLog;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存流水列表行：含变更前后 total/available/locked/damaged。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Data
public class InvLogResponse {

  private String id;

  private String tenantId;

  private String skuId;

  private String warehouseId;

  private Long legacySkuId;

  private String warehouseCode;

  /** 1-入库 2-出库 3-预占 4-释放 5-确认 6-调整 7-调拨 */
  private Integer operationType;

  private String businessType;

  /** 有符号变更量 */
  private Integer changeQuantity;

  private Integer beforeTotal;

  private Integer beforeAvailable;

  private Integer beforeLocked;

  private Integer beforeDamaged;

  private Integer afterTotal;

  private Integer afterAvailable;

  private Integer afterLocked;

  private Integer afterDamaged;

  private String reason;

  private String businessKey;

  private String requestId;

  private String operatorId;

  private String operatorName;

  /** DB / REDIS */
  private String ledgerSource;

  private LocalDateTime createTime;

  public static InvLogResponse from(InvLog log) {
    InvLogResponse response = new InvLogResponse();
    response.setId(log.getId());
    response.setTenantId(log.getTenantId());
    response.setSkuId(log.getSkuId());
    response.setWarehouseId(log.getWarehouseId());
    response.setLegacySkuId(log.getLegacySkuId());
    response.setWarehouseCode(log.getWarehouseCode());
    response.setOperationType(log.getOperationType());
    response.setBusinessType(log.getBusinessType());
    response.setChangeQuantity(log.getQuantity());
    response.setBeforeTotal(log.getBeforeTotal());
    response.setBeforeAvailable(log.getBeforeAvailable());
    response.setBeforeLocked(log.getBeforeLocked());
    response.setBeforeDamaged(log.getBeforeDamaged());
    response.setAfterTotal(log.getAfterTotal());
    response.setAfterAvailable(log.getAfterAvailable());
    response.setAfterLocked(log.getAfterLocked());
    response.setAfterDamaged(log.getAfterDamaged());
    response.setReason(log.getReason());
    response.setBusinessKey(log.getBusinessKey());
    response.setRequestId(log.getRequestId());
    response.setOperatorId(log.getOperatorId());
    response.setOperatorName(log.getOperatorName());
    response.setLedgerSource(log.getLedgerSource());
    response.setCreateTime(log.getCreateTime());
    return response;
  }
}
