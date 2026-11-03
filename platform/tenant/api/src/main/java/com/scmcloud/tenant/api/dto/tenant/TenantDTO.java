package com.scmcloud.tenant.api.dto.tenant;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TenantDTO {

    private String id;
    private String tenantCode;
    private String tenantName;
    private Integer tenantType;
    private String companyName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private Integer status;
    private String industry;
    private String domain;
    private LocalDateTime createTime;
}
