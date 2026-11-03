package com.cognora.platform.inventory.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 库存告警扫描结果
 *
 * @author SCM Platform Team
 * @since 2026-10-29
 */
@Data
@Builder
public class InvAlertScanResult {

  /**
   * 扫描的库存记录数
   */
  private int scannedInventories;

  /**
   * 新建告警数
   */
  private int created;

  /**
   * 刷新告警数
   */
  private int updated;

  /**
   * 自动解除告警数
   */
  private int resolved;
}
