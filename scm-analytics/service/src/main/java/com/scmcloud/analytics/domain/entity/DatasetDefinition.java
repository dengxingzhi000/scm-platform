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
@TableName("analytics_dataset")
public class DatasetDefinition {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("dataset_code")
    private String datasetCode;

    @TableField("dataset_name")
    private String datasetName;

    @TableField("description")
    private String description;

    @TableField("dataset_type")
    private String datasetType;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_table")
    private String sourceTable;

    @TableField("source_config")
    private String sourceConfig;

    @TableField("status")
    private Integer status;

    @TableField("enabled")
    private Boolean enabled;

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