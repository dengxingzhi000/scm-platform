package scm.approval.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serial;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "权限申请审批表")
public class SysPermissionApproval implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @Schema(description = "申请人ID(跨库关联db_user.sys_user)")
    @TableField("applicant_id")
    private String applicantId;

    @Schema(description = "申请类型:1-角色申请,2-权限申请,3-临时授权")
    @TableField("approval_type")
    private Integer approvalType;

    @Schema(description = "目标用户ID(跨库关联db_user.sys_user)")
    @TableField("target_user_id")
    private String targetUserId;

    @Schema(description = "角色ID数组(跨库关联db_permission.sys_role)")
    @TableField("role_ids")
    private String roleIds;

    @Schema(description = "权限ID数组(跨库关联db_permission.sys_permission)")
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

    @Schema(description = "审批状态:0-待审批,1-审批中,2-已批准,3-已拒绝,4-已撤回")
    @TableField("approval_status")
    private Integer approvalStatus;

    @TableField("current_approver_id")
    private String currentApproverId;

    @Schema(description = "审批链(JSONB格式)")
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

    @Schema(description = "冗余字段：申请人姓名")
    @TableField("applicant_name")
    private String applicantName;

    @Schema(description = "冗余字段：申请人部门名称")
    @TableField("applicant_dept_name")
    private String applicantDeptName;

    @Schema(description = "冗余字段：目标用户姓名")
    @TableField("target_user_name")
    private String targetUserName;

    @Schema(description = "冗余字段：审批人姓名")
    @TableField("approver_name")
    private String approverName;

    @Schema(description = "冗余字段：角色名称数组")
    @TableField("role_names")
    private String roleNames;

    @Schema(description = "冗余字段：权限名称数组")
    @TableField("permission_names")
    private String permissionNames;


}
