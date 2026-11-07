package com.cognora.platform.auth.audience;

import com.cognora.platform.common.security.audience.TokenAudience;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录客户端注册表（{@code auth.clients}）。
 *
 * <p>平台只有管理台与商城前台两个入口，每个入口对应一个 clientId 与一个固定的令牌受众。
 * 受众由服务端映射决定，客户端无法通过请求参数自行选择。</p>
 *
 * @author Deng
 * @since 2026-11-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.clients")
public class AuthClientProperties {

    /** 未传 clientId 时使用的客户端；保持管理台默认值，兼容升级前的登录调用方。 */
    private String defaultClientId = "cognora-aura-web";

    private List<Client> registry = new ArrayList<>(List.of(
            new Client("cognora-aura-web", TokenAudience.ADMIN, "管理台"),
            new Client("cognora-aura-mall", TokenAudience.MALL, "商城前台")));

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Client {
        private String clientId;
        private String audience;
        private String description;
    }
}
