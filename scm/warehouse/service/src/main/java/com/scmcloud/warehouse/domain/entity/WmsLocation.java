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
 * 库位表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wms_location")
public class WmsLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("warehouse_id")
    private String warehouseId;

    @TableField("location_code")
    private String locationCode;

    @TableField("zone")
    private String zone;

    @TableField("shelf")
    private String shelf;

    @TableField("layer")
    private String layer;

    @TableField("position")
    private String position;

    @TableField("location_type")
    private Integer locationType;

    @TableField("max_capacity")
    private Integer maxCapacity;

    @TableField("current_capacity")
    private Integer currentCapacity;

    @TableField("status")
    private Integer status;

    @TableField("enabled")
    private Boolean enabled;

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
