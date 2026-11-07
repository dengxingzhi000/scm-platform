package com.cognora.platform.common.dto.user;

import com.cognora.platform.common.web.annotation.Sensitive;
import com.cognora.platform.common.web.enums.SensitiveType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 用户列表行。敏感字段在 JSON 序列化阶段统一脱敏。
 */
@Data
public class UserSummaryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String username;

    @Sensitive(type = SensitiveType.NAME)
    private String realName;

    @Sensitive(type = SensitiveType.ID_CARD)
    private String idCard;

    @Sensitive(type = SensitiveType.EMAIL)
    private String email;

    @Sensitive(type = SensitiveType.MOBILE)
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
    private LocalDateTime lastLoginTime;

    private String lastLoginIp;
    private String remark;
    private Integer version;

    private List<UUID> roleIds;
    private List<String> roleNames;
    private List<String> permissions;
}
