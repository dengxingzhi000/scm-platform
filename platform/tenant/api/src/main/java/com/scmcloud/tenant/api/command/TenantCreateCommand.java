package com.scmcloud.tenant.api.command;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TenantCreateCommand {

    private String tenantName;
    private String tenantNameEn;
    private String tenantCode;
    private Integer tenantType;
    private String companyName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private String industry;
    private String packageId;
    private String adminUsername;
    private String adminEmail;
}
