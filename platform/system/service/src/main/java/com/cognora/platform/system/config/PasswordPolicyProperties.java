package com.cognora.platform.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 密码策略配置。配置前缀沿用平台约定 {@code cognora.*}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cognora.security.password-policy")
public class PasswordPolicyProperties {

    private int minLength = 8;
    private int maxLength = 64;
    private boolean requireUppercase = true;
    private boolean requireLowercase = true;
    private boolean requireDigit = true;
    private boolean requireSpecial = true;

    /** 禁止复用最近 N 次密码。 */
    private int historyCount = 5;

    /** 密码有效期天数。 */
    private int expireDays = 90;

    /** 是否允许在重置密码响应中返回明文临时密码。 */
    private boolean revealResetPassword = true;
}
