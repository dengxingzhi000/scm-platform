package com.cognora.scm.warehouse.service;

import com.cognora.scm.warehouse.job.NearExpiryAlertJob;
import com.cognora.scm.warehouse.dto.NearExpiryProductVO;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NotificationService {
    void sendNearExpiryAlert(UUID tenantId, List<NearExpiryProductVO> products, Map<NearExpiryAlertJob.AlertLevel, Long> alertCounts);
}
