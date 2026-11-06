package com.cognora.platform.inventory.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存成本流水（inv_cost_log）。
 *
 * <p>每次出入库写一条：方向、数量、单价、金额、变更前后均价与结存数量。
 * 与库存数量变更在同一业务事务内写入。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Data
@TableName("inv_cost_log")
public class InvCostLog {

  private String id;

  private String tenantId;

  private String inventoryId;

  private String skuId;

  private String warehouseId;

  private Long legacySkuId;

  private String warehouseCode;

  /** IN-入库（重算均价）；OUT-出库（按当时均价，不改均价） */
  private String direction;

  /** 正数量，方向由 direction 表达 */
  private Integer quantity;

  private BigDecimal unitPrice;

  private BigDecimal amount;

  private BigDecimal beforeAverageCost;

  private BigDecimal afterAverageCost;

  private Integer beforeBalanceQty;

  private Integer afterBalanceQty;

  private String businessType;

  private String businessKey;

  private String requestId;

  private String referenceType;

  private String referenceId;

  private String referenceNo;

  private String counterpartKey;

  private String operatorId;

  private String operatorName;

  private String ledgerSource;

  private String idempotencyKey;

  private String remark;

  private String extraData;

  private LocalDateTime createTime;
}
