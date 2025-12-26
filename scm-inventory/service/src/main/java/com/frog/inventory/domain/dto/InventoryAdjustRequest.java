package com.frog.inventory.domain.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 库存调整请求
 *
 * <p>用于各种库存操作：入库、出库、盘点调整等
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
public class InventoryAdjustRequest {

  /**
   * SKU ID
   */
  @NotBlank(message = "SKU ID 不能为空")
  private String skuId;

  /**
   * 仓库 ID
   */
  @NotBlank(message = "仓库 ID 不能为空")
  private String warehouseId;

  /**
   * 调整数量（正数为增加，负数为减少）
   */
  @NotNull(message = "调整数量不能为空")
  private Integer quantity;

  /**
   * 调整类型（1-入库, 2-出库, 6-盘点调整, 7-调拨）
   */
  @NotNull(message = "调整类型不能为空")
  private Integer adjustType;

  /**
   * 关联业务单号（采购单号、销售单号等）
   */
  private String referenceNo;

  /**
   * 关联业务 ID
   */
  private String referenceId;

  /**
   * 操作人 ID
   */
  private String operatorId;

  /**
   * 操作人姓名
   */
  private String operatorName;

  /**
   * 备注
   */
  private String remark;
}