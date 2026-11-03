package com.cognora.scm.supplier.service;

import com.cognora.scm.supplier.dto.OverdueApprovalTaskVO;

public interface NotificationService {
    void sendApprovalReminder(OverdueApprovalTaskVO task);
}
