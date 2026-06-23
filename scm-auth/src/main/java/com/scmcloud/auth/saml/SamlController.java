package com.scmcloud.auth.saml;

import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * SAML SSO控制器。
 *
 * <p>提供SSO相关的API：
 * <ul>
 *   <li>获取SSO登录URL</li>
 *   <li>获取当前用户信息</li>
 *   <li>SLO登出</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/auth/saml")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "saml", name = "enabled", havingValue = "true")
public class SamlController {

    private final SamlProperties samlProperties;

    /**
     * 获取SAML登录URL。
     */
    @GetMapping("/login-url")
    public ApiResponse<Map<String, String>> getLoginUrl() {
        Map<String, String> result = new HashMap<>();
        result.put("loginUrl", "/saml2/authenticate/" + samlProperties.getTenantId());
        result.put("metadataUrl", "/saml/metadata");
        return ApiResponse.success(result);
    }

    /**
     * 获取当前登录用户信息（SAML认证后）。
     */
    @GetMapping("/user-info")
    public ApiResponse<Map<String, Object>> getUserInfo(Principal principal) {
        if (principal instanceof Saml2Authentication authentication) {
            Map<String, String> attributes = new HashMap<>();
            authentication.getAttributes().forEach((key, value) -> {
                if (value instanceof String str) {
                    attributes.put(key.toString(), str);
                } else {
                    attributes.put(key.toString(), value.toString());
                }
            });

            Map<String, Object> result = new HashMap<>();
            result.put("name", authentication.getName());
            result.put("attributes", attributes);
            return ApiResponse.success(result);
        }

        return ApiResponse.fail(401, "未通过SAML认证");
    }

    /**
     * SAML单点登出。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // SLO需要重定向到IdP的登出URL
        // 实际实现需要根据IdP的SLO endpoint进行配置
        log.info("SAML SLO登出请求");
        return ApiResponse.success();
    }
}
