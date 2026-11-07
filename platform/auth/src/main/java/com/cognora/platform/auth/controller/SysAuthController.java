package com.cognora.platform.auth.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.cognora.platform.auth.service.ISysAuthService;
import com.cognora.platform.common.dto.user.LoginRequest;
import com.cognora.platform.common.dto.user.LoginResponse;
import com.cognora.platform.common.dto.user.LogoutOthersResponse;
import com.cognora.platform.common.dto.user.RefreshTokenRequest;
import com.cognora.platform.common.dto.user.UserInfo;
import com.cognora.platform.common.dto.user.UserSessionDTO;
import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.log.annotation.AuditLog;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.common.security.util.HttpServletRequestUtils;
import com.cognora.platform.common.security.util.IpUtils;
import com.cognora.platform.common.security.util.TraceIdUtil;
import com.cognora.platform.common.sentinel.annotation.RateLimit;
import com.cognora.platform.common.web.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author Deng
 * @version 1.1
 * @since 2025-10-14
 * @apiNote 1.1 修复乱码注释，移除冗余校验，细化异常处理
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "提供登录、登出、令牌刷新、用户信息查询与强制下线等认证能力。")
public class SysAuthController {
    private final ISysAuthService authService;
    private final HttpServletRequestUtils httpServletRequestUtils;

    @Operation(summary = "用户登录", description = "校验用户名与密码，签发访问令牌与刷新令牌。")
    @PostMapping("/login")
    @SentinelResource(value = "auth_login")
    @RateLimit()
    public ApiResponse<LoginResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = IpUtils.getClientIp(httpRequest);
        String deviceId = httpServletRequestUtils.getDeviceId(httpRequest);
        String traceId = TraceIdUtil.resolveAndSanitizeTraceId(httpRequest);

        LoginResponse response = authService.login(request, ipAddress, deviceId);
        log.info("login success traceId={} user={} ip={} device={}", (traceId + "").replace("\r", "_").replace("\n", "_"), (request.getUsername() + "").replace("\r", "_").replace("\n", "_"), (ipAddress + "").replace("\r", "_").replace("\n", "_"), (deviceId + "").replace("\r", "_").replace("\n", "_"));

