package com.cognora.platform.inventory.dto;

import com.cognora.platform.inventory.domain.entity.InvReservationRecord;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预占列表行。
 *
 * <p>关联单据只返回库存域内可确认的信息（单据号 + 类型）；跨库存域的订单明细
 * 需要订单服务补充，不在本接口内跨库 join。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Data
public class InvReservationResponse {

  private String id;

  private String tenantId;

  private String businessKey;

  private String skuId;

  private String warehouseId;

  private String inventoryId;

  private Integer quantity;

  /** RESERVED / CONFIRMED / RELEASED / EXPIRED */
  private String status;

  private String statusName;

  /** 关联单据类型，当前固定 ORDER */
  private String documentType;

  private String documentNo;

  private LocalDateTime expireAt;

  private LocalDateTime reservedAt;

  private LocalDateTime confirmedAt;

  private LocalDateTime releasedAt;

  private LocalDateTime createTime;

  private LocalDateTime updateTime;

  public static InvReservationResponse from(InvReservationRecord record) {
    InvReservationResponse response = new InvReservationResponse();
    response.setId(record.getId());
    response.setTenantId(record.getTenantId());
    response.setBusinessKey(record.getBusinessKey());
    response.setSkuId(record.getSkuId());
    response.setWarehouseId(record.getWarehouseId());
    response.setInventoryId(record.getInventoryId());
    response.setQuantity(record.getQuantity());
    response.setStatus(record.getStatus());
    response.setStatusName(statusName(record.getStatus()));
    response.setDocumentType("ORDER");
    response.setDocumentNo(record.getBusinessKey());
    response.setExpireAt(record.getExpireAt());
    response.setReservedAt(record.getReservedAt());
    response.setConfirmedAt(record.getConfirmedAt());
    response.setReleasedAt(record.getReleasedAt());
    response.setCreateTime(record.getCreateTime());
    response.setUpdateTime(record.getUpdateTime());
    return response;
  }

  private static String statusName(String status) {
    if (status == null) {
      return "未知";
    }
    return switch (status) {
      case "RESERVED" -> "已预占";
      case "CONFIRMED" -> "已确认";
      case "RELEASED" -> "已释放";
      case "EXPIRED" -> "已过期";
      default -> "未知";
    };
  }
}
