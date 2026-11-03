package com.scmcloud.tenant.service.provisioning;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Result of tenant provisioning. Contains all IDs created during onboarding.
 */
@Data
@Builder
public class TenantProvisioningResult {

    private UUID tenantId;
    private String tenantCode;
    private UUID adminUserId;
    private String adminUsername;
    private UUID adminRoleId;
    private String adminRoleCode;
    private UUID subscriptionId;
    private boolean success;
    private String errorMessage;

    public static TenantProvisioningResult success(UUID tenantId, String tenantCode,
                                                    UUID adminUserId, String adminUsername,
                                                    UUID adminRoleId, UUID subscriptionId) {
        return TenantProvisioningResult.builder()
                .tenantId(tenantId)
                .tenantCode(tenantCode)
                .adminUserId(adminUserId)
                .adminUsername(adminUsername)
                .adminRoleId(adminRoleId)
                .adminRoleCode("TENANT_ADMIN")
                .subscriptionId(subscriptionId)
                .success(true)
                .build();
    }

    public static TenantProvisioningResult failure(String errorMessage) {
        return TenantProvisioningResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
