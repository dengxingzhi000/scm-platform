package com.scmcloud.auth.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.scmcloud.auth.service.ISysAuthService;
import com.scmcloud.common.dto.user.LoginRequest;
import com.scmcloud.common.dto.user.LoginResponse;
import com.scmcloud.common.dto.user.RefreshTokenRequest;
import com.scmcloud.common.dto.user.UserInfo;
import com.scmcloud.common.log.annotation.AuditLog;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.security.util.HttpServletRequestUtils;
import com.scmcloud.common.security.util.IpUtils;
import com.scmcloud.common.sentinel.annotation.RateLimit;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.system.api.UserDubboService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 绯荤粺璁よ瘉鎺у埗锟?
 * 鎻愪緵鐢ㄦ埛鐧诲綍銆佺櫥鍑恒€乀oken鍒锋柊绛夌浉鍏虫帴锟?
 * <p>
 * WebAuthn 鐩稿叧鎺ュ彛璇峰弬锟絳@link WebAuthnCredentialController}
 *
 * @author Deng
 * @version 1.0
 * @since 2025-10-14
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SysAuthController {
    private final ISysAuthService authService;
    private final HttpServletRequestUtils httpServletRequestUtils;
    private final UserDubboService userDubboService;

    /**
     * 鐢ㄦ埛鐧诲綍鎺ュ彛
     *
     * @param request 鐧诲綍璇锋眰鍙傛暟
     * @param httpRequest HTTP 璇锋眰瀵硅薄
     * @return 鐧诲綍鍝嶅簲缁撴灉
     */
    @PostMapping("/login")
    @SentinelResource(value = "auth_login")
    @RateLimit()
    public ApiResponse<LoginResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = IpUtils.getClientIp(httpRequest);
        String deviceId = httpServletRequestUtils.getDeviceId(httpRequest);
        String traceId = traceId(httpRequest);

        LoginResponse response = authService.login(request, ipAddress, deviceId);
        log.info("login success traceId={} user={} ip={} device={}", traceId, request.getUsername(), ipAddress, deviceId);

        return ApiResponse.success(response);
    }

    /**
     * 鐢ㄦ埛鐧诲嚭鎺ュ彛
     *
     * @param request HTTP 璇锋眰瀵硅薄
     * @return 鎿嶄綔缁撴灉
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = httpServletRequestUtils.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return ApiResponse.fail(400, "Missing token");
        }
        UUID userId = SecurityUtils.getCurrentUserUuid().orElse(null);
        String traceId = traceId(request);

        authService.logout(token, userId, "User initiated logout");
        log.info("logout traceId={} userId={}", traceId, userId);

        return ApiResponse.success();
    }

    /**
     * 鍒锋柊 Token 鎺ュ彛
     *
     * @param request 鍒锋柊 Token 璇锋眰
     * @param httpRequest HTTP 璇锋眰瀵硅薄
     * @return 鐧诲綍鍝嶅簲缁撴灉
     */
    @PostMapping("/refresh")
    @RateLimit()
    public ApiResponse<LoginResponse> refreshToken(
            @RequestBody @Valid RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        if (!StringUtils.hasText(request.getRefreshToken())) {
            return ApiResponse.fail(400, "Missing refresh token");
        }
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

    /**
     * 鑾峰彇褰撳墠鐢ㄦ埛淇℃伅鎺ュ彛
     *
     * @param request HTTP 璇锋眰瀵硅薄
     * @return 鐢ㄦ埛淇℃伅
     */
    @GetMapping("/userinfo")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserInfo> getUserInfo(HttpServletRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid().orElse(null);
        if (userId == null) {
            return ApiResponse.fail(401, "Unauthorized");
        }

        // 濡傛灉 Dubbo 涓嶅彲鐢紝璇存槑绯荤粺寮傚父锛屽簲璇ュ揩閫熷け锟?
        UserInfo userInfo;
        try {
            userInfo = userDubboService.getUserInfo(userId);
        } catch (Exception ex) {
            String traceId = traceId(request);
            log.error("getUserInfo failed via Dubbo traceId={} userId={} error={}",
                    traceId, userId, ex.getMessage());
            return ApiResponse.fail(503, "User service unavailable");
        }

        return ApiResponse.success(userInfo);
    }

    /**
     * 寮哄埗鐢ㄦ埛鐧诲嚭鎺ュ彛
     *
     * @param userId 鐢ㄦ埛 ID
     * @param reason 鐧诲嚭鍘熷洜
     * @return 鎿嶄綔缁撴灉
     */
    @PostMapping("/force-logout/{userId}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @AuditLog(
            operation = "寮哄埗涓嬬嚎",
            businessType = "USER",
            riskLevel = 3
    )
    public ApiResponse<Void> forceLogout(
            @PathVariable UUID userId,
            @RequestParam @NotBlank(message = "Logout reason cannot be empty") String reason) {

        authService.forceLogout(userId, reason);
        log.info("force logout userId={} reason={}", userId, reason);

        return ApiResponse.success();
    }

    private String traceId(HttpServletRequest request) {
        String id = request.getHeader("X-Request-ID");
        if (!StringUtils.hasText(id)) {
            id = request.getHeader("traceparent");
        }
        return StringUtils.hasText(id) ? id : UUIDv7Util.generate().toString();
    }
}
