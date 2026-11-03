package com.cognora.scm.warehouse.service;

import com.cognora.scm.warehouse.job.NearExpiryAlertJob;
import com.cognora.scm.warehouse.dto.NearExpiryProductVO;
import java.util.List;
import java.util.UUID;

public interface InventoryBatchService {
    List<NearExpiryProductVO> getNearExpiryProducts(UUID tenantId, NearExpiryAlertJob.AlertLevel alertLevel);
}
