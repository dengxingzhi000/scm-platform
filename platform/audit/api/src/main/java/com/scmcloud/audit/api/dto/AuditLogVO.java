package com.scmcloud.audit.api.dto;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AuditLogVO {

    private Long id;
    private String module;
    private String operation;
    private Long operatorId;
    private String operatorName;
    private String ipAddress;
    private String method;
    private Integer result;
    private String errorMsg;
    private LocalDateTime createdAt;
}
