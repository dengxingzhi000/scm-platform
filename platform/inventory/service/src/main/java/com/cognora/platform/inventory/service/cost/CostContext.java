package com.cognora.platform.inventory.service.cost;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 一条成本流水的业务上下文。
 *
 * <p>{@code businessType}/{@code businessKey}/{@code idempotencyKey} 与库存数量流水
 * （{@code inv_log}）保持同一口径，便于两边按同一业务键对账。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Data
@Builder(toBuilder = true)
public class CostContext {

  private String businessType;

  private String businessKey;

  private String requestId;

  private String referenceType;

  private String referenceId;

  private String referenceNo;

  /** 批次号；按批次出库时写入。 */
  private String batchNo;

  /** 批次效期；按批次出库时写入。 */
  private LocalDate expiryDate;

  /** 调拨对侧流水键，例如 TRANSFER_IN:TR-1 / TRANSFER_OUT:TR-1。 */
  private String counterpartKey;

  /** Redis 路径可能只有数字 SKU，UUID 台账行定位不到时使用。 */
  private Long legacySkuId;

  /** Redis 路径的仓库编码（非 UUID）。 */
  private String warehouseCode;

  private String operatorId;

  private String operatorName;

  private String remark;

  /** DB / REDIS，默认 DB。 */
  private String ledgerSource;

  private String idempotencyKey;

  private String extraData;
}
