package com.scmcloud.supplier.service;

import com.scmcloud.supplier.dto.OverdueApprovalTaskVO;
import java.util.List;
import java.util.UUID;

public interface PurchaseApprovalService {
    List<OverdueApprovalTaskVO> getOverdueApprovalTasks(UUID tenantId);
    boolean escalateApproval(String taskId);
    boolean autoApprove(String taskId);
}
