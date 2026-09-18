package com.scmcloud.common.tenant;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 绉熸埛涓婁笅鏂囨寔鏈夛拷
 * 浣跨敤 ThreadLocal 瀛樺偍褰撳墠绾跨▼鐨勭鎴稩D
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
public class TenantContextHolder {
    private static final ThreadLocal<UUID> TENANT_ID_HOLDER = new ThreadLocal<>();

    /**
     * 璁剧疆褰撳墠绉熸埛 ID
     */
    public static void setTenantId(UUID tenantId) {
        if (tenantId == null) {
            log.warn("Setting null tenant ID, this may cause data isolation issues");
        }
        TENANT_ID_HOLDER.set(tenantId);
        log.debug("Set tenant ID: {}", tenantId);
    }

    /**
     * 鑾峰彇褰撳墠绉熸埛 ID
     */
    public static UUID getTenantId() {
        UUID tenantId = TENANT_ID_HOLDER.get();
        if (tenantId == null) {
            log.debug("Tenant ID is null in current thread");
        }
        return tenantId;
    }

    /**
     * 鑾峰彇褰撳墠绉熸埛ID锛堝繀椤诲瓨鍦紝鍚﹀垯鎶涘紓甯革級
     */
    public static UUID getRequiredTenantId() {
        UUID tenantId = getTenantId();
        if (tenantId == null) {
            throw new TenantNotFoundException("Tenant ID is required but not found in current context");
        }
        return tenantId;
    }

    /**
     * 娓呴櫎褰撳墠绉熸埛 ID
     */
    public static void clear() {
        UUID tenantId = TENANT_ID_HOLDER.get();
        TENANT_ID_HOLDER.remove();
        log.debug("Cleared tenant ID: {}", tenantId);
    }

    /**
     * 鍦ㄦ寚瀹氱鎴蜂笂涓嬫枃涓墽琛屾搷锟?
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
     * 绉熸埛涓婁笅鏂囧洖璋冩帴锟?
     */
    @FunctionalInterface
    public interface TenantContextCallback<T> {
        T execute();
    }

    /**
     * 绉熸埛鏈壘鍒板紓锟?
     */
    public static class TenantNotFoundException extends RuntimeException {
        public TenantNotFoundException(String message) {
            super(message);
        }
    }
}