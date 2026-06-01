package com.frog.audit.api.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AuditLogRequest {

    private String module;
    private String operation;
    private Long operatorId;
    private String operatorName;
    private String ipAddress;
    private String method;
    private String params;
    private Integer result;
    private String errorMsg;
}
