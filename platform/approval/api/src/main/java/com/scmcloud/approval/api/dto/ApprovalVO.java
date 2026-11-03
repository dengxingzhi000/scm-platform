package com.scmcloud.approval.api.dto;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ApprovalVO {

    private Long id;
    private String approvalNo;
    private String businessType;
    private Long businessId;
    private Long applicantId;
    private String title;
    private String content;
    private String status;
    private Long approverId;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
