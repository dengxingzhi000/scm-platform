package com.cognora.ecom.search.tenant;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.web.domain.SecurityUser;
import com.cognora.platform.common.web.util.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves the tenant that owns a search request.
 *
 * <p>Sources, in order: the authenticated user's token-bound tenant, then the tenant
 * context established by {@code TenantFilter}. Request parameters are never consulted.
 * When the token tenant and the context tenant disagree the request is rejected instead
 * of silently choosing one; a missing tenant is rejected as well, so an unscoped search
 * can never fall back to "all tenants".
 */
@Component
public class SearchTenantResolver {

    public UUID resolveTenantId() {
        SecurityUser user = SecurityUtils.getCurrentUser();
        UUID tokenTenant = user == null ? null : user.getTenantId();
        UUID contextTenant = TenantContextHolder.getTenantId();

        if (tokenTenant != null) {
            if (contextTenant != null && !contextTenant.equals(tokenTenant)) {
                throw new AccessDeniedException("Tenant context does not match the authenticated tenant");
            }
            return tokenTenant;
        }
        if (contextTenant == null) {
            throw new AccessDeniedException("Tenant context is required for search operations");
        }
        return contextTenant;
    }

    public String resolveTenantIdString() {
        return resolveTenantId().toString();
    }
}
