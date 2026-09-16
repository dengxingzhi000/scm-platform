package com.scmcloud.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 搴撳瓨鍝嶅簲瀵硅薄
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
public class InventoryResponse {

  /**
   * 搴撳瓨 ID
   */
  private String id;

  /**
   * SKU ID
   */
  private String skuId;

  /**
   * 浠撳簱 ID
   */
  private String warehouseId;

  /**
   * 鎬诲簱锟?
   */
  private Integer totalStock;

  /**
   * 鍙敤搴撳瓨
   */
  private Integer availableStock;

  /**
   * 閿佸畾搴撳瓨锛堝凡棰勫崰锟?
   */
  private Integer lockedStock;

  /**
   * 鎹熷潖搴撳瓨
   */
  private Integer damagedStock;

  /**
   * 瀹夊叏搴撳瓨
   */
  private Integer safetyStock;

  /**
   * 鏈€澶у簱锟?
   */
  private Integer maxStock;

  /**
   * 搴撲綅缂栫爜
   */
  private String locationCode;

  /**
   * 骞冲潎鎴愭湰
   */
  private BigDecimal averageCost;

  /**
   * 搴撳瓨鐘舵€侊紙NORMAL-姝e父, LOW_STOCK-浣庡簱锟?OUT_OF_STOCK-缂鸿揣锟?
   */
  private String stockStatus;

  /**
   * 涔愯閿佺増鏈彿
   */
  private Integer version;

  /**
   * 鏈€杩戝叆搴撴椂锟?
   */
  private LocalDateTime lastInboundAt;

  /**
   * 鏈€杩戝嚭搴撴椂锟?
   */
  private LocalDateTime lastOutboundAt;

  /**
   * 鍒涘缓鏃堕棿
   */
  private LocalDateTime createTime;

  /**
   * 鏇存柊鏃堕棿
   */
  private LocalDateTime updateTime;

  /**
   * 澶囨敞
   */
  private String remark;
}