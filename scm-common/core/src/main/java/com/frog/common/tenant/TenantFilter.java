package com.frog.common.tenant;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 租户过滤器
 * 从请求头中提取租户ID，设置到 ThreadLocal
 *
 * 支持的租户ID来源（优先级从高到低）：
 * 1. HTTP Header: X-Tenant-Id
 * 2. HTTP Header: Tenant-Id
 * 3. Request Parameter: tenantId
 * 4. JWT Token 中的 tenant_id claim（需配合JWT解析）
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter implements Filter {

    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_TENANT_ID_ALT = "Tenant-Id";
    private static final String PARAM_TENANT_ID = "tenantId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // 提取租户ID
            UUID tenantId = extractTenantId(httpRequest);

            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
                log.debug("Tenant filter set tenant ID: {} for request: {}",
                        tenantId, httpRequest.getRequestURI());
            } else {
                log.warn("No tenant ID found in request: {}", httpRequest.getRequestURI());
                // 可以选择抛异常或允许继续（根据业务需求）
                // throw new TenantContextHolder.TenantNotFoundException("Tenant ID is required");
            }

            // 继续执行
            chain.doFilter(request, response);
        } finally {
            // 清理 ThreadLocal，避免内存泄漏
            TenantContextHolder.clear();
        }
    }

    /**
     * 从请求中提取租户ID
     *
     * 优先级：
     * 1. X-Tenant-Id header
     * 2. Tenant-Id header
     * 3. tenantId parameter
     * 4. JWT token（如果已配置）
     */
    private UUID extractTenantId(HttpServletRequest request) {
        // 1. 从 X-Tenant-Id header
        String tenantIdStr = request.getHeader(HEADER_TENANT_ID);

        // 2. 从 Tenant-Id header
        if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
            tenantIdStr = request.getHeader(HEADER_TENANT_ID_ALT);
        }

        // 3. 从请求参数
        if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
            tenantIdStr = request.getParameter(PARAM_TENANT_ID);
        }

        // 4. TODO: 从 JWT Token 中提取（需要配合 JwtAuthenticationFilter）
        // if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
        //     tenantIdStr = extractFromJwtToken(request);
        // }

        // 转换为UUID
        if (tenantIdStr != null && !tenantIdStr.trim().isEmpty()) {
            try {
                return UUID.fromString(tenantIdStr.trim());
            } catch (IllegalArgumentException e) {
                log.error("Invalid tenant ID format: {}", tenantIdStr, e);
                throw new IllegalArgumentException("Invalid tenant ID format: " + tenantIdStr);
            }
        }

        return null;
    }

    /**
     * 从JWT Token中提取租户ID（示例）
     * 需要配合实际的JWT解析逻辑
     */
    // private String extractFromJwtToken(HttpServletRequest request) {
    //     String token = request.getHeader("Authorization");
    //     if (token != null && token.startsWith("Bearer ")) {
    //         String jwt = token.substring(7);
    //         // TODO: 解析JWT，获取 tenant_id claim
    //         return JwtUtil.getClaim(jwt, "tenant_id");
    //     }
    //     return null;
    // }
}