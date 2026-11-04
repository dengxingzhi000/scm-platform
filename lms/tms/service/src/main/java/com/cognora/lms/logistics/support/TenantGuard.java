package com.cognora.lms.logistics.support;

import com.cognora.platform.common.tenant.TenantContextHolder;
import org.springframework.security.access.AccessDeniedException;

/**
 * 物流写操作的租户边界校验。
 *
 * <p>租户 SQL 拦截器只在 {@link TenantContextHolder} 中存在租户时才会追加
 * {@code tenant_id} 谓词；上下文为空时它不做任何改写，查询会退化为跨租户可见。
 * 因此写路径必须显式要求租户上下文，缺失时按 403 拒绝，而不是静默放行。</p>
 */
public final class TenantGuard {

    private TenantGuard() {
    }

    public static void requireTenant() {
        if (TenantContextHolder.getTenantId() == null) {
            throw new AccessDeniedException("Tenant context is required for logistics operations");
        }
    }
}
