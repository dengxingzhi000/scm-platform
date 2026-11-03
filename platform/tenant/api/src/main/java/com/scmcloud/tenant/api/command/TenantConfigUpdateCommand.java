package com.scmcloud.tenant.api.command;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TenantConfigUpdateCommand {

    private String configKey;
    private String configValue;
}
