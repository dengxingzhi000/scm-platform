package com.cognora.scm.finance.service.support;

import com.cognora.platform.common.tenant.TenantContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;

/**
 * 财务单据租户归属校验（纵深防御）。
 *
 * <p>{@code TenantInterceptor} 已把 SELECT 限定在当前租户，正常情况下读不到
 * 他租户行；这里再对实体/请求体里的 {@code tenantId} 做一次比对，避免 SQL
 * 隔离关闭、XML 查询或缓存回填等路径把跨租户单据带进写流程。</p>
 *
 * <p>跨租户一律抛 {@link AccessDeniedException}，由全局异常处理返回 403。</p>
 */
public final class FinanceTenantGuard {

    private FinanceTenantGuard() {
    }

    public static void verifyOwnership(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return;
        }
        String currentTenant = TenantContextHolder.getRequiredTenantId().toString();
        if (!currentTenant.equals(tenantId)) {
            throw new AccessDeniedException("禁止操作其他租户的财务单据");
        }
    }
}
