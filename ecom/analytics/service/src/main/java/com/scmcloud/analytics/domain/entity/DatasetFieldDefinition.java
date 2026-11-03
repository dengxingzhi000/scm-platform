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
@TableName("analytics_dataset_field")
public class DatasetFieldDefinition {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("dataset_id")
    private UUID datasetId;

    @TableField("field_code")
    private String fieldCode;

    @TableField("field_name")
    private String fieldName;

    @TableField("description")
    private String description;

    @TableField("field_type")
    private String fieldType;

    @TableField("data_type")
    private String dataType;

    @TableField("metric_id")
    private UUID metricId;

    @TableField("dimension_id")
    private UUID dimensionId;

    @TableField("expr")
    private String expr;

    @TableField("is_partition_key")
    private Boolean isPartitionKey;

    @TableField("is_hidden")
    private Boolean isHidden;

    @TableField("is_required")
    private Boolean isRequired;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("default_value")
    private String defaultValue;

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