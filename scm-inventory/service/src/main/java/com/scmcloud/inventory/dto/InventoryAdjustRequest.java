package com.scmcloud.inventory.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 搴撳瓨璋冩暣璇锋眰
 *
 * <p>鐢ㄤ簬鍚勭搴撳瓨鎿嶄綔锛氬叆搴撱€佸嚭搴撱€佺洏鐐硅皟鏁寸瓑
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
public class InventoryAdjustRequest {

  /**
   * SKU ID
   */
  @NotBlank(message = "SKU ID cannot be empty")
  private String skuId;

  /**
   * Warehouse ID
   */
  @NotBlank(message = "Warehouse ID cannot be empty")
  private String warehouseId;

  /**
   * Adjustment quantity (positive for increase, negative for decrease)
   */
  @NotNull(message = "Adjustment quantity cannot be empty")
  private Integer quantity;

  /**
   * Adjustment type: 1-inbound, 2-outbound, 6-inventory check, 7-transfer
   */
  @NotNull(message = "Adjustment type cannot be empty")
  private Integer adjustType;

  /**
   * 鍏宠仈涓氬姟鍗曞彿锛堥噰璐崟鍙枫€侀攢鍞崟鍙风瓑锟?
   */
  private String referenceNo;

  /**
   * 鍏宠仈涓氬姟 ID
   */
  private String referenceId;

  /**
   * 鎿嶄綔锟絀D
   */
  private String operatorId;

  /**
   * 鎿嶄綔浜哄锟?
   */
  private String operatorName;

  /**
   * 澶囨敞
   */
  private String remark;
}