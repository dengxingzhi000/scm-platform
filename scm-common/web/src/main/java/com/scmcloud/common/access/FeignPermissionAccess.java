package com.scmcloud.common.access;

import com.scmcloud.common.rest.client.SysPermissionServiceClient;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.security.PermissionService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 基于 RestClient 的 PermissionService 降级实现
 *
 * <p>仅当不存在其他 PermissionService bean（如 DubboPermissionServiceAdapter）时激活
 *
 * <p>安全特性：
 * <ul>
 *   <li>Fail-Closed：服务调用失败时抛出 PermissionServiceException 拒绝访问</li>
 *   <li>Sentinel 熔断由 SysPermissionServiceClient 内置处理，此处无需重复</li>
 * </ul>
 *
 * @author deng
 * @version 4.0
 * @since 2025-12-12
 */
@Component
@ConditionalOnMissingBean(PermissionService.class)
@Slf4j
public class FeignPermissionAccess implements PermissionService {
    private final SysPermissionServiceClient permissionServiceClient;
    private final MeterRegistry meterRegistry;

    public FeignPermissionAccess(SysPermissionServiceClient permissionServiceClient, MeterRegistry meterRegistry) {
        this.permissionServiceClient = permissionServiceClient;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public List<String> findPermissionsByUrl(String url, String method) {
        try {
            List<String> permissions = permissionServiceClient.findPermissionsByUrl(url, method);
            meterRegistry.counter("security.permissions.rest.lookup.success").increment();
            log.debug("Permission lookup success via RestClient: url={}, method={}, permissions={}",
                     url, method, permissions);
            return permissions != null ? permissions : List.of();

        } catch (Exception ex) {
            meterRegistry.counter("security.permissions.rest.lookup.fail").increment();
            log.error("SECURITY: Permission lookup failed via RestClient - DENYING ACCESS. " +
                     "url={}, method={}", url, method, ex);
            throw new PermissionServiceException(
                "Permission service unavailable via RestClient - access denied as safety measure", ex);
        }
    }

    @Override
    public Set<String> findAllPermissionsByUserId(UUID userId) {
        try {
            ApiResponse<Set<String>> resp = permissionServiceClient.getUserPermissions(userId);
            Set<String> perms = resp != null ? resp.data() : null;
            meterRegistry.counter("security.permissions.rest.user.success").increment();
            log.debug("User permission lookup success via RestClient: userId={}, count={}",
                     userId, perms != null ? perms.size() : 0);
            return perms != null ? perms : Set.of();

        } catch (Exception ex) {
            meterRegistry.counter("security.permissions.rest.user.fail").increment();
            log.error("SECURITY: User permission lookup failed via RestClient - DENYING ACCESS. " +
                     "userId={}", userId, ex);
            throw new PermissionServiceException(
                "Permission service unavailable via RestClient - access denied as safety measure", ex);
        }
    }
}
