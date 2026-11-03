package com.scmcloud.tenant.api.service;

import com.scmcloud.tenant.api.command.TenantCreateCommand;
import com.scmcloud.tenant.api.command.TenantUpdateCommand;
import com.scmcloud.tenant.api.dto.common.PageResult;
import com.scmcloud.tenant.api.dto.tenant.QuotaCheckResultDTO;
import com.scmcloud.tenant.api.dto.tenant.QuotaType;
import com.scmcloud.tenant.api.dto.tenant.TenantDTO;
import com.scmcloud.tenant.api.dto.tenant.TenantResourceQuotaDTO;
import com.scmcloud.tenant.api.query.TenantQuery;

public interface TenantDubboService {

    TenantDTO getById(String tenantId);

    TenantDTO getByCode(String tenantCode);

    PageResult<TenantDTO> queryTenants(TenantQuery query);

    boolean checkFeatureEnabled(String tenantId, String featureCode);

    TenantResourceQuotaDTO getResourceQuota(String tenantId);

    QuotaCheckResultDTO checkQuota(String tenantId, QuotaType quotaType, int required);

    String createTenant(TenantCreateCommand command);

    void updateTenant(String tenantId, TenantUpdateCommand command);

    void suspendTenant(String tenantId);

    void activateTenant(String tenantId);
}
