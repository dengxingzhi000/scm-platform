package com.cognora.platform.auth.audience;

import com.cognora.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 把登录请求中的 clientId 解析为令牌受众。
 *
 * <p>未登记的 clientId 直接拒绝，避免客户端“发明”一个受众；未传 clientId 时回落到默认
 * 管理台客户端，保证升级前的登录调用方行为不变。</p>
 *
 * @author Deng
 * @since 2026-11-06
 */
@Component
@RequiredArgsConstructor
public class AuthAudienceResolver {

    private final AuthClientProperties properties;

    public ResolvedClient resolve(String requestedClientId) {
        String clientId = StringUtils.hasText(requestedClientId)
                ? requestedClientId.trim()
                : properties.getDefaultClientId();
        return properties.getRegistry().stream()
                .filter(client -> client.getClientId().equals(clientId))
                .findFirst()
                .map(client -> new ResolvedClient(client.getClientId(), client.getAudience()))
                .orElseThrow(() -> new BusinessException(400, "unknown_client_id"));
    }

    public record ResolvedClient(String clientId, String audience) {
    }
}
