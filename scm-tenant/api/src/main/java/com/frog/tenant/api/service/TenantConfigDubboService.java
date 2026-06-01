package com.frog.tenant.api.service;

import com.frog.tenant.api.command.TenantConfigUpdateCommand;
import com.frog.tenant.api.dto.config.TenantConfigDTO;

import java.util.List;
import java.util.Map;

public interface TenantConfigDubboService {

    List<TenantConfigDTO> listConfigs(String tenantId);

    String getConfigValue(String tenantId, String configKey);

    void updateConfig(String tenantId, TenantConfigUpdateCommand command);

    Map<String, String> getFeatureFlags(String tenantId);
}
