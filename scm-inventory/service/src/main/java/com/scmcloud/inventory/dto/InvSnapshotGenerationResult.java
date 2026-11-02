package com.scmcloud.inventory.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 库存快照生成结果
 *
 * @author SCM Platform Team
 * @since 2026-10-29
 */
@Data
@Builder
public class InvSnapshotGenerationResult {

  private LocalDate snapshotDate;

  /**
   * 扫描的库存记录数
   */
  private int scannedInventories;

  /**
   * 新生成的快照数
   */
  private int created;

  /**
   * 已存在而跳过的快照数
   */
  private int skipped;

  /**
   * 生成失败的快照数
   */
  private int failed;
}
