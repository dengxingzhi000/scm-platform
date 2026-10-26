package com.scmcloud.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.domain.PageResult;
import com.scmcloud.common.log.annotation.AuditLog;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.dto.role.RoleDTO;
import com.scmcloud.system.service.ISysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 瑙掕壊绠$悊鎺у埗锟?
 *
 * @author Deng
 * createData 2025/10/14 18:01
 * @version 1.0
 */
@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "角色的查询与维护，以及角色权限的授予和查询。")
public class SysRoleController {
    private final ISysRoleService roleService;

    /**
     * 鏌ヨ瑙掕壊鍒楄〃
     */
    @Operation(summary = "分页查询角色列表", description = "分页查询角色列表，支持按角色名称模糊筛选。")
    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public ApiResponse<PageResult<RoleDTO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @RequestParam(required = false) String roleName) {
        Page<RoleDTO> result = roleService.listRoles(page, size, roleName);

        return ApiResponse.success(PageResult.of(result));
    }

    /**
     * 鏌ヨ鎵€鏈夎鑹诧紙鐢ㄤ簬涓嬫媺閫夋嫨锟?
     */
    @Operation(summary = "查询全部角色", description = "查询当前租户下的全部角色，用于下拉选择。")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('system:role:list')")
    public ApiResponse<List<RoleDTO>> listAll() {
        List<RoleDTO> roles = roleService.listAllRoles();

        return ApiResponse.success(roles);
    }

    /**
     * 鏂板瑙掕壊
     */
    @Operation(summary = "新增角色", description = "新增一个角色并写入审计日志。")
    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    @AuditLog(
            operation = "Add role",
            businessType = "ROLE",
            riskLevel = 4
    )
    public ApiResponse<Void> add(@Validated @RequestBody RoleDTO roleDTO) {
        roleService.addRole(roleDTO);

        return ApiResponse.success();
    }

    /**
     * 淇敼瑙掕壊
     */
    @Operation(summary = "修改角色", description = "根据角色 ID 更新角色信息并写入审计日志。")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @AuditLog(
            operation = "淇敼瑙掕壊",
            businessType = "ROLE",
            riskLevel = 4
    )
    public ApiResponse<Void> update(@Parameter(description = "角色 ID") @PathVariable UUID id, @Validated @RequestBody RoleDTO roleDTO) {
        roleDTO.setId(id);
        roleService.updateRole(roleDTO);

        return ApiResponse.success();
    }

    /**
     * 鍒犻櫎瑙掕壊
     */
    @Operation(summary = "删除角色", description = "根据角色 ID 删除角色并写入审计日志。")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    @AuditLog(
            operation = "鍒犻櫎瑙掕壊",
            businessType = "ROLE",
            riskLevel = 4
    )
    public ApiResponse<Void> delete(@Parameter(description = "角色 ID") @PathVariable UUID id) {
        roleService.deleteRole(id);

        return ApiResponse.success();
    }

    /**
     * 鎺堟潈鏉冮檺
     */
    @Operation(summary = "角色授权", description = "为指定角色重新绑定权限，原有权限关系会被覆盖。")
    @PostMapping("/{id}/grant-permissions")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @AuditLog(
            operation = "瑙掕壊鎺堟潈",
            businessType = "ROLE",
            riskLevel = 4
    )
    public ApiResponse<Void> grantPermissions(@Parameter(description = "角色 ID") @PathVariable UUID id, @RequestBody List<UUID> permissionIds) {
        roleService.grantPermissions(id, permissionIds);

        return ApiResponse.success();
    }

    /**
     * 鏌ヨ瑙掕壊鏉冮檺
     */
    @Operation(summary = "查询角色权限", description = "查询指定角色已绑定的权限 ID 列表。")
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:list')")
    public ApiResponse<List<UUID>> getRolePermissions(@Parameter(description = "角色 ID") @PathVariable UUID id) {
        List<UUID> permissionIds = roleService.getRolePermissionIds(id);

        return ApiResponse.success(permissionIds);
    }
}

