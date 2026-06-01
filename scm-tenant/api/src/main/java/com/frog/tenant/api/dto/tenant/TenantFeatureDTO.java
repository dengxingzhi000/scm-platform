package com.frog.tenant.api.dto.tenant;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TenantFeatureDTO {

    private String id;
    private String tenantId;
    private String featureCode;
    private String featureName;
    private Boolean enabled;
    private Integer usageLimit;
    private Integer currentUsage;
    private LocalDateTime expireAt;
}
