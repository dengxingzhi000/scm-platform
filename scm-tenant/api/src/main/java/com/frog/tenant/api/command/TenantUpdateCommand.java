package com.frog.tenant.api.command;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TenantUpdateCommand {

    private String tenantName;
    private String companyName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String industry;
    private String domain;
}
