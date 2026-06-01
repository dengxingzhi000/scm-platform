package scm.approval.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serial;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 权限申请审批表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_permission_approval")
public class SysPermissionApproval implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("applicant_id")
    private String applicantId;

    @TableField("approval_type")
    private Integer approvalType;

    @TableField("target_user_id")
    private String targetUserId;

    @TableField("role_ids")
    private String roleIds;

    @TableField("permission_ids")
    private String permissionIds;

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
    private String currentApproverId;

    @TableField("approval_chain")
    private String approvalChain;

    @TableField("approved_by")
    private String approvedBy;

    @TableField("approved_time")
    private LocalDateTime approvedTime;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("applicant_name")
    private String applicantName;

    @TableField("applicant_dept_name")
    private String applicantDeptName;

    @TableField("target_user_name")
    private String targetUserName;

    @TableField("approver_name")
    private String approverName;

    @TableField("role_names")
    private String roleNames;

    @TableField("permission_names")
    private String permissionNames;

}
