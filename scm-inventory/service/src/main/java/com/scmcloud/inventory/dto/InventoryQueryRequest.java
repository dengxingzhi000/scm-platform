package com.scmcloud.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

/**
 * 搴撳瓨鏌ヨ璇锋眰
 *
 * <p>鏀寔澶氱鏌ヨ鏉′欢缁勫悎锟?
 * <ul>
 *   <li>SKU 鍒楄〃鏌ヨ</li>
 *   <li>浠撳簱鍒楄〃鏌ヨ</li>
 *   <li>搴撲綅缂栫爜鏌ヨ</li>
 *   <li>搴撳瓨鐘舵€佽繃婊わ紙缂鸿揣/浣庡簱锟芥甯革拷/li>
 *   <li>鍙敤搴撳瓨鑼冨洿鏌ヨ</li>
 * </ul>
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
public class InventoryQueryRequest {

  /**
   * SKU ID 鍒楄〃锛堟敮鎸佹壒閲忔煡璇級
   */
  private List<String> skuIds;

  /**
   * 浠撳簱 ID 鍒楄〃
   */
  private List<String> warehouseIds;

  /**
   * 搴撲綅缂栫爜
   */
  private String locationCode;

  /**
   * 搴撳瓨鐘舵€佽繃婊わ紙NORMAL-姝ｅ父, LOW_STOCK-浣庡簱锟?OUT_OF_STOCK-缂鸿揣锟?
   */
  private String stockStatus;

  /**
   * 鏈€灏忓彲鐢ㄥ簱瀛橈紙澶т簬绛変簬锟?
   */
  private Integer minAvailableStock;

  /**
   * 鏈€澶у彲鐢ㄥ簱瀛橈紙灏忎簬绛変簬锟?
   */
  private Integer maxAvailableStock;

  /**
   * 鏄惁浠呮煡璇㈡湁搴撳瓨鍟嗗搧
   */
  private Boolean onlyInStock;

  /**
   * 椤电爜锛堜粠1寮€濮嬶級
   */
  @Min(value = 1, message = "页码不能小于1")
  private Integer page = 1;

  /**
   * 姣忛〉鏉℃暟
   */
  @Min(value = 1, message = "每页数量不能小于1")
  @Max(value = 500, message = "每页数量不能超过500")
  private Integer size = 20;

  /**
   * 鎺掑簭瀛楁锛坅vailable_stock, total_stock, update_time锟?
   */
  private String sortBy = "update_time";

  /**
   * 鎺掑簭鏂瑰悜锛圓SC, DESC锟?
   */
  private String sortOrder = "DESC";
}