package com.scmcloud.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.rest.client.SysAuthServiceClient;
import com.scmcloud.common.log.annotation.AuditLog;
import com.scmcloud.common.domain.PageResult;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.dto.user.ChangePasswordRequest;
import com.scmcloud.common.dto.role.TemporaryRoleGrantDTO;
import com.scmcloud.common.dto.user.UserDTO;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.system.service.command.ISysUserCommandService;
import com.scmcloud.system.service.command.IUserRoleCommandService;
import com.scmcloud.system.service.query.ISysUserQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户管理控制器
 *
 * @author Deng
 * createData 2025/10/14 18:00
 * @version 1.0
 */
@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户的分页查询、增删改、密码管理、角色授权与临时角色管理。")
public class SysUserController {
    private final ISysUserQueryService userQueryService;
    private final ISysUserCommandService userCommandService;
    private final IUserRoleCommandService userRoleCommandService;
    private final SysAuthServiceClient authServiceClient;

    /**
     * 查询用户列表
     */
    @Operation(summary = "分页查询用户列表", description = "分页查询用户列表，支持按用户名和用户状态筛选。")
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public ApiResponse<PageResult<UserDTO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @RequestParam(required = false) String username,
                                                 @Parameter(description = "用户状态，用于按状态筛选") @RequestParam(required = false) Integer status) {
        Page<UserDTO> result = userQueryService.listUsers(page, size, username, status);

        return ApiResponse.success(PageResult.of(result));
    }

    /**
     * 查询用户详情
     */
    @Operation(summary = "查询用户详情", description = "根据用户 ID 查询用户的详细信息。")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public ApiResponse<UserDTO> getById(@Parameter(description = "用户 ID") @PathVariable UUID id) {
        UserDTO user = userQueryService.getUserById(id);

        return ApiResponse.success(user);
    }

    /**
     * 新增用户
     */
    @Operation(summary = "新增用户", description = "新增一个用户并写入审计日志。")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    @AuditLog(
            operation = "新增用户",
            businessType = "USER"
    )
    public ApiResponse<Void> add(@Validated @RequestBody UserDTO userDTO) {
        userCommandService.addUser(userDTO);

        return ApiResponse.success();
    }

    /**
     * 修改用户
     */
    @Operation(summary = "修改用户", description = "根据用户 ID 更新用户信息并写入审计日志。")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @AuditLog(
            operation = "修改用户",
            businessType = "USER"
    )
    public ApiResponse<Void> update(@Parameter(description = "用户 ID") @PathVariable UUID id, @Validated @RequestBody UserDTO userDTO) {
        userDTO.setId(id);
        userCommandService.updateUser(userDTO);

        return ApiResponse.success();
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "根据用户 ID 删除用户并写入审计日志。")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @AuditLog(
            operation = "删除用户",
            businessType = "USER",
            riskLevel = 4
    )
    public ApiResponse<Void> delete(@Parameter(description = "用户 ID") @PathVariable UUID id) {
        userCommandService.deleteUser(id);

        return ApiResponse.success();
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码", description = "当前登录用户修改自己的登录密码。")
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Validated @RequestBody ChangePasswordRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid().orElse(null);
        userCommandService.changePassword(userId, request.getOldPassword(), request.getNewPassword());

        return ApiResponse.success();
    }

    /**
     * 重置密码
     */
    @Operation(summary = "重置密码", description = "根据用户 ID 重置密码并返回新的初始密码。")
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('system:user:reset')")
    @AuditLog(
            operation = "重置密码",
            businessType = "USER",
            riskLevel = 3
    )
    public ApiResponse<String> resetPassword(@Parameter(description = "用户 ID") @PathVariable UUID id) {
        String newPassword = userCommandService.resetPassword(id);

        return ApiResponse.success(newPassword);
    }

    /**
     * 授权角色
     */
    @Operation(summary = "授予角色", description = "为指定用户重新绑定角色，原有角色关系会被清除后写入。")
    @PostMapping("/{id}/grant-roles")
    @PreAuthorize("hasAuthority('system:user:grant')")
    @AuditLog(
            operation = "授权角色",
            businessType = "USER",
            riskLevel = 4
    )
    public ApiResponse<Void> grantRoles(@Parameter(description = "用户 ID") @PathVariable UUID id, @RequestBody List<UUID> roleIds) {
        userCommandService.grantRoles(id, roleIds);

        return ApiResponse.success();
    }

    /**
     * 锁定/解锁用户
     */
    @Operation(summary = "锁定或解锁用户", description = "根据用户 ID 锁定或解锁用户账号，锁定操作会设置 24 小时锁定截止时间。")
    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @AuditLog(
            operation = "锁定用户",
            businessType = "USER",
            riskLevel = 3
    )
    public ApiResponse<Void> lockUser(@Parameter(description = "用户 ID") @PathVariable UUID id,
                                      @Parameter(description = "是否锁定，true 表示锁定，false 表示解锁") @RequestParam Boolean lock) {
        userCommandService.lockUser(id, lock);

        return ApiResponse.success();
    }

    /**
     * 强制用户下线
     */
    @Operation(summary = "强制用户下线", description = "调用认证服务强制指定用户下线，并记录下线原因。")
    @PostMapping("/{id}/force-logout")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @AuditLog(
            operation = "强制下线",
            businessType = "USER",
            riskLevel = 3
    )
    public ApiResponse<Void> forceLogout(@Parameter(description = "用户 ID") @PathVariable UUID id,
                                         @Parameter(description = "强制下线原因") @RequestParam String reason) {
        authServiceClient.forceLogout(id, reason);

        return ApiResponse.success();
    }

    /**
     * 授予临时角色
     */
    @Operation(summary = "授予临时角色", description = "为用户授予在指定生效时间到失效时间之间有效的临时角色。")
    @PostMapping("/{id}/grant-temporary-roles")
    @PreAuthorize("hasAuthority('system:user:grant')")
    @AuditLog(
            operation = "授予临时角色",
            businessType = "USER",
            riskLevel = 4
    )
    public ApiResponse<Void> grantTemporaryRoles(@Parameter(description = "用户 ID") @PathVariable UUID id,
                                                 @RequestBody @Validated TemporaryRoleGrantDTO dto) {
        userRoleCommandService.grantTemporaryRoles(
                id,
                dto.getRoleIds(),
                dto.getEffectiveTime(),
                dto.getExpireTime()
        );

        return ApiResponse.success();
    }

    /**
     * 延长临时角色有效
     */
    @Operation(summary = "延长临时角色有效期", description = "更新用户某个临时角色的失效时间，延长其有效期。")
    @PostMapping("/{userId}/extend-temporary-role/{roleId}")
    @PreAuthorize("hasAuthority('system:user:grant')")
    @AuditLog(
            operation = "延长临时角色",
            businessType = "USER",
            riskLevel = 3
    )
    public ApiResponse<Void> extendTemporaryRole(
            @Parameter(description = "用户 ID") @PathVariable UUID userId,
            @Parameter(description = "角色 ID") @PathVariable UUID roleId,
            @Parameter(description = "新的失效时间，格式 yyyy-MM-dd HH:mm:ss") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime newExpireTime) {
        userRoleCommandService.extendTemporaryRole(userId, roleId, newExpireTime);

        return ApiResponse.success();
    }

    /**
     * 终止临时角色
     */
    @Operation(summary = "终止临时角色", description = "提前终止用户某个临时角色的授权。")
    @PostMapping("/{userId}/terminate-temporary-role/{roleId}")
    @PreAuthorize("hasAuthority('system:user:grant')")
    @AuditLog(
            operation = "终止临时角色",
            businessType = "USER",
            riskLevel = 3
    )
    public ApiResponse<Void> terminateTemporaryRole(@Parameter(description = "用户 ID") @PathVariable UUID userId,
                                                    @Parameter(description = "角色 ID") @PathVariable UUID roleId) {
        userRoleCommandService.terminateTemporaryRole(userId, roleId);

        return ApiResponse.success();
    }

    /**
     * 查询用户的临时角色列
     */
    @Operation(summary = "查询临时角色列表", description = "查询指定用户当前持有的临时角色列表。")
    @GetMapping("/{id}/temporary-roles")
    @PreAuthorize("hasAuthority('system:user:list')")
    public ApiResponse<List<Map<String, Object>>> getUserTemporaryRoles(@Parameter(description = "用户 ID") @PathVariable UUID id) {
        List<Map<String, Object>> roles = userQueryService.getUserTemporaryRoles(id);

        return ApiResponse.success(roles);
    }

    /**
     * 查询用户统计信息
     */
    @Operation(summary = "查询用户统计信息", description = "查询指定用户的统计数据。")
    @GetMapping("/{id}/statistics")
    @PreAuthorize("hasAuthority('system:user:list')")
    public ApiResponse<Map<String, Object>> getUserStatistics(@Parameter(description = "用户 ID") @PathVariable UUID id) {
        Map<String, Object> stats = userQueryService.getUserStatistics(id);

        return ApiResponse.success(stats);
    }

    /**
     * 更新最后登录信
     */
    @Operation(summary = "更新最后登录信息", description = "更新指定用户的最后登录时间与登录 IP。")
    @GetMapping("/{userId}/update-login")
    public ApiResponse<Void> updateLastLogin(@Parameter(description = "用户 ID") @PathVariable UUID userId,
                                             @Parameter(description = "登录 IP 地址") @RequestParam("ipAddress") String ipAddress) {
        userCommandService.updateLastLogin(userId, ipAddress, LocalDateTime.now());

        return ApiResponse.success();
    }
}
