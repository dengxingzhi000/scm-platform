package com.cognora.platform.inventory.domain.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存批次（inv_batch）。
 *
 * <p>FEFO 出库按 {@code expiry_date} 升序分配，同效期按入库时间
 * （{@code create_time}）升序；只分配 {@code quality_status=1}（合格）且
 * {@code available_quantity>0} 的批次。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Data
@TableName("inv_batch")
public class InvBatch {

  private String id;

  private String tenantId;

  private String batchCode;

  private String skuId;

  private String warehouseId;

  private LocalDate productionDate;

  private LocalDate expiryDate;

  private Integer shelfLifeDays;

  /** 1-合格 2-待检 3-不合格 4-隔离 */
  private Integer qualityStatus;

  private Integer totalQuantity;

  private Integer availableQuantity;

  private Integer lockedQuantity;

  private Integer damagedQuantity;

  private BigDecimal unitCost;

  /** 0-已耗尽 1-正常 2-临期 3-已过期 */
  private Integer status;

  private LocalDateTime createTime;

  private LocalDateTime updateTime;

  @TableLogic
  private Boolean deleted;
}
