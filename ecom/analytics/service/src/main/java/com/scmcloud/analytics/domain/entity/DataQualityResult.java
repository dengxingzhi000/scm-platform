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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("analytics_quality_result")
public class DataQualityResult {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("rule_id")
    private UUID ruleId;

    @TableField("rule_code")
    private String ruleCode;

    @TableField("dataset_code")
    private String datasetCode;

    @TableField("check_date")
    private LocalDate checkDate;

    @TableField("passed")
    private Boolean passed;

    @TableField("metric_value")
    private BigDecimal metricValue;

    @TableField("message")
    private String message;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private OffsetDateTime createTime;

    @TableLogic
    @TableField("deleted")
    private Boolean deleted;
}
