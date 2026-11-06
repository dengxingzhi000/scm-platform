package com.cognora.platform.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存流水分页查询请求。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Data
public class InvLogQueryRequest {

  private String skuId;

  private String warehouseId;

  /** 业务单据号 */
  private String businessKey;

  /** 业务动作：ADJUST/INBOUND/OUTBOUND/TRANSFER_OUT/TRANSFER_IN/RESERVE/... */
  private String businessType;

  /** 1-入库 2-出库 3-预占 4-释放 5-确认 6-调整 7-调拨 */
  private Integer operationType;

  private LocalDateTime startTime;

  private LocalDateTime endTime;

  @Min(value = 1, message = "页码最小为 1")
  private Integer page = 1;

  @Min(value = 1, message = "每页数量最小为 1")
  @Max(value = 500, message = "每页数量最大为 500")
  private Integer size = 20;
}
