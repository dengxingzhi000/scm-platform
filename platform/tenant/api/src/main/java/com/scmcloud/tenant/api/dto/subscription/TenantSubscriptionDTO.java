package com.scmcloud.tenant.api.dto.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TenantSubscriptionDTO {

    private String id;
    private String tenantId;
    private String packageId;
    private Integer subscriptionType;
    private Integer status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean autoRenew;
    private BigDecimal actualPrice;
    private Integer paymentStatus;
    private LocalDateTime createTime;
}
