package com.cognora.platform.common.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户登录历史（来源 db_audit.sys_audit_log）。
 */
@Data
public class LoginHistoryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID userId;
    private String username;
    /** LOGIN / LOGIN_FAILURE / LOGOUT */
    private String operationType;
    private Integer status;
    private String ipAddress;
    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
