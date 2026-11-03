package com.scmcloud.tenant.api.dto.config;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TenantConfigDTO {

    private String id;
    private String tenantId;
    private String configCategory;
    private String configKey;
    private String configValue;
    private String valueType;
    private String description;
}
