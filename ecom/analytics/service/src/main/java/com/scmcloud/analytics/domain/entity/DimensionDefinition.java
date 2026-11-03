package com.scmcloud.analytics.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("analytics_dimension")
public class DimensionDefinition {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("dim_code")
    private String dimCode;

    @TableField("dim_name")
    private String dimName;

    @TableField("description")
    private String description;

    @TableField("dim_type")
    private String dimType;

    @TableField("data_type")
    private String dataType;

    @TableField("parent_dim_id")
    private UUID parentDimId;

    @TableField("hierarchy_level")
    private Integer hierarchyLevel;

    @TableField("is_required")
    private Boolean isRequired;

    @TableField("dict_code")
    private String dictCode;

    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private OffsetDateTime createTime;

    @TableField("create_by")
    private UUID createBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updateTime;

    @TableField("update_by")
    private UUID updateBy;

    @TableLogic
    @TableField("deleted")
    private Boolean deleted;

    @TableField("remark")
    private String remark;
}