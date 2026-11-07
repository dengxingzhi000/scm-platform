package com.cognora.platform.auth.service;

import com.cognora.platform.common.dto.user.RecoveryCodesResponse;
import com.cognora.platform.common.dto.user.TwoFactorSetupResponse;
import com.cognora.platform.common.dto.user.TwoFactorVerifyRequest;
import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.security.util.RecoveryCodeUtils;
import com.cognora.platform.common.security.util.TotpUtils;
import com.cognora.platform.common.web.domain.SecurityUser;
import com.cognora.platform.common.web.util.SecurityUtils;
import com.cognora.platform.system.api.UserDubboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 2FA 自助管理：初始化、启用、关闭与恢复码重生成。
 *
 * <p>未验证的秘钥先放 Redis 10 分钟，验证通过后才通过 Dubbo 写入 db_user。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private static final String PENDING_SECRET_PREFIX = "mfa:setup:";
    private static final String ISSUER = "Cognora Aura";
    private static final int RECOVERY_CODE_COUNT = 10;

    private final TotpUtils totpUtils;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserDubboService userDubboService;

    public TwoFactorSetupResponse setup() {
        UUID userId = requireUserId();
        SecurityUser user = currentUser();
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(),
                    "二次验证已启用，请先关闭后再重新初始化");
        }

        String secret = totpUtils.generateSecret();
        stringRedisTemplate.opsForValue().set(PENDING_SECRET_PREFIX + userId, secret, Duration.ofMinutes(10));
        String uri = totpUtils.generateQrCodeUrl(user.getUsername(), ISSUER, secret);
        return TwoFactorSetupResponse.builder().secret(secret).otpauthUri(uri).build();
    }

    public void enable(TwoFactorVerifyRequest request) {
        UUID userId = requireUserId();
        String pendingSecret = stringRedisTemplate.opsForValue().get(PENDING_SECRET_PREFIX + userId);
        if (pendingSecret == null || pendingSecret.isBlank()) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(),
                    "初始化已过期，请重新获取二维码");
        }
        if (!totpUtils.verifyCode(pendingSecret, request.getCode())) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "验证码不正确");
        }

        userDubboService.enableTwoFactor(userId, pendingSecret);
        stringRedisTemplate.delete(PENDING_SECRET_PREFIX + userId);
        log.info("2FA enabled: userId={}", userId);
    }

    public void disable(TwoFactorVerifyRequest request) {
        UUID userId = requireUserId();
        SecurityUser user = currentUser();
        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "二次验证未启用");
        }
        if (!verify(user.getTwoFactorSecret(), request.getCode(), userId)) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "验证码或恢复码不正确");
        }

        userDubboService.disableTwoFactor(userId);
        log.info("2FA disabled: userId={}", userId);
    }

    public RecoveryCodesResponse regenerateRecoveryCodes(TwoFactorVerifyRequest request) {
        UUID userId = requireUserId();
        SecurityUser user = currentUser();
        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "二次验证未启用");
        }
        if (!totpUtils.verifyCode(user.getTwoFactorSecret(), request.getCode())) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "验证码不正确");
        }

        List<String> codes = RecoveryCodeUtils.generateCodes(RECOVERY_CODE_COUNT);
        List<String> hashes = codes.stream().map(RecoveryCodeUtils::sha256Hex).toList();
        userDubboService.replaceRecoveryCodes(userId, hashes);
        log.info("2FA recovery codes regenerated: userId={}", userId);
        return RecoveryCodesResponse.builder().codes(codes).build();
    }

    private boolean verify(String secret, String code, UUID userId) {
        if (totpUtils.verifyCode(secret, code)) {
            return true;
        }
        return userDubboService.consumeRecoveryCode(userId, RecoveryCodeUtils.sha256Hex(code));
    }

    private SecurityUser currentUser() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new BusinessException(
                        ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage()));
        SecurityUser user = userDubboService.getUserByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }
        return user;
    }

    private UUID requireUserId() {
        return SecurityUtils.getCurrentUserUuid()
                .orElseThrow(() -> new BusinessException(
                        ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage()));
    }
}
