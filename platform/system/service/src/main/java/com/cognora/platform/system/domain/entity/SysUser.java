package com.cognora.platform.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户表UUID主键)
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_user")
public class SysUser implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(
            value = "id",
            type = IdType.NONE
    )
    private UUID id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private UUID tenantId;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("real_name")
    private String realName;

    @TableField("id_card")
    private String idCard;

    @TableField("email")
    private String email;

    @TableField("phone")
    private String phone;

    @TableField("avatar")
    private String avatar;

    /** 用户时区（IANA 名称，如 Asia/Shanghai），用于个人资料与通知时间展示。 */
    @TableField("timezone")
    private String timezone;

    @TableField("status")
    private Integer status;

    @TableField("dept_id")
    private UUID deptId;

    @TableField("user_level")
    private Integer userLevel;

    @TableField("account_type")
    private Integer accountType;

    @TableField("user_type")
    private String userType;

    @TableField("data_scope")
    private String dataScope;

    @TableField("login_attempts")
    private Integer loginAttempts;

    @TableField("locked_until")
    private LocalDateTime lockedUntil;

    /** 锁定原因，供审计与用户详情展示。 */
    @TableField("lock_reason")
    private String lockReason;

    /** 最近一次账号状态变更时间/操作人。 */
    @TableField("status_changed_at")
    private LocalDateTime statusChangedAt;

    @TableField("status_changed_by")
    private UUID statusChangedBy;

    @TableField("password_expire_time")
    private LocalDateTime passwordExpireTime;

    @TableField("force_change_password")
    private Boolean forceChangePassword;

    @TableField("two_factor_enabled")
    private Boolean twoFactorEnabled;

    @TableField("two_factor_secret")
    private String twoFactorSecret;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private UUID createBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private UUID updateBy;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField("last_login_ip")
    private String lastLoginIp;

    @TableField("last_password_change_time")
    private LocalDateTime lastPasswordChangeTime;

    @TableLogic(value = "false", delval = "true")
    @TableField("deleted")
    private Boolean deleted;

    @TableField("remark")
    private String remark;

    /** 乐观锁版本号；MyBatis-Plus OptimisticLockerInnerInterceptor 已全局注册。 */
    @Version
    @TableField("version")
    private Integer version;
}
