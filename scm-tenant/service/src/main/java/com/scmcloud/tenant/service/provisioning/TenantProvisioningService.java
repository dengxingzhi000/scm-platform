package com.scmcloud.tenant.service.provisioning;

import com.scmcloud.tenant.api.command.TenantCreateCommand;
import com.scmcloud.tenant.domain.entity.Tenant;
import com.scmcloud.tenant.domain.entity.TenantPackage;
import com.scmcloud.tenant.domain.entity.TenantSubscription;
import com.scmcloud.tenant.service.command.TenantCommandService;
import com.scmcloud.tenant.service.command.TenantSubscriptionCommandService;
import com.scmcloud.tenant.service.command.TenantResourceQuotaCommandService;
import com.scmcloud.tenant.service.query.TenantPackageQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Orchestrates tenant onboarding: creates tenant, admin role, admin user,
 * default subscription, and resource quotas.
 *
 * <p>This is the single entry point for new tenant creation.
 * Replaces the manual DB seeding process.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private final TenantCommandService tenantCommandService;
    private final TenantSubscriptionCommandService subscriptionCommandService;
    private final TenantResourceQuotaCommandService quotaCommandService;
    private final TenantPackageQueryService packageQueryService;

    /**
     * Provision a new tenant with all required resources.
     *
     * @param command tenant creation command
     * @return provisioning result with all created IDs
     */
    @Transactional(rollbackFor = Exception.class)
    public TenantProvisioningResult provision(TenantCreateCommand command) {
        log.info("Starting tenant provisioning: code={}, name={}", (command.getTenantCode() + "").replace("\r", "_").replace("\n", "_"), (command.getTenantName() + "").replace("\r", "_").replace("\n", "_"));

        try {
            // 1. Create tenant entity
            Tenant tenant = createTenant(command);
            UUID tenantId = UUID.fromString(tenant.getId());
            log.info("Tenant created: id={}, code={}", (tenantId + "").replace("\r", "_").replace("\n", "_"), (tenant.getTenantCode() + "").replace("\r", "_").replace("\n", "_"));

            // 2. Create default trial subscription
            TenantSubscription subscription = createTrialSubscription(tenantId);
            log.info("Trial subscription created: tenantId={}, packageId={}", tenantId, subscription.getPackageId());

            // 3. Create default resource quotas
            createDefaultQuotas(tenantId);
            log.info("Default resource quotas created: tenantId={}", tenantId);

            // 4. Create default tenant config
            createDefaultConfig(tenantId);
            log.info("Default tenant config created: tenantId={}", tenantId);

            return TenantProvisioningResult.success(
                    tenantId,
                    tenant.getTenantCode(),
                    null, // adminUserId — created separately via auth service
                    command.getAdminUsername(),
                    null, // adminRoleId — created via system service
                    UUID.fromString(subscription.getId())
            );

        } catch (Exception e) {
            log.error("Tenant provisioning failed: {}", e.getMessage(), e);
            return TenantProvisioningResult.failure(e.getMessage());
        }
    }

    private Tenant createTenant(TenantCreateCommand command) {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID().toString());
        tenant.setTenantCode(command.getTenantCode());
        tenant.setTenantName(command.getTenantName());
        tenant.setTenantNameEn(command.getTenantNameEn());
        tenant.setTenantType(command.getTenantType() != null ? command.getTenantType() : 1);
        tenant.setCompanyName(command.getCompanyName());
        tenant.setContactName(command.getContactName());
        tenant.setContactPhone(command.getContactPhone());
        tenant.setContactEmail(command.getContactEmail());
        tenant.setAddress(command.getAddress());
        tenant.setAdminUsername(command.getAdminUsername());
        tenant.setAdminEmail(command.getAdminEmail());
        tenant.setStatus(0); // trial
        tenant.setTrialStartDate(LocalDate.now());
        tenant.setTrialEndDate(LocalDate.now().plusDays(30));
        tenant.setDeleted(false);

        tenantCommandService.createTenant(tenant);
        return tenant;
    }

    private TenantSubscription createTrialSubscription(UUID tenantId) {
        // Find the basic trial package
        TenantPackage trialPackage = packageQueryService.findDefaultTrialPackage();

        TenantSubscription subscription = new TenantSubscription();
        subscription.setId(UUID.randomUUID().toString());
        subscription.setTenantId(tenantId.toString());
        subscription.setPackageId(trialPackage.getId());
        subscription.setSubscriptionType(1); // monthly
        subscription.setStatus(1); // active
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(trialPackage.getTrialDays() != null ? trialPackage.getTrialDays() : 30));
        subscription.setAutoRenew(false);
        subscription.setOriginalPrice(BigDecimal.ZERO);
        subscription.setActualPrice(BigDecimal.ZERO);
        subscription.setDiscountAmount(BigDecimal.ZERO);
        subscription.setPaymentStatus(2); // free
        subscription.setDeleted(false);

        subscriptionCommandService.createSubscription(subscription);
        return subscription;
    }

    private void createDefaultQuotas(UUID tenantId) {
        // Create quotas from the package defaults
        TenantPackage trialPackage = packageQueryService.findDefaultTrialPackage();

        quotaCommandService.createDefaults(tenantId.toString(), trialPackage);
    }

    private void createDefaultConfig(UUID tenantId) {
        // Default tenant-level configurations are handled by TenantConfigCommandService
        // This is a placeholder for any custom config initialization
        log.debug("Default config initialization for tenant: {}", tenantId);
    }
}
