package com.cognora.platform.system.service.support;

import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.util.UUIDv7Util;
import com.cognora.platform.system.config.PasswordPolicyProperties;
import com.cognora.platform.system.domain.entity.SysUserPasswordHistory;
import com.cognora.platform.system.mapper.SysUserPasswordHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * 密码策略与密码历史校验。
 *
 * <p>BCrypt 只取前 72 字节，超过部分会被静默忽略，因此这里显式拒绝超过
 * 72 UTF-8 字节的密码，避免“长度看起来不同、实际等价”的绕过。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordPolicyService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final PasswordPolicyProperties properties;
    private final SysUserPasswordHistoryMapper historyMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 校验密码复杂度。仅用于调用方显式设置密码的场景；系统生成的随机密码另行保证复杂度。
     */
    public void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw policyViolation("密码不能为空");
        }
        if (rawPassword.length() < properties.getMinLength()
                || rawPassword.length() > properties.getMaxLength()) {
            throw policyViolation("密码长度必须在" + properties.getMinLength()
                    + "-" + properties.getMaxLength() + "个字符之间");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw policyViolation("密码超过 BCrypt 支持的最大长度（72字节）");
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (char c : rawPassword.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        if (properties.isRequireUppercase() && !hasUpper) {
            throw policyViolation("密码必须包含大写字母");
        }
        if (properties.isRequireLowercase() && !hasLower) {
            throw policyViolation("密码必须包含小写字母");
        }
        if (properties.isRequireDigit() && !hasDigit) {
            throw policyViolation("密码必须包含数字");
        }
        if (properties.isRequireSpecial() && !hasSpecial) {
            throw policyViolation("密码必须包含特殊字符");
        }
    }

    /**
     * 禁止复用最近 N 次密码（包含当前密码）。
     */
    public void validateNotReused(UUID userId, String rawPassword) {
        if (userId == null || rawPassword == null || properties.getHistoryCount() <= 0) {
            return;
        }
        List<String> recentHashes = historyMapper.selectRecentHashes(userId, properties.getHistoryCount());
        if (recentHashes == null || recentHashes.isEmpty()) {
            return;
        }
        for (String hash : recentHashes) {
            if (hash != null && passwordEncoder.matches(rawPassword, hash)) {
                throw new BusinessException(ResultCode.USER_PASSWORD_IN_HISTORY.getCode(),
                        ResultCode.USER_PASSWORD_IN_HISTORY.getMessage());
            }
        }
    }

    /**
     * 记录一条密码历史。changeType: INITIAL / CHANGE / RESET。
     */
    public void record(UUID userId, String encodedPassword, String changeType) {
        if (userId == null || encodedPassword == null) {
            return;
        }
        SysUserPasswordHistory history = new SysUserPasswordHistory();
        history.setId(UUIDv7Util.generate());
        history.setUserId(userId);
        history.setPasswordHash(encodedPassword);
        history.setChangeType(changeType);
        historyMapper.insert(history);
    }

    public int getExpireDays() {
        return properties.getExpireDays();
    }

    public boolean isRevealResetPassword() {
        return properties.isRevealResetPassword();
    }

    private BusinessException policyViolation(String message) {
        return new BusinessException(ResultCode.USER_PASSWORD_POLICY_VIOLATION.getCode(), message);
    }
}
