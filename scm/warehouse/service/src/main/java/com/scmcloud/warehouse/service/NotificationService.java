package com.scmcloud.warehouse.service;

import com.scmcloud.warehouse.job.NearExpiryAlertJob;
import com.scmcloud.warehouse.dto.NearExpiryProductVO;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NotificationService {
    void sendNearExpiryAlert(UUID tenantId, List<NearExpiryProductVO> products, Map<NearExpiryAlertJob.AlertLevel, Long> alertCounts);
}
