package com.scmcloud.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter extends OncePerRequestFilter {

    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_TENANT_ID_ALT = "Tenant-Id";
    private static final String PARAM_TENANT_ID = "tenantId";

    private final TenantProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TenantFilter(TenantProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        List<String> excludePaths = properties.excludePaths();
        String path = request.getRequestURI();
        return excludePaths.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            UUID tenantId = extractTenantId(request);
            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
                log.debug("Tenant filter set tenant ID: {} for {}",
                        tenantId, request.getRequestURI());
            } else if (properties.required()) {
                throw new TenantParseException("Tenant ID is required but missing in request: "
                        + request.getRequestURI());
            } else {
                log.debug("Tenant ID not present and not required: {}",
                        request.getRequestURI());
            }
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private UUID extractTenantId(HttpServletRequest request) {
        String value = request.getHeader(HEADER_TENANT_ID);
        if (value == null || value.isBlank()) {
            value = request.getHeader(HEADER_TENANT_ID_ALT);
        }
        if (value == null || value.isBlank()) {
            value = request.getParameter(PARAM_TENANT_ID);
        }
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid tenant ID format: " + value, e);
        }
    }
}
