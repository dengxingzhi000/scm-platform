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
@TableName("analytics_metric")
public class MetricDefinition {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("metric_code")
    private String metricCode;

    @TableField("metric_name")
    private String metricName;

    @TableField("description")
    private String description;

    @TableField("metric_type")
    private String metricType;

    @TableField("data_type")
    private String dataType;

    @TableField("agg_func")
    private String aggFunc;

    @TableField("expr")
    private String expr;

    @TableField("unit")
    private String unit;

    @TableField("format")
    private String format;

    @TableField("visible")
    private Boolean visible;

    @TableField("status")
    private Integer status;

    @TableField("sort_order")
    private Integer sortOrder;

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