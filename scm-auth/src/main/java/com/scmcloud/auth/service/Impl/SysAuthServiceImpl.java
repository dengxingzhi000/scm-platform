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
import com.scmcloud.auth.service.ISysAuthService;
import com.scmcloud.common.security.util.TotpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
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

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String deviceId) {
        String username = request.getUsername();

        // 1. 妫€鏌ヨ处鎴锋槸鍚﹁閿佸畾
        if (isAccountLocked(username)) {
            auditLogService.recordLoginFailure(username, ipAddress, "璐︽埛宸查攣瀹?);
            throw new LockedException("璐︽埛宸查攣瀹氾紝璇风◢鍚庡啀璇?);
        }

        // 2. 妫€鏌ョ櫥褰曞け璐ユ锟?
        int attempts = getLoginAttempts(username);
        if (attempts >= securityProperties.getMaxLoginAttempts()) {
            lockAccount(username);
            auditLogService.recordLoginFailure(username, ipAddress, "鐧诲綍澶辫触娆℃暟杩囧");
            throw new LockedException("鐧诲綍澶辫触娆℃暟杩囧锛岃处鎴峰凡琚攣瀹?);
        }

        try {
            // 3. 鎵ц璁よ瘉
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );

            SecurityUser user = (SecurityUser) authentication.getPrincipal();
            
            // 楠岃瘉鐢ㄦ埛瀵硅薄闈炵┖
            if (user == null) {
                auditLogService.recordLoginFailure(username, ipAddress, "Authentication failed: user info is empty");
                throw new BadCredentialsException("Authentication failed");
            }

            // 4. 妫€鏌ュ弻鍥犵礌璁よ瘉锛圡FA锟?
            if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
                // 淇: MFA 鍚敤鏃跺繀椤绘彁渚涢獙璇佺爜
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

            // 5. 妫€鏌ュ瘑鐮佹槸鍚﹁繃锟?
            if (user.getPasswordExpireTime() != null && user.getPasswordExpireTime().isBefore(LocalDateTime.now())) {
                auditLogService.recordLogin(user.getUserId(), username, ipAddress, true, "Password expired");
                // 淇: 瀵嗙爜杩囨湡鏃朵笉杩斿洖 token锛屾樉寮忚缃负 null 闃叉瀹夊叏婕忔礊
                return LoginResponse.builder()
                        .accessToken(null)
                        .refreshToken(null)
                        .userId(user.getUserId())
                        .username(username)
                        .needChangePassword(true)
                        .message("Password has expired, please change your password")
                        .build();
            }

            // 6. 鐢熸垚Token
            Set<String> roles = user.getRoles();
            Set<String> permissions = user.getPermissions();
            List<String> amr = Boolean.TRUE.equals(user.getTwoFactorEnabled()) ? List.of("pwd","mfa") :
                    List.of("pwd");
            String accessToken = jwtUtils.generateAccessToken(
                    user.getUserId(), username, roles, permissions, deviceId, ipAddress, amr);
            String refreshToken = jwtUtils.generateRefreshToken(
                    user.getUserId(), username, deviceId);

            // 7. 娓呴櫎鐧诲綍澶辫触璁板綍
            clearLoginAttempts(username);

            // 8. 鏇存柊鏈€鍚庣櫥褰曚俊鎭紙浼樺厛 Dubbo锛屽け璐ュ洖閫€ Feign锟?
            try {
                userDubboService.updateLastLogin(user.getUserId(), ipAddress, LocalDateTime.now());
            } catch (Exception ex) {
                userServiceClient.updateLastLogin(user.getUserId(), ipAddress);
            }

            // 9. 璁板綍鐧诲綍鏃ュ織
            auditLogService.recordLogin(user.getUserId(), username, ipAddress, true, "Login successful");

            // 10. 璁板綍鐧诲綍鎴愬姛鎸囨爣
            businessMetrics.recordLogin(true, deviceId);

            log.info("User login success: {}, IP: {}, Device: {}", username, ipAddress, deviceId);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtProperties.getExpiration() / 1000)
                    .userId(user.getUserId())
                    .username(username)
                    .realName(user.getRealName())
                    .roles(roles)
                    .permissions(permissions)
                    .needChangePassword(user.getForceChangePassword())
                    .build();

        } catch (AuthenticationException e) {
            // 璁よ瘉澶辫触锛屽鍔犲け璐ユ锟?
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

    private boolean isAccountLocked(String username) {
        String lockKey = ACCOUNT_LOCK_PREFIX + username;
        return redisTemplate.hasKey(lockKey);
    }

    private void lockAccount(String username) {
        String lockKey = ACCOUNT_LOCK_PREFIX + username;
        redisTemplate.opsForValue().set(lockKey, System.currentTimeMillis(),
                Duration.ofMillis(securityProperties.getLockDuration()));
        log.warn("Account locked: {}", username);
    }

    private int getLoginAttempts(String username) {
        String attemptKey = LOGIN_ATTEMPTS_PREFIX + username;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptKey);
        return attempts != null ? attempts : 0;
    }

    private void incrementLoginAttempts(String username) {
        String attemptKey = LOGIN_ATTEMPTS_PREFIX + username;
        redisTemplate.opsForValue().increment(attemptKey);
        redisTemplate.expire(attemptKey, Duration.ofMillis(securityProperties.getLockDuration()));
    }

    private void clearLoginAttempts(String username) {
        String attemptKey = LOGIN_ATTEMPTS_PREFIX + username;
        redisTemplate.delete(attemptKey);
    }

    private boolean verifyTwoFactor(String code, UUID userId) {
        // Deprecated path kept for backward compatibility; unified check is done earlier.
        return true;
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
        Boolean firstUse = redisTemplate.opsForValue()
                .setIfAbsent(replayKey, System.currentTimeMillis(), Duration.ofSeconds(90));
        return Boolean.TRUE.equals(firstUse);
    }

    @Override
    public void logout(String token, UUID userId, String reason) {
        jwtUtils.revokeToken(token, reason != null ? reason : "User initiated logout");
        auditLogService.recordLogout(userId, "Logout successful");
        log.info("User logout: UserId={}", userId);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken, String deviceId, String ipAddress) {
        // 淇: 绉婚櫎閫昏緫鍙嶈浆 - isRefreshTokenInvalid() 杩斿洖 true 琛ㄧず鏃犳晥
        if (jwtUtils.isRefreshTokenInvalid(refreshToken)) {
            throw new BadCredentialsException("Refresh token invalid or expired");
        }

        UUID userId = jwtUtils.getUserIdFromToken(refreshToken);
        String username = jwtUtils.getUsernameFromToken(refreshToken);

        // 閲嶆柊鑾峰彇鐢ㄦ埛鏉冮檺锛堜娇锟紻ubbo 楂樻€ц兘 RPC锟?
        Set<String> roles = userDubboService.findRolesByUserId(userId);
        Set<String> permissions = userDubboService.findPermissionsByUserId(userId);

        // 浣跨敤鍒锋柊浠ょ墝杞崲鏈哄埗锛堟帹鑽愶級- 鐢熸垚鏂扮殑璁块棶浠ょ墝鍜屽埛鏂颁护锟?
        JwtUtils.TokenPair tokenPair = jwtUtils.refreshTokenWithRotation(
                refreshToken, roles, permissions, deviceId, ipAddress);

        log.info("Token refreshed with rotation for user: {}", username);

        return LoginResponse.builder()
                .accessToken(tokenPair.getAccessToken())
                .refreshToken(tokenPair.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpiration() / 1000)
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
}
