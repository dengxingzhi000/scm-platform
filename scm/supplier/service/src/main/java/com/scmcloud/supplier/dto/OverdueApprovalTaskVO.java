package com.scmcloud.supplier.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class OverdueApprovalTaskVO {
    private UUID tenantId;
    private String taskId;
    private String taskType;
    private String approverId;
    private String approverName;
    private LocalDateTime createTime;
    private LocalDateTime deadline;
    private int hoursOverdue;
    private String severity; // CRITICAL, WARNING, APPROACHING
    private boolean autoApproveEnabled;

    public boolean isSeverelyOverdue() {
        return "CRITICAL".equals(severity);
    }

    public boolean isOverdue() {
        return hoursOverdue > 0;
    }

    public boolean isAboutToTimeout() {
        return "APPROACHING".equals(severity);
    }
}
