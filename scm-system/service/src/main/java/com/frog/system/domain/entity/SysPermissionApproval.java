package com.frog.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.frog.common.mybatisPlus.handler.StringArrayTypeHandler;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 权限申请审批表
 * </p>
 *
 * @author author
 * @since 2025-10-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@TableName("sys_permission_approval")
public class SysPermissionApproval {

    @TableId(value = "id", type = IdType.NONE)
    private UUID id;

    @TableField("applicant_id")
    private UUID applicantId;

    @TableField("approval_type")
    private Integer approvalType;

    @TableField("target_user_id")
    private UUID targetUserId;

    @TableField("role_ids")
    private UUID[] roleIds;

    @TableField("permission_ids")
    private UUID[] permissionIds;

    @TableField("effective_time")
    private LocalDateTime effectiveTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("apply_reason")
    private String applyReason;

    @TableField("business_justification")
    private String businessJustification;

    @TableField("approval_status")
    private Integer approvalStatus;

    @TableField("current_approver_id")
    private UUID currentApproverId;

    @TableField(value = "approval_chain", typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> approvalChain;

    @TableField("approved_by")
    private UUID approvedBy;

    @TableField("approved_time")
    private LocalDateTime approvedTime;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 冗余字段 ====================

    @TableField("applicant_name")
    private String applicantName;

    @TableField("applicant_dept_name")
    private String applicantDeptName;

    @TableField("target_user_name")
    private String targetUserName;

    @TableField("approver_name")
    private String approverName;

    @TableField(value = "role_names", typeHandler = StringArrayTypeHandler.class)
    private String[] roleNames;

    @TableField(value = "permission_names", typeHandler = StringArrayTypeHandler.class)
    private String[] permissionNames;
}
