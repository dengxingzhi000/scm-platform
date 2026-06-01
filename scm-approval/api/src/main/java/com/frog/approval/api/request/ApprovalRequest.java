package com.frog.approval.api.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ApprovalRequest {

    private String businessType;
    private Long businessId;
    private Long applicantId;
    private String title;
    private String content;
    private String priority;
}
