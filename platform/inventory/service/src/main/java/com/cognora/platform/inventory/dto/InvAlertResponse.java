package com.cognora.platform.inventory.dto;

import com.cognora.platform.inventory.domain.entity.InvAlert;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存告警响应
 *
 * @author SCM Platform Team
 * @since 2026-10-29
 */
@Data
public class InvAlertResponse {

  private String id;

  private String skuId;

  private String warehouseId;

  /**
   * 告警类型：1-库存不足，2-库存为0，3-即将过期，4-损坏
   */
  private Integer alertType;

  /**
   * 告警类型名称
   */
  private String alertTypeName;

  private Integer currentStock;

  private Integer safetyStock;

  private Integer thresholdValue;

  private Boolean isResolved;

  private LocalDateTime resolvedAt;

  private String resolvedBy;

  private Boolean notified;

  private LocalDateTime notifiedAt;

  private LocalDateTime createTime;

  private LocalDateTime updateTime;

  private String remark;

  public static InvAlertResponse from(InvAlert alert) {
    InvAlertResponse response = new InvAlertResponse();
    response.setId(alert.getId());
    response.setSkuId(alert.getSkuId());
    response.setWarehouseId(alert.getWarehouseId());
    response.setAlertType(alert.getAlertType());
    response.setAlertTypeName(alertTypeName(alert.getAlertType()));
    response.setCurrentStock(alert.getCurrentStock());
    response.setSafetyStock(alert.getSafetyStock());
    response.setThresholdValue(alert.getThresholdValue());
    response.setIsResolved(alert.getIsResolved());
    response.setResolvedAt(alert.getResolvedAt());
    response.setResolvedBy(alert.getResolvedBy());
    response.setNotified(alert.getNotified());
    response.setNotifiedAt(alert.getNotifiedAt());
    response.setCreateTime(alert.getCreateTime());
    response.setUpdateTime(alert.getUpdateTime());
    response.setRemark(alert.getRemark());
    return response;
  }

  private static String alertTypeName(Integer alertType) {
    if (alertType == null) {
      return "未知";
    }
    return switch (alertType) {
      case 1 -> "库存不足";
      case 2 -> "库存为0";
      case 3 -> "即将过期";
      case 4 -> "库存损坏";
      default -> "未知";
    };
  }
}
