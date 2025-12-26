package com.frog.inventory.domain.entity;

import java.io.Serial;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 库存快照表（每日快照）
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("inv_snapshot")
@Schema(description = "库存快照表（每日快照）")
public class InvSnapshot implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("snapshot_date")
    private LocalDate snapshotDate;

    @TableField("sku_id")
    private String skuId;

    @TableField("warehouse_id")
    private String warehouseId;

    @TableField("total_stock")
    private Integer totalStock;

    @TableField("available_stock")
    private Integer availableStock;

    @TableField("locked_stock")
    private Integer lockedStock;

    @TableField("damaged_stock")
    private Integer damagedStock;

    @TableField("average_cost")
    private BigDecimal averageCost;

    @TableField("total_value")
    private BigDecimal totalValue;

    @TableField("create_time")
    private LocalDateTime createTime;

}
