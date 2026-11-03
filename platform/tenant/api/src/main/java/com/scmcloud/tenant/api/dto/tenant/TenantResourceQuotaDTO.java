package com.scmcloud.tenant.api.dto.tenant;

import java.math.BigDecimal;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TenantResourceQuotaDTO {

    private String id;
    private String tenantId;
    private Integer maxUsers;
    private Integer currentUsers;
    private Integer maxWarehouses;
    private Integer currentWarehouses;
    private Integer maxSkus;
    private Integer currentSkus;
    private Integer maxOrdersPerDay;
    private Integer currentOrdersToday;
    private Integer maxStorageGb;
    private BigDecimal currentStorageGb;
    private Integer maxApiCallsPerDay;
    private Integer currentApiCallsToday;
}
