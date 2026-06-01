package com.frog.tenant.api.service;

import com.frog.tenant.api.command.TenantSubscribeCommand;
import com.frog.tenant.api.dto.subscription.TenantPackageDTO;
import com.frog.tenant.api.dto.subscription.TenantSubscriptionDTO;

import java.util.List;

public interface TenantPackageDubboService {

    TenantPackageDTO getPackageById(String packageId);

    TenantPackageDTO getTenantCurrentPackage(String tenantId);

    List<TenantPackageDTO> listAvailablePackages();

    TenantSubscriptionDTO getActiveSubscription(String tenantId);

    String subscribe(String tenantId, TenantSubscribeCommand command);
}
