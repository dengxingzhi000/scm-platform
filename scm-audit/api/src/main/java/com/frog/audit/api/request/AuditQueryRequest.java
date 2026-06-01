package com.frog.audit.api.request;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AuditQueryRequest {

    private String module;
    private Long operatorId;
    private Integer result;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int page;
    private int size;
}
