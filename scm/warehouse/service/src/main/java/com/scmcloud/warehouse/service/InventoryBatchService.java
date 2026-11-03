package com.scmcloud.warehouse.service;

import com.scmcloud.warehouse.job.NearExpiryAlertJob;
import com.scmcloud.warehouse.dto.NearExpiryProductVO;
import java.util.List;
import java.util.UUID;

public interface InventoryBatchService {
    List<NearExpiryProductVO> getNearExpiryProducts(UUID tenantId, NearExpiryAlertJob.AlertLevel alertLevel);
}
