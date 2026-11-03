package com.scmcloud.tenant.api.query;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TenantDetailQuery {

    private String tenantId;
    private String tenantCode;
    private boolean includeConfig;
    private boolean includePackage;
    private boolean includeQuota;
}
