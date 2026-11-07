package com.cognora.platform.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * FEFO 分配预览请求（只读，不扣减）。
 */
@Data
public class FefoPreviewRequest {

  @NotBlank(message = "SKU ID cannot be empty")
  private String skuId;

  @NotBlank(message = "Warehouse ID cannot be empty")
  private String warehouseId;

  @NotNull(message = "Quantity cannot be empty")
  @Min(value = 1, message = "Quantity must be greater than 0")
  private Integer quantity;
}
