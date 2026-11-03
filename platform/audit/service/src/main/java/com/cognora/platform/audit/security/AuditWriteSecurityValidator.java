package com.cognora.platform.audit.security;

import com.cognora.platform.common.rest.signature.InternalSignatureSecretResolver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * 启动校验：内部写入通道没有可用密钥时拒绝启动。
 *
 * <p>审计台账是只追加的账本，{@code POST /sys-audit-log} 只能由内部服务写入。密钥缺失或仍是仓库里
 * 的默认值时，服务必须启动失败——否则等于用公开常量保护写入通道，任何能访问 scm-audit 的人都能
 * 伪造签名并写入任意审计记录。
 *
 * <p>写法参照网关的 {@code SecurityConfigurationValidator}（{@code @PostConstruct} + 明确的报错信息）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditWriteSecurityValidator {

    /** 随仓库发布的占位密钥，任何一个都不允许用来保护写入通道。 */
    private static final Set<String> FORBIDDEN_SECRETS = Set.of(
            "your-internal-secret-key",
            "your-secret-key-here",
            "changeme",
            "changeit");

    /** 占位密钥里出现过的片段（大小写不敏感），出现即拒绝启动。 */
    private static final Set<String> FORBIDDEN_FRAGMENTS = Set.of(
            "change-me",
            "change_me",
            "changeme",
            "your-secret",
            "your-internal",
            "placeholder");

    /** 网关对内部密钥的强度要求（{@code SecurityConfigurationValidator#validateSecretStrength}）。 */
    private static final int MIN_SECRET_LENGTH = 32;

    private final InternalSignatureSecretResolver secretResolver;

    @PostConstruct
    public void validateInternalSignatureSecret() {
        String secret = secretResolver.resolveSecret();

        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "the audit service refuses to start: the internal audit write channel "
                            + "(POST /sys-audit-log) has no signature secret. "
                            + "Set API_SECRET_INTERNAL_SERVICE (or "
                            + "security.signature.app-secrets.internal-service) to the same random value "
                            + "on every service that reports audit records, then start the audit service "
                            + "again.");
        }

        if (FORBIDDEN_SECRETS.contains(secret.trim())) {
            throw new IllegalStateException(
                    "the audit service refuses to start: the internal audit write channel "
                            + "(POST /sys-audit-log) still uses a shipped placeholder secret. "
                            + "Set API_SECRET_INTERNAL_SERVICE (or "
                            + "security.signature.app-secrets.internal-service) to a random value of "
                            + "at least 32 characters on every service that reports audit records, "
                            + "then start the audit service again.");
        }

        String lower = secret.toLowerCase(Locale.ROOT);
        for (String fragment : FORBIDDEN_FRAGMENTS) {
            if (lower.contains(fragment)) {
                throw new IllegalStateException(
                        "the audit service refuses to start: the internal audit write channel "
                                + "(POST /sys-audit-log) still uses a shipped placeholder secret "
                                + "(it contains '" + fragment + "'). "
                                + "Set API_SECRET_INTERNAL_SERVICE (or "
                                + "security.signature.app-secrets.internal-service) to a random value of "
                                + "at least 32 characters on every service that reports audit records, "
                                + "then start the audit service again.");
            }
        }

        if (secret.length() < MIN_SECRET_LENGTH) {
            log.warn("The internal signature secret is shorter than {} characters; "
                    + "use a random value of at least that length.", MIN_SECRET_LENGTH);
        }

        log.info("The internal audit write channel is protected by the configured signature secret");
    }
}
