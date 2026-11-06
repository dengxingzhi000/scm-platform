package com.cognora.platform.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预占分页查询请求。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Data
public class InvReservationQueryRequest {

  /** 业务键（订单号等），支持模糊匹配 */
  private String businessKey;

  private String skuId;

  private String warehouseId;

  /** RESERVED / CONFIRMED / RELEASED / EXPIRED */
  private String status;

  /** 创建时间范围（含） */
  private LocalDateTime startTime;

  private LocalDateTime endTime;

  @Min(value = 1, message = "页码最小为 1")
  private Integer page = 1;

  @Min(value = 1, message = "每页数量最小为 1")
  @Max(value = 500, message = "每页数量最大为 500")
  private Integer size = 20;
}
