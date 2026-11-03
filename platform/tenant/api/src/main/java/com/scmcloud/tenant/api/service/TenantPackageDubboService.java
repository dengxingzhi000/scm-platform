package com.scmcloud.tenant.api.service;

import com.scmcloud.tenant.api.command.TenantSubscribeCommand;
import com.scmcloud.tenant.api.dto.subscription.TenantPackageDTO;
import com.scmcloud.tenant.api.dto.subscription.TenantSubscriptionDTO;

import java.util.List;

public interface TenantPackageDubboService {

    TenantPackageDTO getPackageById(String packageId);

    TenantPackageDTO getTenantCurrentPackage(String tenantId);

    List<TenantPackageDTO> listAvailablePackages();

    TenantSubscriptionDTO getActiveSubscription(String tenantId);

    String subscribe(String tenantId, TenantSubscribeCommand command);
}
