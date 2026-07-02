package com.scmcloud.system.service.dubbo;

import com.scmcloud.common.dto.permission.ApiPermissionDTO;
import com.scmcloud.system.api.PermissionDubboService;
import com.scmcloud.system.service.ISysPermissionService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@DubboService
@Component
public class PermissionDubboServiceImpl implements PermissionDubboService {
    private final ISysPermissionService permissionService;

    @Override
    public List<String> findPermissionsByUrl(String url, String method) {
        return permissionService.findPermissionsByUrl(url, method);
    }

    @Override
    public Set<String> findAllPermissionsByUserId(UUID userId) {
        return permissionService.getUserPermissions(userId);
    }

    @Override
    public List<ApiPermissionDTO> findApiPermissions() {
        return permissionService.findApiPermissions();
    }
}

