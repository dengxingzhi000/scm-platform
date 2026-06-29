package com.scmcloud.auth.service.Impl;

import com.scmcloud.common.rest.client.SysUserServiceClient;
import com.scmcloud.system.api.UserDubboService;
import com.scmcloud.common.metrics.BusinessMetrics;
import com.scmcloud.common.web.domain.SecurityUser;
import com.scmcloud.common.security.properties.JwtProperties;
import com.scmcloud.common.security.properties.SecurityProperties;
import com.scmcloud.common.security.util.JwtUtils;
import com.scmcloud.common.log.service.ISysAuditLogService;
import com.scmcloud.common.dto.user.LoginRequest;
import com.scmcloud.common.dto.user.LoginResponse;
import com.scmcloud.common.dto.user.UserInfo;
import com.scmcloud.auth.service.ISysAuthService;
import com.scmcloud.common.security.util.TotpUtils;
import com.scmcloud.common.redis.script.LuaScriptRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 璁よ瘉鏈嶅姟
 *
 * @author Deng
 * createData 2025/10/14 15:00
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SysAuthServiceImpl implements ISysAuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final SysUserServiceClient userServiceClient;
    private final ISysAuditLogService auditLogService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityProperties securityProperties;
    private final JwtProperties jwtProperties;
    private final BusinessMetrics businessMetrics;
    private final TotpUtils totpUtils;
    private final UserDubboService userDubboService;

    private static final String LOGIN_ATTEMPTS_PREFIX = "login:attempts:";
    private static final String ACCOUNT_LOCK_PREFIX = "account:lock:";

    private final LuaScriptRegistry luaScriptRegistry;

    private long tokenExpirationInSeconds;

    @PostConstruct
    public void init() {
        this.tokenExpirationInSeconds = jwtProperties.getExpiration() / 1000;
    }

    @Override
    public LoginResponse login(LoginRequest request, String ipAddress, String deviceId) {
        String username = request.getUsername();

        // 1. Atomically check if account is locked or max attempts reached (Lua script)
        String lockKey = ACCOUNT_LOCK_PREFIX + username;
        String attemptKey = LOGIN_ATTEMPTS_PREFIX + username;
        try {
            Long locked = redisTemplate.execute(
                    luaScriptRegistry.get("auth:check_and_lock", Long.class),
                    List.of(lockKey, attemptKey),
                    String.valueOf(securityProperties.getMaxLoginAttempts()),
                    String.valueOf(System.currentTimeMillis()),
                    String.valueOf(securityProperties.getLockDuration()));
            if (Long.valueOf(1L).equals(locked)) {
                auditLogService.recordLoginFailure(username, ipAddress, "Account locked or excessive login failures");
                throw new LockedException("Account locked, please try later");
            }
        } catch (LockedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis CHECK_AND_LOCK failed for user={}: {}", username, e.getMessage());
            auditLogService.recordLoginFailure(username, ipAddress, "Security check unavailable: " + e.getMessage());
            throw new LockedException("Security service unavailable, please try later");
        }

        try {
            // 2. Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );

            SecurityUser user = (SecurityUser) authentication.getPrincipal();

            if (user == null) {
                auditLogService.recordLoginFailure(username, ipAddress, "Authentication failed: user info is empty");
                throw new BadCredentialsException("Authentication failed");
            }

            // 3. Check MFA
            if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
                if (!StringUtils.hasText(request.getTwoFactorCode())) {
                    auditLogService.recordLoginFailure(username, ipAddress, "MFA enabled but verification code not provided");
                    businessMetrics.recordLoginAttempt(false, "mfa");
                    throw new BadCredentialsException("Two-factor authentication enabled, verification code required");
                }
                if (!verifyTwoFactor(user.getTwoFactorSecret(), request.getTwoFactorCode(), user.getUserId())) {
                    auditLogService.recordLoginFailure(username, ipAddress, "Two-factor verification failed");
                    businessMetrics.recordLoginAttempt(false, "mfa");
                    throw new BadCredentialsException("Two-factor verification code incorrect");
                }
            }

            // 4. Check password expiry
            if (user.getPasswordExpireTime() != null && user.getPasswordExpireTime().isBefore(LocalDateTime.now())) {
                auditLogService.recordLogin(user.getUserId(), username, ipAddress, true, "Password expired");
                return LoginResponse.builder()
                        .accessToken(null)
                        .refreshToken(null)
                        .userId(user.getUserId())
                        .username(username)
                        .needChangePassword(true)
                        .message("Password has expired, please change your password")
                        .build();
            }

            // 5. Generate tokens
            Set<String> roles = user.getRoles();
            Set<String> permissions = user.getPermissions();
            List<String> amr = Boolean.TRUE.equals(user.getTwoFactorEnabled()) ? List.of("pwd", "mfa") :
                    List.of("pwd");
            String accessToken = jwtUtils.generateAccessToken(
                    user.getUserId(), username, roles, permissions, deviceId, ipAddress, amr);
            String refreshToken = jwtUtils.generateRefreshToken(
                    user.getUserId(), username, deviceId);

            // 6. Clear login attempts
            clearLoginAttempts(username);

            // 7. Update last login (Dubbo first, fallback to Feign)
            try {
                userDubboService.updateLastLogin(user.getUserId(), ipAddress, LocalDateTime.now());
            } catch (Exception ex) {
                userServiceClient.updateLastLogin(user.getUserId(), ipAddress);
            }

            // 8. Audit log
            auditLogService.recordLogin(user.getUserId(), username, ipAddress, true, "Login successful");

            // 9. Metrics
            businessMetrics.recordLogin(true, deviceId);

            log.info("User login success: {}, IP: {}, Device: {}", username, ipAddress, deviceId);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(tokenExpirationInSeconds)
                    .userId(user.getUserId())
                    .username(username)
                    .realName(user.getRealName())
                    .roles(roles)
                    .permissions(permissions)
                    .needChangePassword(user.getForceChangePassword())
                    .build();

        } catch (AuthenticationException e) {
            incrementLoginAttempts(username);
            auditLogService.recordLoginFailure(username, ipAddress, e.getMessage());

            int remainingAttempts = securityProperties.getMaxLoginAttempts() - getLoginAttempts(username);
            String message = "Invalid username or password";
            if (remainingAttempts > 0) {
                message += ", attempts remaining: " + remainingAttempts;
            }

            log.warn("Login failed for user: {}, IP: {}, Reason: {}", username, ipAddress, e.getMessage());

            businessMetrics.recordLogin(false, deviceId);
            throw new BadCredentialsException(message);
        }
    }

    private int getLoginAttempts(String username) {
        String attemptKey = LOGIN_ATTEMPTS_PREFIX + username;
        try {
            Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptKey);
            return attempts != null ? attempts : 0;
        } catch (Exception e) {
            log.warn("Redis GET failed for key={}: {}", attemptKey, e.getMessage());
            return 0;
        }
    }

    private void incrementLoginAttempts(String username) {
        String attemptKey = LOGIN_ATTEMPTS_PREFIX + username;
        try {
            redisTemplate.execute(
                    luaScriptRegistry.get("auth:increment_and_expire", Long.class),
                    Collections.singletonList(attemptKey),
                    String.valueOf(securityProperties.getLockDuration()));
        } catch (Exception e) {
            log.error("Redis INCR+EXPIRE failed for key={}: {}", attemptKey, e.getMessage());
        }
    }

    private void clearLoginAttempts(String username) {
        String attemptKey = LOGIN_ATTEMPTS_PREFIX + username;
        try {
            redisTemplate.delete(attemptKey);
        } catch (Exception e) {
            log.warn("Redis DELETE failed for key={}: {}", attemptKey, e.getMessage());
        }
    }

    private boolean verifyTwoFactor(String secret, String code, UUID userId) {
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(code)) {
            return false;
        }

        boolean valid = totpUtils.verifyCode(secret, code);
        if (!valid) {
            return false;
        }

        String replayKey = "mfa:totp:used:" + userId + ":" + code;
        try {
            Boolean firstUse = redisTemplate.opsForValue()
                    .setIfAbsent(replayKey, System.currentTimeMillis(), Duration.ofSeconds(90));
            return Boolean.TRUE.equals(firstUse);
        } catch (Exception e) {
            log.error("Redis SETNX failed for key={}: {}", replayKey, e.getMessage());
            return false;
        }
    }

    @Override
    public void logout(String token, UUID userId, String reason) {
        jwtUtils.revokeToken(token, reason != null ? reason : "User initiated logout");
        auditLogService.recordLogout(userId, "Logout successful");
        log.info("User logout: UserId={}", userId);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken, String deviceId, String ipAddress) {
        if (jwtUtils.isRefreshTokenInvalid(refreshToken)) {
            throw new BadCredentialsException("Refresh token invalid or expired");
        }

        UUID userId = jwtUtils.getUserIdFromToken(refreshToken);
        String username = jwtUtils.getUsernameFromToken(refreshToken);

        Set<String> roles = userDubboService.findRolesByUserId(userId);
        Set<String> permissions = userDubboService.findPermissionsByUserId(userId);

        JwtUtils.TokenPair tokenPair = jwtUtils.refreshTokenWithRotation(
                refreshToken, roles, permissions, deviceId, ipAddress);

        log.info("Token refreshed with rotation for user: {}", username);

        return LoginResponse.builder()
                .accessToken(tokenPair.getAccessToken())
                .refreshToken(tokenPair.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(tokenExpirationInSeconds)
                .userId(userId)
                .username(username)
                .build();
    }

    @Override
    public void forceLogout(UUID userId, String reason) {
        jwtUtils.revokeAllUserTokens(userId);
        auditLogService.recordLogout(userId, "Admin force logout: " + reason);
        log.info("User force logout: UserId={}, Reason={}", userId, reason);
    }

    @Override
    public UserInfo getUserInfo(UUID userId) {
        return userDubboService.getUserInfo(userId);
    }
}
