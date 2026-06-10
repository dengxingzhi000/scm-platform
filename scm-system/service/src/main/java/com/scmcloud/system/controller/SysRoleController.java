package com.scmcloud.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.domain.PageResult;
import com.scmcloud.common.log.annotation.AuditLog;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.dto.role.RoleDTO;
import com.scmcloud.system.service.ISysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 瑙掕壊绠＄悊鎺у埗锟?
 *
 * @author Deng
 * createData 2025/10/14 18:01
 * @version 1.0
 */
@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class SysRoleController {
    private final ISysRoleService roleService;

    /**
     * 鏌ヨ瑙掕壊鍒楄〃
     */
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
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('system:role:list')")
    public ApiResponse<List<RoleDTO>> listAll() {
        List<RoleDTO> roles = roleService.listAllRoles();

        return ApiResponse.success(roles);
    }

    /**
     * 鏂板瑙掕壊
     */
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
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @AuditLog(
            operation = "淇敼瑙掕壊",
            businessType = "ROLE",
            riskLevel = 4
    )
    public ApiResponse<Void> update(@PathVariable UUID id, @Validated @RequestBody RoleDTO roleDTO) {
        roleDTO.setId(id);
        roleService.updateRole(roleDTO);

        return ApiResponse.success();
    }

    /**
     * 鍒犻櫎瑙掕壊
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    @AuditLog(
            operation = "鍒犻櫎瑙掕壊",
            businessType = "ROLE",
            riskLevel = 4
    )
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        roleService.deleteRole(id);

        return ApiResponse.success();
    }

    /**
     * 鎺堟潈鏉冮檺
     */
    @PostMapping("/{id}/grant-permissions")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @AuditLog(
            operation = "瑙掕壊鎺堟潈",
            businessType = "ROLE",
            riskLevel = 4
    )
    public ApiResponse<Void> grantPermissions(@PathVariable UUID id, @RequestBody List<UUID> permissionIds) {
        roleService.grantPermissions(id, permissionIds);

        return ApiResponse.success();
    }

    /**
     * 鏌ヨ瑙掕壊鏉冮檺
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:list')")
    public ApiResponse<List<UUID>> getRolePermissions(@PathVariable UUID id) {
        List<UUID> permissionIds = roleService.getRolePermissionIds(id);

        return ApiResponse.success(permissionIds);
    }
}

