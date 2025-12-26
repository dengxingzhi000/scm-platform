package com.frog.common.tenant;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 租户上下文持有者
 * 使用 ThreadLocal 存储当前线程的租户ID
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
public class TenantContextHolder {

    private static final ThreadLocal<UUID> TENANT_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前租户ID
     */
    public static void setTenantId(UUID tenantId) {
        if (tenantId == null) {
            log.warn("Setting null tenant ID, this may cause data isolation issues");
        }
        TENANT_ID_HOLDER.set(tenantId);
        log.debug("Set tenant ID: {}", tenantId);
    }

    /**
     * 获取当前租户ID
     */
    public static UUID getTenantId() {
        UUID tenantId = TENANT_ID_HOLDER.get();
        if (tenantId == null) {
            log.warn("Tenant ID is null in current thread, please check tenant filter/interceptor");
        }
        return tenantId;
    }

    /**
     * 获取当前租户ID（必须存在，否则抛异常）
     */
    public static UUID getRequiredTenantId() {
        UUID tenantId = getTenantId();
        if (tenantId == null) {
            throw new TenantNotFoundException("Tenant ID is required but not found in current context");
        }
        return tenantId;
    }

    /**
     * 清除当前租户ID
     */
    public static void clear() {
        UUID tenantId = TENANT_ID_HOLDER.get();
        TENANT_ID_HOLDER.remove();
        log.debug("Cleared tenant ID: {}", tenantId);
    }

    /**
     * 在指定租户上下文中执行操作
     */
    public static <T> T executeInTenantContext(UUID tenantId, TenantContextCallback<T> callback) {
        UUID originalTenantId = getTenantId();
        try {
            setTenantId(tenantId);
            return callback.execute();
        } finally {
            if (originalTenantId != null) {
                setTenantId(originalTenantId);
            } else {
                clear();
            }
        }
    }

    /**
     * 租户上下文回调接口
     */
    @FunctionalInterface
    public interface TenantContextCallback<T> {
        T execute();
    }

    /**
     * 租户未找到异常
     */
    public static class TenantNotFoundException extends RuntimeException {
        public TenantNotFoundException(String message) {
            super(message);
        }
    }
}