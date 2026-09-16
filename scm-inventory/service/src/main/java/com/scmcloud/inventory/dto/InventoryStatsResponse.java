package com.scmcloud.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 搴撳瓨缁熻鍝嶅簲
 *
 * <p>鎻愪緵鍏ㄥ眬搴撳瓨缁熻淇℃伅
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
public class InventoryStatsResponse {

  /**
   * SKU 鎬绘暟
   */
  private Long totalSkuCount;

  /**
   * 浠撳簱鎬绘暟
   */
  private Long totalWarehouseCount;

  /**
   * 鎬诲簱瀛樻暟锟?
   */
  private Long totalStockQuantity;

  /**
   * 鍙敤搴撳瓨鏁伴噺
   */
  private Long availableStockQuantity;

  /**
   * 閿佸畾搴撳瓨鏁伴噺
   */
  private Long lockedStockQuantity;

  /**
   * 鎹熷潖搴撳瓨鏁伴噺
   */
  private Long damagedStockQuantity;

  /**
   * 搴撳瓨鎬讳环锟?
   */
  private BigDecimal totalStockValue;

  /**
   * 缂鸿揣 SKU 鏁伴噺
   */
  private Long outOfStockCount;

  /**
   * 浣庡簱锟絊KU 鏁伴噺
   */
  private Long lowStockCount;

  /**
   * 姝e父搴撳瓨 SKU 鏁伴噺
   */
  private Long normalStockCount;
}