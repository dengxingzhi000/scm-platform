package com.cognora.platform.common.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 用户详情/编辑回显。
 *
 * <p>本 DTO 不使用 {@code @Sensitive} 注解：是否脱敏由服务层根据调用方是否持有
 * {@code system:user:view:sensitive} 决定，否则编辑表单会把掩码值回写到数据库。</p>
 */
@Data
public class UserDetailDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String username;
    private String realName;
    private String idCard;
    private String email;
    private String phone;
    private String avatar;
    private String timezone;
    private Integer status;
    private UUID deptId;
    private String deptName;
    private Integer userLevel;
    private Integer accountType;
    private String userType;
    private String dataScope;
    private Integer loginAttempts;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lockedUntil;

    private String lockReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime passwordExpireTime;

    private Boolean forceChangePassword;
    private Boolean twoFactorEnabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    private UUID createBy;
    private UUID updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastPasswordChangeTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime statusChangedAt;

    private UUID statusChangedBy;
    private String remark;
    private Integer version;

    private List<UUID> roleIds;
    private List<String> roleNames;
    private List<String> permissions;
}
