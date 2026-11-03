package com.scmcloud.system.config;

import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.system.service.ISysPermissionService;
import com.scmcloud.system.service.ISysRoleService;
import com.scmcloud.system.service.ISysDeptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Pre-warms critical caches on service startup.
 * Reduces cold-start latency for permission checks, role lookups, and department trees.
 *
 * Runs asynchronously to avoid blocking service readiness.
 * Failures are logged but do not prevent startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmingService {

    private final ISysPermissionService permissionService;
    private final ISysRoleService roleService;
    private final ISysDeptService deptService;

    @Async("tenantAwareTaskExecutor")
    @Order
    @EventListener(ApplicationReadyEvent.class)
    public void warmCaches() {
        log.info("[CacheWarming] Starting cache pre-load...");
        long start = System.currentTimeMillis();

        warmPermissionTree();
        warmApiPermissions();
        warmRoles();
        warmDeptTree();

        long elapsed = System.currentTimeMillis() - start;
        log.info("[CacheWarming] Completed in {}ms", elapsed);
    }

    private void warmPermissionTree() {
        try {
            permissionService.getPermissionTree();
            log.debug("[CacheWarming] permissionTree loaded");
        } catch (Exception e) {
            log.warn("[CacheWarming] Failed to warm permissionTree: {}", e.getMessage());
        }
    }

    private void warmApiPermissions() {
        try {
            permissionService.findApiPermissions();
            log.debug("[CacheWarming] apiPermissions loaded");
        } catch (Exception e) {
            log.warn("[CacheWarming] Failed to warm apiPermissions: {}", e.getMessage());
        }
    }

    private void warmRoles() {
        try {
            roleService.listAllRoles();
            log.debug("[CacheWarming] roles loaded");
        } catch (Exception e) {
            log.warn("[CacheWarming] Failed to warm roles: {}", e.getMessage());
        }
    }

    private void warmDeptTree() {
        try {
            // deptTree requires tenant context
            if (TenantContextHolder.getTenantId() != null) {
                deptService.getDeptTree();
                log.debug("[CacheWarming] deptTree loaded");
            } else {
                log.debug("[CacheWarming] Skipping deptTree (no tenant context)");
            }
        } catch (Exception e) {
            log.warn("[CacheWarming] Failed to warm deptTree: {}", e.getMessage());
        }
    }
}
