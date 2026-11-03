package com.scmcloud.tenant.api.dto.subscription;

import java.math.BigDecimal;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TenantPackageDTO {

    private String id;
    private String packageCode;
    private String packageName;
    private Integer packageLevel;
    private BigDecimal priceMonthly;
    private BigDecimal priceYearly;
    private Integer maxUsers;
    private Integer maxWarehouses;
    private Integer maxSkus;
    private Integer maxOrdersPerDay;
    private String features;
    private Boolean enabled;
}
