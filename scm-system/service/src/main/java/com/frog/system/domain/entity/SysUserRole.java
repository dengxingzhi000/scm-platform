package com.frog.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户角色关联�?- 支持临时角色授权
 *
 * @author Deng
 * @since 2025-12-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_user_role")
public class SysUserRole {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("tenant_id")
    private UUID tenantId;

    private UUID userId;

    private UUID roleId;

    @TableField("effective_time")
    private LocalDateTime effectiveTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    private Integer approvalStatus;

    private UUID approvedBy;

    private LocalDateTime approvedTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private UUID createBy;

    // ==================== 冗余字段（来�?db_user.sys_user�?===================

    @TableField("username")
    private String username;

    @TableField("real_name")
    private String realName;

    @TableField("user_status")
    private Integer userStatus;

    /**
     * 审批状态枚�?
     */
    @Getter
    public enum ApprovalStatus {
        PENDING(0, "待审批"),
        IN_PROGRESS(1, "审批中"),
        APPROVED(2, "已批准"),
        REJECTED(3, "已拒绝");

        private final int code;
        private final String desc;

        ApprovalStatus(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }

    /**
     * 判断是否为临时授�?
     */
    public boolean isTemporary() {
        return effectiveTime != null && expireTime != null;
    }

    /**
     * 判断是否在有效期�?
     */
    public boolean isEffective() {
        if (approvalStatus == null || approvalStatus != ApprovalStatus.APPROVED.getCode()) {
            return false;
        }
        if (!isTemporary()) {
            return true; // 永久授权
        }
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(effectiveTime) && now.isBefore(expireTime);
    }
}