        return ApiResponse.success(response);
    }

    @Operation(summary = "用户登出", description = "注销当前登录令牌并清理相关会话信息。")
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = httpServletRequestUtils.getTokenFromRequest(request);
        UUID userId = SecurityUtils.getCurrentUserUuid().orElse(null);
        String traceId = TraceIdUtil.resolveAndSanitizeTraceId(request);

        authService.logout(token, userId, "User initiated logout");
        log.info("logout traceId={} userId={}", (traceId + "").replace("\r", "_").replace("\n", "_"), (userId + "").replace("\r", "_").replace("\n", "_"));

        return ApiResponse.success();
    }

    @Operation(summary = "刷新令牌", description = "使用刷新令牌换取新的访问令牌，并同步更新设备与会话信息。")
    @PostMapping("/refresh")
    @RateLimit()
    public ApiResponse<LoginResponse> refreshToken(
            @RequestBody @Valid RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = IpUtils.getClientIp(httpRequest);

        String deviceId = httpServletRequestUtils.getDeviceId(httpRequest);
        if (StringUtils.hasText(request.getDeviceId())) {
            deviceId = request.getDeviceId();
        }

        LoginResponse response = authService.refreshToken(
                request.getRefreshToken(),
                deviceId,
                ipAddress);

        return ApiResponse.success(response);
    }

    @Operation(summary = "获取当前用户信息", description = "根据当前登录身份返回用户基础信息及其权限数据。")
    @GetMapping("/userinfo")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserInfo> getUserInfo(HttpServletRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid().orElse(null);
        if (userId == null) {
            return ApiResponse.fail(401, "Unauthorized");
        }

        try {
            UserInfo userInfo = authService.getUserInfo(userId);
            return ApiResponse.success(userInfo);
        } catch (IllegalArgumentException ex) {
            log.warn("getUserInfo failed: userId={} error={}", userId, ex.getMessage());
            return ApiResponse.fail(404, ex.getMessage());
        } catch (Exception ex) {
            String traceId = TraceIdUtil.resolveAndSanitizeTraceId(request);
            log.error("getUserInfo failed traceId={} userId={}", (traceId + "").replace("\r", "_").replace("\n", "_"), (userId + "").replace("\r", "_").replace("\n", "_"), ex);
            return ApiResponse.fail(503, "User service unavailable");
        }
    }

    @Operation(summary = "查询登录会话", description = "列出当前访问令牌所属用户的全部有效会话，用户身份只从令牌解析。")
    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<UserSessionDTO>> listSessions(HttpServletRequest request) {
        String token = httpServletRequestUtils.getTokenFromRequest(request);
        return ApiResponse.success(authService.listSessions(token));
    }

    @Operation(summary = "撤销其他设备会话", description = "撤销除当前设备外的全部访问令牌，不接受 userId 入参。")
    @PostMapping("/sessions/logout-others")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(
            operation = "撤销其他设备会话",
            businessType = "USER",
            riskLevel = 2
    )
    public ApiResponse<LogoutOthersResponse> logoutOthers(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        rejectUserIdOverride(request, body);
        int revoked = authService.logoutOtherSessions(httpServletRequestUtils.getTokenFromRequest(request));
        return ApiResponse.success(new LogoutOthersResponse(revoked));
    }

    /**
     * 会话端点只允许操作当前令牌所属用户：任何 userId/user_id 入参都必须拒绝，
     * 否则普通用户可借该接口把他人踢下线（等价于绕过 system:user:edit）。
     */
    private void rejectUserIdOverride(HttpServletRequest request, Map<String, Object> body) {
        boolean queryCarriesUserId = request.getParameterMap().containsKey("userId")
                || request.getParameterMap().containsKey("user_id");
        boolean bodyCarriesUserId = body != null
                && (body.containsKey("userId") || body.containsKey("user_id"));
        if (queryCarriesUserId || bodyCarriesUserId) {
            throw new BusinessException(400, "userId is not accepted; the target user is resolved from the access token");
        }
    }

    @Operation(summary = "管理员查询用户会话", description = "仅供 scm-system 通过服务间签名调用，列出指定用户的有效会话。")
    @GetMapping("/admin/users/{userId}/sessions")
    public ApiResponse<List<UserSessionDTO>> listUserSessions(
            @Parameter(description = "用户 ID") @PathVariable UUID userId) {
        return ApiResponse.success(authService.listUserSessions(userId));
    }

    @Operation(summary = "管理员撤销用户会话", description = "仅供 scm-system 通过服务间签名调用，撤销指定用户的单个设备会话。")
    @DeleteMapping("/admin/users/{userId}/sessions/{deviceId}")
    @AuditLog(operation = "撤销用户会话", businessType = "USER", riskLevel = 3)
    public ApiResponse<Integer> revokeUserSession(
            @Parameter(description = "用户 ID") @PathVariable UUID userId,
            @Parameter(description = "设备 ID") @PathVariable String deviceId,
            @Parameter(description = "撤销原因") @RequestParam(required = false) String reason) {
        return ApiResponse.success(authService.revokeUserSession(userId, deviceId, reason));
    }

    @Operation(summary = "强制下线用户", description = "管理员强制指定用户下线并记录下线原因。")
    @PostMapping("/force-logout/{userId}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @AuditLog(
            operation = "强制下线",
            businessType = "USER",
            riskLevel = 3
    )
    public ApiResponse<Void> forceLogout(
            @Parameter(description = "被强制下线的用户 ID") @PathVariable UUID userId,
            @Parameter(description = "强制下线原因") @RequestParam @NotBlank(message = "Logout reason cannot be empty") String reason) {

        authService.forceLogout(userId, reason);
        log.info("force logout userId={} reason={}", (userId + "").replace("\r", "_").replace("\n", "_"), (reason + "").replace("\r", "_").replace("\n", "_"));

        return ApiResponse.success();
    }
}
