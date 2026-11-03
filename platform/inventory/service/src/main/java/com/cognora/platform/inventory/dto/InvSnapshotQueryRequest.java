package com.cognora.platform.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

/**
 * 库存快照分页查询请求
 *
 * @author SCM Platform Team
 * @since 2026-10-29
 */
@Data
public class InvSnapshotQueryRequest {

  /**
   * 快照日期（精确匹配）
   */
  private LocalDate snapshotDate;

  /**
   * 开始日期（包含）
   */
  private LocalDate startDate;

  /**
   * 结束日期（包含）
   */
  private LocalDate endDate;

  private String skuId;

  private String warehouseId;

  @Min(value = 1, message = "页码不能小于1")
  private Integer page = 1;

  @Min(value = 1, message = "每页数量不能小于1")
  @Max(value = 500, message = "每页数量不能超过500")
  private Integer size = 20;
}
