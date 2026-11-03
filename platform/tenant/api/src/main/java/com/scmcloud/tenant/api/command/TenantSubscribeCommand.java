package com.scmcloud.tenant.api.command;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TenantSubscribeCommand {

    private String packageId;
    private Integer subscriptionType;
    private Boolean autoRenew;
}
