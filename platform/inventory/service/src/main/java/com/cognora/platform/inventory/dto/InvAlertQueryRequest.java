package com.cognora.platform.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 库存告警分页查询请求
 *
 * <p>支持按 SKU、仓库、告警类型和解决状态过滤。
 *
 * @author SCM Platform Team
 * @since 2026-10-29
 */
@Data
public class InvAlertQueryRequest {

  /**
   * SKU ID
   */
  private String skuId;

  /**
   * 仓库 ID
   */
  private String warehouseId;

  /**
   * 告警类型：1-库存不足，2-库存为0，3-即将过期，4-损坏
   */
  private Integer alertType;

  /**
   * 是否已解决
   */
  private Boolean isResolved;

  /**
   * 页码（从1开始）
   */
  @Min(value = 1, message = "页码不能小于1")
  private Integer page = 1;

  /**
   * 每页条数
   */
  @Min(value = 1, message = "每页数量不能小于1")
  @Max(value = 500, message = "每页数量不能超过500")
  private Integer size = 20;
}
