package com.cognora.platform.gateway.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关受众策略（{@code security.audience}）。
 *
 * <p>管理台令牌默认可以访问全部外部路径，但不能访问商城专属路径；商城令牌采用白名单，
 * 只允许访问商城前台相关的服务。策略通过配置外置，新增服务时无需改代码。</p>
 *
 * @author Deng
 * @since 2026-11-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.audience")
public class AudiencePolicyProperties {

    /** 是否开启网关受众校验。 */
    private boolean enabled = true;

    /** 升级前签发、没有 {@code aud} 声明的旧令牌如何处理。 */
    private LegacyTokenPolicy legacyTokenPolicy = LegacyTokenPolicy.ALLOW;

    /** 商城令牌允许访问的路径（白名单）。 */
    private List<String> mallAllowedPaths = new ArrayList<>(List.of(
            "/platform-auth/api/auth/**",
            "/platform-auth/api/public/**",
            "/platform-auth/oauth2/**",
            "/auth/api/auth/**",
            "/auth/api/public/**",
            "/auth/oauth2/**",
            "/ecom-mall/**",
            "/ecom-member/**",
            "/ecom-promotion/**",
            "/ecom-payment/**",
            "/ecom-search/**",
            "/ecom-order-center/**",
            "/ecom-fulfillment/**",
            "/scm-mall/**",
            "/scm-member/**",
            "/scm-promotion/**",
            "/scm-payment/**",
            "/scm-search/**",
            "/scm-order-center/**",
            "/scm-fulfillment/**",
            "/platform-file/**",
            "/scm-file/**",
            "/platform-notify/**",
            "/scm-notify/**",
            "/actuator/health",
            "/actuator/info"));

    /** 管理台令牌禁止访问的路径（商城专属入口）。 */
    private List<String> adminDeniedPaths = new ArrayList<>(List.of(
            "/ecom-mall/**",
            "/scm-mall/**"));

    public enum LegacyTokenPolicy {
        /** 兼容窗口内放行旧令牌。 */
        ALLOW,
        /** 收紧后拒绝旧令牌。 */
        DENY
    }
}
