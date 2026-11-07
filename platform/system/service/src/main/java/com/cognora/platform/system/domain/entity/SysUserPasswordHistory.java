package com.cognora.platform.system.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户密码历史，用于禁止重复使用最近 N 次密码。
 */
@Data
@TableName("sys_user_password_history")
public class SysUserPasswordHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private UUID id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private UUID tenantId;

    @TableField("user_id")
    private UUID userId;

    @TableField("password_hash")
    private String passwordHash;

    /** INITIAL / CHANGE / RESET */
    @TableField("change_type")
    private String changeType;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private UUID createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
