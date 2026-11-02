package com.scmcloud.common.log.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Payload accepted by scm-audit's {@code POST /sys-audit-log}; field names mirror the audit
 * entity so the record can be persisted without translation. Tenant and id are filled by the
 * audit service (tenant from the forwarded header).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRecord {

    private String userId;
    private String username;
    private String realName;
    private String deptId;

    private String operationType;
    private String operationModule;
    private String operationDesc;

    private String requestUri;
    private String requestMethod;
    private String requestParams;
    private String responseData;
    private Integer responseStatus;

    private String ipAddress;
    private String location;
    private String userAgent;

    private String businessType;
    private String businessId;
    private String oldValue;
    private String newValue;

    private Integer riskLevel;
    private Integer status;
    private String errorMsg;
    private Integer executeTime;

    private LocalDateTime createTime;
}
