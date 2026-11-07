package com.cognora.platform.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.log.annotation.AuditLog;
import com.cognora.platform.common.domain.PageResult;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.common.dto.user.BatchOperationResult;
import com.cognora.platform.common.dto.user.BatchUserRequest;
import com.cognora.platform.common.dto.user.ChangePasswordRequest;
import com.cognora.platform.common.dto.role.TemporaryRoleGrantDTO;
import com.cognora.platform.common.dto.user.CreateUserRequest;
import com.cognora.platform.common.dto.user.DisableUserRequest;
import com.cognora.platform.common.dto.user.LockUserRequest;
import com.cognora.platform.common.dto.user.LoginHistoryDTO;
import com.cognora.platform.common.dto.user.ResetPasswordResponse;
import com.cognora.platform.common.dto.user.UpdateProfileRequest;
import com.cognora.platform.common.dto.user.UpdateUserRequest;
import com.cognora.platform.common.dto.user.UserDTO;
import com.cognora.platform.common.dto.user.UserDetailDTO;
import com.cognora.platform.common.dto.user.UserImportResult;
import com.cognora.platform.common.dto.user.UserQueryRequest;
import com.cognora.platform.common.dto.user.UserSessionDTO;
import com.cognora.platform.common.dto.user.UserSummaryDTO;
import com.cognora.platform.common.security.PermissionChecker;
import com.cognora.platform.common.web.util.SecurityUtils;
import com.cognora.platform.system.service.command.BatchUserCommandService;
import com.cognora.platform.system.service.command.ISysUserCommandService;
import com.cognora.platform.system.service.command.IUserRoleCommandService;
import com.cognora.platform.system.service.query.ISysUserQueryService;
import com.cognora.platform.system.service.query.UserLoginHistoryQueryService;
import com.cognora.platform.system.service.support.UserImportExportService;
import com.cognora.platform.system.service.support.UserReadAccessGuard;
import com.cognora.platform.system.service.support.UserSensitiveMasker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final BatchUserCommandService batchUserCommandService;
    private final UserLoginHistoryQueryService loginHistoryQueryService;
    private final UserImportExportService userImportExportService;
    private final UserReadAccessGuard userReadAccessGuard;
    private final PermissionChecker permissionChecker;

    /**
     * 查询用户列表
     */
    @Operation(summary = "分页查询用户列表", description = "支持用户名、姓名、手机、邮箱、部门、角色、状态与创建时间范围筛选。")
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public ApiResponse<PageResult<UserSummaryDTO>> list(@Valid @ModelAttribute UserQueryRequest request) {
        Page<UserSummaryDTO> result = userQueryService.listUsers(request);

        return ApiResponse.success(PageResult.of(result));
    }

    @Operation(summary = "批量启用用户", description = "逐行返回成功/失败，单行失败不影响其他行。")
    @PostMapping("/batch/enable")
    @PreAuthorize("hasAuthority('system:user:batch:enable')")
    @AuditLog(operation = "批量启用用户", businessType = "USER", riskLevel = 3)
    public ApiResponse<BatchOperationResult> batchEnable(@Valid @RequestBody BatchUserRequest request) {
        return ApiResponse.success(batchUserCommandService.enable(request));
    }

    @Operation(summary = "批量禁用用户", description = "逐行返回成功/失败，单行失败不影响其他行。")
    @PostMapping("/batch/disable")
    @PreAuthorize("hasAuthority('system:user:batch:disable')")
    @AuditLog(operation = "批量禁用用户", businessType = "USER", riskLevel = 3)
    public ApiResponse<BatchOperationResult> batchDisable(@Valid @RequestBody BatchUserRequest request) {
        return ApiResponse.success(batchUserCommandService.disable(request));
    }

    @Operation(summary = "批量删除用户", description = "逐行返回成功/失败，不能删除自己或内置管理员。")
    @PostMapping("/batch/delete")
    @PreAuthorize("hasAuthority('system:user:batch:delete')")
    @AuditLog(operation = "批量删除用户", businessType = "USER", riskLevel = 4)
    public ApiResponse<BatchOperationResult> batchDelete(@Valid @RequestBody BatchUserRequest request) {
        return ApiResponse.success(batchUserCommandService.delete(request));
    }

    @Operation(summary = "批量调整部门", description = "逐行返回成功/失败，单行失败不影响其他行。")
    @PostMapping("/batch/transfer-dept")
    @PreAuthorize("hasAuthority('system:user:batch:transfer')")
    @AuditLog(operation = "批量调整部门", businessType = "USER", riskLevel = 3)
    public ApiResponse<BatchOperationResult> batchTransferDept(@Valid @RequestBody BatchUserRequest request) {
        return ApiResponse.success(batchUserCommandService.transferDept(request));
    }

    @Operation(summary = "批量授予角色", description = "逐行返回成功/失败，单行失败不影响其他行。")
    @PostMapping("/batch/grant-roles")
    @PreAuthorize("hasAuthority('system:user:batch:grant')")
    @AuditLog(operation = "批量授予角色", businessType = "USER", riskLevel = 4)
    public ApiResponse<BatchOperationResult> batchGrantRoles(@Valid @RequestBody BatchUserRequest request) {
        return ApiResponse.success(batchUserCommandService.grantRoles(request));
    }

    @Operation(summary = "下载用户导入模板", description = "UTF-8 BOM CSV 模板。")
    @GetMapping(value = "/import/template", produces = "text/csv")
    @PreAuthorize("hasAuthority('system:user:import')")
    public ResponseEntity<byte[]> importTemplate() {
        byte[] body = userImportExportService.template().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return csvResponse(body, "user-import-template.csv");
    }

    @Operation(summary = "CSV 导入用户", description = "单次最多500行、5MB；逐行返回失败原因。")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('system:user:import')")
    @AuditLog(operation = "导入用户", businessType = "USER", riskLevel = 3)
    public ApiResponse<UserImportResult> importUsers(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return ApiResponse.success(userImportExportService.importUsers(file, dryRun));
    }

    @Operation(summary = "导出用户 CSV", description = "最多10000行；默认脱敏，持有导出敏感权限时可导出完整值。")
    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasAuthority('system:user:export')")
    @AuditLog(operation = "导出用户", businessType = "USER", riskLevel = 2)
    public ResponseEntity<byte[]> exportUsers(@Valid @ModelAttribute UserQueryRequest request,
                                              @RequestParam(defaultValue = "false") boolean includeSensitive) {
        byte[] body = userImportExportService.export(request, includeSensitive);
        return csvResponse(body, "users.csv");
    }

    /**
     * 查询用户详情
     */
    @Operation(summary = "查询用户详情", description = "根据用户 ID 查询用户的详细信息；无 system:user:view:sensitive 权限时敏感字段脱敏。")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:detail')")
    public ApiResponse<UserDetailDTO> getById(@Parameter(description = "用户 ID") @PathVariable UUID id) {
        userReadAccessGuard.requireReadable(id);
        UserDetailDTO detail = userQueryService.getUserDetail(id);
        maskIfNeeded(detail);
        return ApiResponse.success(detail);
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
    public ApiResponse<Void> add(@Valid @RequestBody CreateUserRequest request) {
        userCommandService.addUser(request);

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
    public ApiResponse<UserDetailDTO> update(@Parameter(description = "用户 ID") @PathVariable UUID id,
                                             @Valid @RequestBody UpdateUserRequest request) {
        userCommandService.updateUser(id, request);

        UserDetailDTO detail = userQueryService.getUserDetail(id);
        maskIfNeeded(detail);
        return ApiResponse.success(detail);
    }

    /**
     * 修改当前登录用户自己的资料
     */
    @Operation(summary = "修改个人资料", description = "当前登录用户修改昵称、头像、手机、邮箱与时区，用户身份只从访问令牌解析。")
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(
            operation = "修改个人资料",
            businessType = "USER"
    )
    public ApiResponse<UserDTO> updateOwnProfile(@Validated @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userCommandService.updateOwnProfile(request));
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
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userCommandService.changePassword(request.getOldPassword(), request.getNewPassword());

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
            riskLevel = 4
    )
    public ApiResponse<ResetPasswordResponse> resetPassword(@Parameter(description = "用户 ID") @PathVariable UUID id) {
        ResetPasswordResponse response = userCommandService.resetPassword(id);

        return ApiResponse.success(response);
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

    @Operation(summary = "启用用户", description = "将禁用/锁定账号恢复为启用，并清空锁定信息与登录失败次数。")
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('system:user:enable')")
    @AuditLog(operation = "启用用户", businessType = "USER", riskLevel = 3)
    public ApiResponse<Void> enableUser(@Parameter(description = "用户 ID") @PathVariable UUID id) {
        userCommandService.enableUser(id);
        return ApiResponse.success();
    }

    @Operation(summary = "禁用用户", description = "禁用账号并强制下线。")
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('system:user:disable')")
    @AuditLog(operation = "禁用用户", businessType = "USER", riskLevel = 3)
    public ApiResponse<Void> disableUser(@Parameter(description = "用户 ID") @PathVariable UUID id,
                                         @Valid @RequestBody(required = false) DisableUserRequest request) {
        userCommandService.disableUser(id, request != null ? request.getReason() : null);
        return ApiResponse.success();
    }

    @Operation(summary = "锁定用户", description = "按指定时长锁定账号，并记录锁定原因。")
    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('system:user:lock')")
    @AuditLog(operation = "锁定用户", businessType = "USER", riskLevel = 3)
    public ApiResponse<Void> lockUser(@Parameter(description = "用户 ID") @PathVariable UUID id,
                                      @Valid @RequestBody LockUserRequest request) {
        userCommandService.lockUser(id, request.getReason(), request.getDurationMinutes());
        return ApiResponse.success();
    }

    @Operation(summary = "解锁用户", description = "解除账号锁定并清空登录失败次数。")
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAuthority('system:user:unlock')")
    @AuditLog(operation = "解锁用户", businessType = "USER", riskLevel = 3)
    public ApiResponse<Void> unlockUser(@Parameter(description = "用户 ID") @PathVariable UUID id) {
        userCommandService.unlockUser(id);
        return ApiResponse.success();
    }

    @Operation(summary = "查询用户登录历史", description = "来源审计库，默认最近30天，最大180天。")
    @GetMapping("/{id}/login-history")
    @PreAuthorize("hasAuthority('system:user:login-history')")
    public ApiResponse<PageResult<LoginHistoryDTO>> loginHistory(
            @Parameter(description = "用户 ID") @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String operationType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        userReadAccessGuard.requireReadable(id);
        return ApiResponse.success(loginHistoryQueryService.query(
                id, startTime, endTime, operationType, page, size));
    }

    @Operation(summary = "查询用户会话", description = "代理认证服务，列出指定用户的有效会话。")
    @GetMapping("/{id}/sessions")
    @PreAuthorize("hasAuthority('system:user:session:list')")
    public ApiResponse<List<UserSessionDTO>> listSessions(@Parameter(description = "用户 ID") @PathVariable UUID id) {
        return ApiResponse.success(userCommandService.listUserSessions(id));
    }

    @Operation(summary = "撤销用户会话", description = "撤销指定用户单个设备的会话。")
    @DeleteMapping("/{id}/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('system:user:session:revoke')")
    @AuditLog(operation = "撤销用户会话", businessType = "USER", riskLevel = 3)
    public ApiResponse<Integer> revokeSession(@Parameter(description = "用户 ID") @PathVariable UUID id,
                                              @Parameter(description = "会话/设备 ID") @PathVariable String sessionId,
                                              @RequestParam(required = false) String reason) {
        return ApiResponse.success(userCommandService.revokeUserSession(id, sessionId, reason));
    }

    /**
     * 强制用户下线
     */
    @Operation(summary = "强制用户下线", description = "调用认证服务强制指定用户下线，并记录下线原因。")
    @PostMapping("/{id}/force-logout")
    @PreAuthorize("hasAuthority('system:user:force-logout')")
    @AuditLog(
            operation = "强制下线",
            businessType = "USER",
            riskLevel = 3
    )
    public ApiResponse<Void> forceLogout(@Parameter(description = "用户 ID") @PathVariable UUID id,
                                         @Parameter(description = "强制下线原因") @RequestParam String reason) {
        userCommandService.forceLogout(id, reason);

        return ApiResponse.success();
    }

    /**
     * 授予临时角色
     */
    @Operation(summary = "授予临时角色", description = "为用户授予在指定生效时间到失效时间之间有效的临时角色。")
    @PostMapping("/{id}/grant-temporary-roles")
    @PreAuthorize("hasAuthority('system:user:temporary-role:grant')")
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
    @PreAuthorize("hasAuthority('system:user:temporary-role:extend')")
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
    @PreAuthorize("hasAuthority('system:user:temporary-role:terminate')")
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
    @PreAuthorize("hasAuthority('system:user:temporary-role:list')")
    public ApiResponse<List<Map<String, Object>>> getUserTemporaryRoles(@Parameter(description = "用户 ID") @PathVariable UUID id) {
        userReadAccessGuard.requireReadable(id);
        List<Map<String, Object>> roles = userQueryService.getUserTemporaryRoles(id);

        return ApiResponse.success(roles);
    }

    /**
     * 查询用户统计信息
     */
    @Operation(summary = "查询用户统计信息", description = "查询指定用户的统计数据。")
    @GetMapping("/{id}/statistics")
    @PreAuthorize("hasAuthority('system:user:statistics')")
    public ApiResponse<Map<String, Object>> getUserStatistics(@Parameter(description = "用户 ID") @PathVariable UUID id) {
        userReadAccessGuard.requireReadable(id);
        Map<String, Object> stats = userQueryService.getUserStatistics(id);

        return ApiResponse.success(stats);
    }

    /**
     * 更新最后登录信
     */
    @Operation(summary = "更新最后登录信息", description = "仅供认证服务通过服务间签名调用，更新最后登录时间与登录 IP。")
    @PostMapping("/{userId}/update-login")
    public ApiResponse<Void> updateLastLogin(@Parameter(description = "用户 ID") @PathVariable UUID userId,
                                             @Parameter(description = "登录 IP 地址") @RequestParam("ipAddress") String ipAddress) {
        userCommandService.updateLastLogin(userId, ipAddress);

        return ApiResponse.success();
    }

    private void maskIfNeeded(UserDetailDTO detail) {
        if (!permissionChecker.hasPermission(
                SecurityUtils.getCurrentUserUuid().orElse(null), "system:user:view:sensitive")) {
            UserSensitiveMasker.mask(detail);
        }
    }

    private ResponseEntity<byte[]> csvResponse(byte[] body, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", fileName);
        return ResponseEntity.ok().headers(headers).body(body);
    }
}