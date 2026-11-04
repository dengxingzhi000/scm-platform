package com.cognora.platform.system.controller;

import com.cognora.platform.common.log.annotation.AuditLog;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.common.dto.permission.ApiPermissionDTO;
import com.cognora.platform.common.dto.permission.PermissionDTO;
import com.cognora.platform.system.service.ISysPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 权限管理控制类
 *
 * @author Deng
 * createData 2025/10/14 17:47
 * @version 1.0
 */
@RestController
@RequestMapping("/api/system/permissions")
@RequiredArgsConstructor
@Tag(name = "权限管理", description = "权限树、权限明细与用户权限的查询及维护，为网关动态鉴权提供权限数据。")
public class SysPermissionController {
    private final ISysPermissionService permissionService;

    /**
     * 查询权限树
     */
    @Operation(summary = "查询权限树", description = "查询完整的权限树形结构，用于权限分配与菜单展示。")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:permission:list')")
    public ApiResponse<List<PermissionDTO>> tree() {
        List<PermissionDTO> tree = permissionService.getPermissionTree();

        return ApiResponse.success(tree);
    }

    /**
     * 新增权限
     */
    @Operation(summary = "新增权限", description = "新增一条权限并写入高风险审计日志。")
    @PostMapping
    @PreAuthorize("hasAuthority('system:permission:add')")
    @AuditLog(
            operation = "Add permission",
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
    @Operation(summary = "修改权限", description = "根据权限 ID 更新权限信息并写入审计日志。")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:edit')")
    @AuditLog(
            operation = "修改权限",
            businessType = "PERMISSION",
            riskLevel = 4
    )
    public ApiResponse<Void> update(@Parameter(description = "权限 ID") @PathVariable UUID id,
                                   @Validated @RequestBody PermissionDTO permissionDTO) {
        permissionService.updatePermission(id, permissionDTO);

        return ApiResponse.success();
    }

    /**
     * 删除权限
     */
    @Operation(summary = "删除权限", description = "根据权限 ID 删除权限并写入审计日志。")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:delete')")
    @AuditLog(
            operation = "删除权限",
            businessType = "PERMISSION",
            riskLevel = 4
    )
    public ApiResponse<Void> delete(@Parameter(description = "权限 ID") @PathVariable UUID id) {
        permissionService.deletePermission(id);

        return ApiResponse.success();
    }

    /**
     * 根据 id查询权限
     */
    @Operation(summary = "查询单个权限", description = "根据权限 ID 查询权限详情。")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:list')")
    public ApiResponse<PermissionDTO> getById(@Parameter(description = "权限 ID") @PathVariable UUID id) {
        PermissionDTO permissionDTO = permissionService.getPermissionById(id);

        return ApiResponse.success(permissionDTO);
    }

    /**
     * 查询用户权限（用于 Feign 调用）
     * 瀵瑰簲 Dubbo: PermissionDubboService.findAllPermissionsByUserId
     */
    @Operation(summary = "查询用户权限", description = "查询指定用户拥有的全部权限标识，供其他服务通过 Feign 调用。")
    @GetMapping("/user/{userId}")
    public ApiResponse<Set<String>> getUserPermissions(@Parameter(description = "用户 ID") @PathVariable UUID userId) {
        Set<String> permissions = permissionService.getUserPermissions(userId);

        return ApiResponse.success(permissions);
    }

    /**
     * 根据 URL HTTP 方法查询权限（用于 Feign 调用）
     * 瀵瑰簲 Dubbo: PermissionDubboService.findPermissionsByUrl
     */
    @Operation(summary = "按 URL 查询权限", description = "根据请求 URL 与 HTTP 方法查询匹配的权限标识，供网关鉴权调用。")
    @GetMapping("/find-by-url")
    public List<String> findPermissionsByUrl(@RequestParam("url") String url,
                                              @Parameter(description = "HTTP 方法，如 GET、POST") @RequestParam("method") String method) {
        return permissionService.findPermissionsByUrl(url, method);
    }

    /**
     * 查询所有 API 权限（用于动态权限加载）
     * 用于 DynamicPermissionLoader 加载权限映射
     */
    @Operation(summary = "查询全部 API 权限", description = "查询全部 API 权限映射关系，用于动态权限加载。")
    @GetMapping("/api")
    public List<ApiPermissionDTO> findApiPermissions() {
        return permissionService.findApiPermissions();
    }
}
