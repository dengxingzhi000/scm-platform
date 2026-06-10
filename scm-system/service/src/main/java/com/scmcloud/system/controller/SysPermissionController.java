package com.scmcloud.system.controller;

import com.scmcloud.common.log.annotation.AuditLog;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.dto.permission.ApiPermissionDTO;
import com.scmcloud.common.dto.permission.PermissionDTO;
import com.scmcloud.system.service.ISysPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 鏉冮檺绠＄悊鎺у埗锟?
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
     * 鏌ヨ鏉冮檺锟?
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:permission:list')")
    public ApiResponse<List<PermissionDTO>> tree() {
        List<PermissionDTO> tree = permissionService.getPermissionTree();

        return ApiResponse.success(tree);
    }

    /**
     * 鏂板鏉冮檺
     */
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
     * 淇敼鏉冮檺
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:edit')")
    @AuditLog(
            operation = "淇敼鏉冮檺",
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
     * 鍒犻櫎鏉冮檺
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:delete')")
    @AuditLog(
            operation = "鍒犻櫎鏉冮檺",
            businessType = "PERMISSION",
            riskLevel = 4
    )
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        permissionService.deletePermission(id);

        return ApiResponse.success();
    }

    /**
     * 鏍规嵁 id鏌ヨ鏉冮檺
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:list')")
    public ApiResponse<PermissionDTO> getById(@PathVariable UUID id) {
        PermissionDTO permissionDTO = permissionService.getPermissionById(id);

        return ApiResponse.success(permissionDTO);
    }

    /**
     * 鏌ヨ鐢ㄦ埛鏉冮檺锛堢敤锟紽eign 璋冪敤锟?
     * 瀵瑰簲 Dubbo: PermissionDubboService.findAllPermissionsByUserId
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<Set<String>> getUserPermissions(@PathVariable UUID userId) {
        Set<String> permissions = permissionService.getUserPermissions(userId);

        return ApiResponse.success(permissions);
    }

    /**
     * 鏍规嵁 URL 锟紿TTP 鏂规硶鏌ヨ鏉冮檺锛堢敤锟紽eign 璋冪敤锟?
     * 瀵瑰簲 Dubbo: PermissionDubboService.findPermissionsByUrl
     */
    @GetMapping("/find-by-url")
    public List<String> findPermissionsByUrl(@RequestParam("url") String url,
                                              @RequestParam("method") String method) {
        return permissionService.findPermissionsByUrl(url, method);
    }

    /**
     * 鏌ヨ鎵€锟紸PI 鏉冮檺锛堢敤浜庡姩鎬佹潈闄愬姞杞斤級
     * 鐢ㄤ簬 DynamicPermissionLoader 鍔犺浇鏉冮檺鏄犲皠
     */
    @GetMapping("/api")
    public List<ApiPermissionDTO> findApiPermissions() {
        return permissionService.findApiPermissions();
    }
}
