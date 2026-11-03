package com.scmcloud.inventory.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 库存告警统计
 *
 * @author SCM Platform Team
 * @since 2026-10-29
 */
@Data
@Builder
public class InvAlertStatsResponse {

  /**
   * 告警总数
   */
  private long total;

  /**
   * 未解决告警数
   */
  private long unresolved;

  /**
   * 已解决告警数
   */
  private long resolved;

  /**
   * 未解决的库存不足告警数
   */
  private long lowStock;

  /**
   * 未解决的缺货告警数
   */
  private long outOfStock;
}
