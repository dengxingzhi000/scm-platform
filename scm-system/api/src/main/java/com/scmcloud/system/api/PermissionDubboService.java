package com.scmcloud.system.api;

import com.scmcloud.common.dto.permission.ApiPermissionDTO;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Dubbo API for high-frequency permission checks.
 * Used by gateway/auth to perform fast in-memory/RPC permission queries.
 */
public interface PermissionDubboService {

    /**
     * Find required permission codes by API url and HTTP method.
     */
    List<String> findPermissionsByUrl(String url, String method);

    /**
     * Get all effective permission codes for a user.
     */
    Set<String> findAllPermissionsByUserId(UUID userId);

    /**
     * Find all API permissions (path + HTTP method + permission code).
     * Used by DynamicPermissionLoader for permission cache loading.
     */
    List<ApiPermissionDTO> findApiPermissions();
}

