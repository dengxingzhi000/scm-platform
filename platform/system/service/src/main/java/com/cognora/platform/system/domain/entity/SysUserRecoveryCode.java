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
 * TOTP 一次性恢复码（SHA-256 哈希存储）。
 */
@Data
@TableName("sys_user_recovery_code")
public class SysUserRecoveryCode implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private UUID id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private UUID tenantId;

    @TableField("user_id")
    private UUID userId;

    @TableField("code_hash")
    private String codeHash;

    @TableField("used")
    private Boolean used;

    @TableField("used_at")
    private LocalDateTime usedAt;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
