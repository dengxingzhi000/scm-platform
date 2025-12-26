package com.frog.inventory.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 库存预占请求
 *
 * <p>用于订单创建时预占库存，防止超卖
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
public class InventoryReservationRequest {

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
   * 预占数量
   */
  @NotNull(message = "预占数量不能为空")
  @Positive(message = "预占数量必须大于0")
  private Integer quantity;

  /**
   * 业务键（订单号等），用于幂等性
   */
  @NotBlank(message = "业务键不能为空")
  private String businessKey;

  /**
   * 预占超时时间（秒），默认15分钟
   */
  private Integer timeoutSeconds = 900;

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