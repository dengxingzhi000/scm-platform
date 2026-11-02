package com.scmcloud.audit.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.scmcloud.common.domain.TenantId;

import java.io.Serial;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 鏁忔劅鎿嶄綔鏃ュ織锟?
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_sensitive_operation_log")
public class SysSensitiveOperationLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    /** Filled from the tenant context by {@code AuditMetaObjectHandler}. */
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private TenantId tenantId;

    @TableField("user_id")
    private String userId;

    @TableField("username")
    private String username;

    @TableField("operation_type")
    private String operationType;

    @TableField("operation_module")
    private String operationModule;

    @TableField("sensitive_data_type")
    private String sensitiveDataType;

    @TableField("data_fingerprint")
    private String dataFingerprint;

    @TableField("affected_count")
    private Integer affectedCount;

    @TableField("target_table")
    private String targetTable;

    @TableField("target_ids")
    private String targetIds;

    @TableField("operation_detail")
    private String operationDetail;

    @TableField("approval_required")
    private Boolean approvalRequired;

    @TableField("approval_id")
    private String approvalId;

    @TableField("risk_score")
    private Integer riskScore;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("user_agent")
    private String userAgent;

    @TableField("device_fingerprint")
    private String deviceFingerprint;

    @TableField("location")
    private String location;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String realName;

    @TableField(exist = false)
    private String deptName;

}
