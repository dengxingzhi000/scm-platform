package com.frog.inventory.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serial;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 库存告警表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("inv_alert")
@Schema(description = "库存告警表")
public class InvAlert implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("sku_id")
    private String skuId;

    @TableField("warehouse_id")
    private String warehouseId;

    @Schema(description = "告警类型:1-库存不足,2-库存为0,3-即将过期,4-损坏")
    @TableField("alert_type")
    private Integer alertType;

    @TableField("current_stock")
    private Integer currentStock;

    @TableField("safety_stock")
    private Integer safetyStock;

    @TableField("threshold_value")
    private Integer thresholdValue;

    @TableField("is_resolved")
    private Boolean isResolved;

    @TableField("resolved_at")
    private LocalDateTime resolvedAt;

    @TableField("resolved_by")
    private String resolvedBy;

    @TableField("notified")
    private Boolean notified;

    @TableField("notified_at")
    private LocalDateTime notifiedAt;

    @TableField("notify_to_user_ids")
    private String notifyToUserIds;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("remark")
    private String remark;


}
