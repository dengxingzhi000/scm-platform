package com.cognora.platform.common.security.audience;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务服务侧的受众校验配置（{@code security.audience}）。
 *
 * <p>默认关闭且允许列表为空，保持升级前的行为：不配置就不校验。网关是主要校验点，
 * 这里是防止绕过网关直连的第二道校验，按服务逐步开启。</p>
 *
 * @author Deng
 * @since 2026-11-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.audience")
public class TokenAudienceProperties {

    /** 是否开启本服务的受众校验。 */
    private boolean enabled = false;

    /** 允许访问本服务的受众；为空表示不限制。 */
    private List<String> allowed = new ArrayList<>();

    /** 升级前签发、没有 {@code aud} 声明的旧令牌是否放行。 */
    private boolean allowLegacyToken = true;
}
