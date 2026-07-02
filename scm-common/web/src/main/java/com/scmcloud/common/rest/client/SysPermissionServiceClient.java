package com.scmcloud.common.rest.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.scmcloud.common.dto.permission.ApiPermissionDTO;
import com.scmcloud.common.dto.permission.PermissionDTO;
import com.scmcloud.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 权限服务客户端（@HttpExchange 版本）
 *
 * <p>架构说明：
 * <ul>
 *   <li>主要通信：Dubbo (PermissionDubboService) - 高性能 RPC</li>
 *   <li>降级备用：RestClient + @HttpExchange (SysPermissionServiceClient) - HTTP REST</li>
 * </ul>
 *
 * <p>对应 system-service 的 SysPermissionController 端点
 *
 * <p><strong>安全特性：Fail-Closed 模式</strong>
 * <ul>
 *   <li>权限查询失败时，默认拒绝访问（而不是放行）</li>
 *   <li>{@code findPermissionsByUrl} 失败时抛出 AccessDeniedException</li>
 *   <li>{@code getUserPermissions} 失败时返回空集合（拒绝所有权限）</li>
 * </ul>
 *
 * @author Claude
 * @since 2025-12-29
 */
@HttpExchange("/api/system/permissions")
public interface SysPermissionServiceClient {

    Logger log = LoggerFactory.getLogger(SysPermissionServiceClient.class);

    /**
     * 查询权限树
     *
     * @return 权限树
     */
    @GetExchange("/tree")
    @SentinelResource(
        value = "permission-service:getTree",
        fallback = "getPermissionTreeFallback"
    )
    ApiResponse<List<PermissionDTO>> getPermissionTree();

    default ApiResponse<List<PermissionDTO>> getPermissionTreeFallback(Throwable ex) {
        return ApiResponse.success(new ArrayList<>());
    }

    /**
     * 查询用户权限
     *
     * <p><strong>SECURITY: Fail-Closed 模式</strong> - 失败时返回空集合（拒绝所有权限）</p>
     *
     * @param userId 用户 ID
     * @return 用户权限集合
     */
    @GetExchange("/user/{userId}")
    @SentinelResource(
        value = "permission-service:getUserPermissions",
        fallback = "getUserPermissionsFallback"
    )
    ApiResponse<Set<String>> getUserPermissions(@PathVariable UUID userId);

    default ApiResponse<Set<String>> getUserPermissionsFallback(UUID userId, Throwable ex) {
        log.error("SECURITY ALERT: Permission lookup failed for userId={} - DENYING ALL ACCESS. Error: {}",
                  userId, ex.getMessage());
        return ApiResponse.success(Collections.emptySet());
    }

    /**
     * 根据 ID 获取权限详情
     *
     * @param id 权限 ID
     * @return 权限详情
     */
    @GetExchange("/{id}")
    @SentinelResource(
        value = "permission-service:getById",
        fallback = "getPermissionByIdFallback"
    )
    ApiResponse<PermissionDTO> getPermissionById(@PathVariable UUID id);

    default ApiResponse<PermissionDTO> getPermissionByIdFallback(UUID id, Throwable ex) {
        return ApiResponse.fail(503, "权限服务暂时不可用");
    }

    /**
     * 根据 URL + HTTP 方法查询权限
     *
     * <p><strong>SECURITY: Fail-Closed 模式</strong> - 失败时抛出 AccessDeniedException（拒绝访问）</p>
     *
     * @param url URL 路径
     * @param method HTTP 方法
     * @return 权限编码列表
     */
    @GetExchange("/find-by-url")
    @SentinelResource(
        value = "permission-service:findByUrl",
        fallback = "findPermissionsByUrlFallback"
    )
    List<String> findPermissionsByUrl(
        @RequestParam("url") String url,
        @RequestParam("method") String method
    );

    default List<String> findPermissionsByUrlFallback(
        String url,
        String method,
        Throwable ex) {

        log.error("SECURITY ALERT: Permission lookup by URL failed - DENYING ACCESS. " +
                  "url={}, method={}, error={}",
                  url, method, ex.getMessage());

        throw new AccessDeniedException(
            "Permission service unavailable (Sentinel circuit open or error) - access denied as safety measure",
            ex
        );
    }

    /**
     * 查询所有 API 权限（供 DynamicPermissionLoader 使用）
     *
     * @return API 权限列表，包含路径、HTTP 方法和权限编码
     */
    @GetExchange("/api")
    @SentinelResource(
        value = "permission-service:getApiPermissions",
        fallback = "findApiPermissionsFallback"
    )
    List<ApiPermissionDTO> findApiPermissions();

    default List<ApiPermissionDTO> findApiPermissionsFallback(Throwable ex) {
        return Collections.emptyList();
    }
}
