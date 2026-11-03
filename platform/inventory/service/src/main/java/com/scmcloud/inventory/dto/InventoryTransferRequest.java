package com.scmcloud.inventory.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 搴撳瓨璋冩嫧璇锋眰
 *
 * <p>鐢ㄤ簬浠撳簱闂寸殑搴撳瓨杞Щ鎿嶄綔
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
public class InventoryTransferRequest {

  /**
   * SKU ID
   */
  @NotBlank(message = "SKU ID cannot be empty")
  private String skuId;

  /**
   * Source warehouse ID
   */
  @NotBlank(message = "Source warehouse ID cannot be empty")
  private String fromWarehouseId;

  /**
   * Target warehouse ID
   */
  @NotBlank(message = "Target warehouse ID cannot be empty")
  private String toWarehouseId;

  /**
   * Transfer quantity
   */
  @NotNull(message = "Transfer quantity cannot be empty")
  @Positive(message = "Transfer quantity must be greater than 0")
  private Integer quantity;

  /**
   * 璋冩嫧鍗曞彿
   */
  private String transferNo;

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