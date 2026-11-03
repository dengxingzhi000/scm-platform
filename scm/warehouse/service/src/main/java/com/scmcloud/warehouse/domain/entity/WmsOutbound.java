package com.scmcloud.warehouse.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 出库单表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wms_outbound")
public class WmsOutbound implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("outbound_no")
    private String outboundNo;

    @TableField("warehouse_id")
    private String warehouseId;

    @TableField("outbound_type")
    private Integer outboundType;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_id")
    private String sourceId;

    @TableField("source_no")
    private String sourceNo;

    @TableField("priority")
    private Integer priority;

    @TableField("status")
    private Integer status;

    @TableField("total_quantity")
    private Integer totalQuantity;

    @TableField("picked_quantity")
    private Integer pickedQuantity;

    @TableField("picking_path")
    private String pickingPath;

    @TableField("total_distance")
    private Integer totalDistance;

    @TableField("expected_at")
    private LocalDateTime expectedAt;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("picker_id")
    private String pickerId;

    @TableField("picker_name")
    private String pickerName;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField("deleted")
    private Boolean deleted;

    @TableField("remark")
    private String remark;


}
