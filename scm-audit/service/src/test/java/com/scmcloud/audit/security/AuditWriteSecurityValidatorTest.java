package com.scmcloud.audit.security;

import com.scmcloud.common.rest.signature.InternalSignatureSecretResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 启动校验：没有可用密钥时 scm-audit 不能启动，否则等于用仓库里的公开常量保护审计写入通道。
 */
class AuditWriteSecurityValidatorTest {

    @Test
    void missingSecretShouldPreventStartup() {
        AuditWriteSecurityValidator validator = validatorFor(new MockEnvironment());

        assertThrows(IllegalStateException.class, validator::validateInternalSignatureSecret);
    }

    @Test
    void emptySecretShouldPreventStartup() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("API_SECRET_INTERNAL_SERVICE", "");
        AuditWriteSecurityValidator validator = validatorFor(environment);

        assertThrows(IllegalStateException.class, validator::validateInternalSignatureSecret);
    }

    @Test
    void shippedDefaultSecretShouldPreventStartup() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("API_SECRET_INTERNAL_SERVICE", "your-internal-secret-key");
        AuditWriteSecurityValidator validator = validatorFor(environment);

        assertThrows(IllegalStateException.class, validator::validateInternalSignatureSecret);
    }

    @Test
    void configuredSecretShouldAllowStartup() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("API_SECRET_INTERNAL_SERVICE", "5f4dcc3b5aa765d61d8327deb882cf99");
        AuditWriteSecurityValidator validator = validatorFor(environment);

        assertDoesNotThrow(validator::validateInternalSignatureSecret);
    }

    @Test
    void platformPropertyShouldBeAcceptedAsWell() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("security.signature.app-secrets.internal-service",
                "5f4dcc3b5aa765d61d8327deb882cf99");
        AuditWriteSecurityValidator validator = validatorFor(environment);

        assertDoesNotThrow(validator::validateInternalSignatureSecret);
    }

    private AuditWriteSecurityValidator validatorFor(MockEnvironment environment) {
        return new AuditWriteSecurityValidator(new InternalSignatureSecretResolver(environment));
    }
}
