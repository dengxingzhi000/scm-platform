package com.scmcloud.supplier.service;

import com.scmcloud.supplier.dto.OverdueApprovalTaskVO;

public interface NotificationService {
    void sendApprovalReminder(OverdueApprovalTaskVO task);
}
