package com.cognora.platform.inventory.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存变动流水（inv_log）。
 *
 * <p>每次数量变更写一条：变更前后 total/available/locked/damaged、变更量、
 * 业务动作、操作人、业务单据号、租户与时间。
 *
 * <p>{@code ledgerSource} 区分口径来源：{@code DB} 表示以 inv_inventory 为准；
 * {@code REDIS} 表示 Dubbo 扣减路径，数量以 Redis 为准，total/locked/damaged
 * 可能为空（未同步到台账）。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Data
@TableName("inv_log")
public class InvLog {

  private String id;

  private String tenantId;

  private String skuId;

  private String warehouseId;

  private Long legacySkuId;

  private String warehouseCode;

  /** 批次号；按批次出库时写入，整批移动/历史行为空 */
  private String batchNo;

  /** 批次效期；按批次出库时写入，整批移动/历史行为空 */
  private LocalDate expiryDate;

  /** 1-入库 2-出库 3-预占 4-释放 5-确认 6-调整 7-调拨 */
  private Integer operationType;

  private String businessType;

  /** 有符号变更量：入库/释放为正，出库/预占为负 */
  private Integer quantity;

  private Integer beforeStock;

  private Integer afterStock;

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

  private String ledgerSource;

  private String extraData;

  private LocalDateTime createTime;
}
