package com.cognora.platform.product.support;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.web.domain.SecurityUser;
import com.cognora.platform.common.web.util.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 商品域统一的租户解析入口。
 *
 * <p>租户只从登录态或租户上下文获取，绝不从请求参数/请求体读取；两者不一致直接拒绝。
 */
@Component
public class ProductTenantResolver {

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
            throw new AccessDeniedException("Tenant context is required for product operations");
        }
        return contextTenant;
    }

    public String resolveTenantIdString() {
        return resolveTenantId().toString();
    }
}
