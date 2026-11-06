package com.cognora.platform.inventory.service.flow;

import lombok.Builder;
import lombok.Data;

/**
 * 一条库存流水的入参。
 *
 * <p>调用方负责填变更前后快照；{@code idempotencyKey} 非空时由
 * {@link InventoryFlowLogService} 做跨租户去重。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Data
@Builder
public class FlowLogEntry {

  private String tenantId;

  private String skuId;

  private String warehouseId;

  private Long legacySkuId;

  private String warehouseCode;

  /** 1-入库 2-出库 3-预占 4-释放 5-确认 6-调整 7-调拨 */
  private Integer operationType;

  private String businessType;

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

  private String referenceType;

  private String referenceId;

  private String referenceNo;

  private String operatorId;

  private String operatorName;

  /** DB / REDIS，默认 DB */
  private String ledgerSource;

  private String extraData;

  private String idempotencyKey;
}
