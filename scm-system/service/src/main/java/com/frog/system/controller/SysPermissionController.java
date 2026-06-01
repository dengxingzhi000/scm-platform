package com.frog.system.controller;

import com.frog.common.log.annotation.AuditLog;
import com.frog.common.response.ApiResponse;
import com.frog.common.dto.permission.ApiPermissionDTO;
import com.frog.common.dto.permission.PermissionDTO;
import com.frog.system.service.ISysPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 权限管理控制�?
 *
 * @author Deng
 * createData 2025/10/14 17:47
 * @version 1.0
 */
@RestController
@RequestMapping("/api/system/permissions")
@RequiredArgsConstructor
public class SysPermissionController {
    private final ISysPermissionService permissionService;

    /**
     * 查询权限�?
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:permission:list')")
    public ApiResponse<List<PermissionDTO>> tree() {
        List<PermissionDTO> tree = permissionService.getPermissionTree();

        return ApiResponse.success(tree);
    }

    /**
     * 新增权限
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:permission:add')")
    @AuditLog(
            operation = "新增权限",
            businessType = "PERMISSION",
            riskLevel = 4
    )
    public ApiResponse<Void> add(@Validated @RequestBody PermissionDTO permissionDTO) {
        permissionService.addPermission(permissionDTO);

        return ApiResponse.success();
    }

    /**
     * 修改权限
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:edit')")
    @AuditLog(
            operation = "修改权限",
            businessType = "PERMISSION",
            riskLevel = 4
    )
    public ApiResponse<Void> update(@PathVariable UUID id,
                                   @Validated @RequestBody PermissionDTO permissionDTO) {
        permissionDTO.setId(id);
        permissionService.updatePermission(permissionDTO);

        return ApiResponse.success();
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:delete')")
    @AuditLog(
            operation = "删除权限",
            businessType = "PERMISSION",
            riskLevel = 4
    )
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        permissionService.deletePermission(id);

        return ApiResponse.success();
    }

    /**
     * 根据 id查询权限
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:list')")
    public ApiResponse<PermissionDTO> getById(@PathVariable UUID id) {
        PermissionDTO permissionDTO = permissionService.getPermissionById(id);

        return ApiResponse.success(permissionDTO);
    }

    /**
     * 查询用户权限（用�?Feign 调用�?
     * 对应 Dubbo: PermissionDubboService.findAllPermissionsByUserId
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<Set<String>> getUserPermissions(@PathVariable UUID userId) {
        Set<String> permissions = permissionService.getUserPermissions(userId);

        return ApiResponse.success(permissions);
    }

    /**
     * 根据 URL �?HTTP 方法查询权限（用�?Feign 调用�?
     * 对应 Dubbo: PermissionDubboService.findPermissionsByUrl
     */
    @GetMapping("/find-by-url")
    public List<String> findPermissionsByUrl(@RequestParam("url") String url,
                                              @RequestParam("method") String method) {
        return permissionService.findPermissionsByUrl(url, method);
    }

    /**
     * 查询所�?API 权限（用于动态权限加载）
     * 用于 DynamicPermissionLoader 加载权限映射
     */
    @GetMapping("/api")
    public List<ApiPermissionDTO> findApiPermissions() {
        return permissionService.findApiPermissions();
    }
}
