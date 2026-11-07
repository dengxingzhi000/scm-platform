package com.cognora.platform.common.security.audience;

/**
 * 令牌受众（JWT 标准声明 {@code aud}）。
 *
 * <p>平台只有两个前端入口，因此只有两个受众：管理台与商城前台。审计、审计日志等
 * 下游判断必须使用这里的常量，不要各自拼字符串。</p>
 *
 * @author Deng
 * @since 2026-11-06
 */
public final class TokenAudience {

    /** 管理台（Admin Console）令牌。 */
    public static final String ADMIN = "cognora-admin";

    /** 商城前台（Storefront）令牌。 */
    public static final String MALL = "cognora-mall";

    private TokenAudience() {
    }
}
