package com.cognora.platform.common.security.audience;

import com.cognora.platform.common.security.metrics.SecurityMetrics;
import com.cognora.platform.common.security.util.HttpServletRequestUtils;
import com.cognora.platform.common.security.util.JwtUtils;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 业务服务侧受众过滤器测试。
 */
@DisplayName("Token audience filter tests")
class TokenAudienceFilterTest {

    private JwtUtils jwtUtils;
    private HttpServletRequestUtils requestUtils;
    private TokenAudienceProperties properties;
    private TokenAudienceFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtils = mock(JwtUtils.class);
        requestUtils = mock(HttpServletRequestUtils.class);
        properties = new TokenAudienceProperties();
        filter = new TokenAudienceFilter(jwtUtils, requestUtils, properties,
                new SecurityMetrics(new SimpleMeterRegistry()));
    }

    @Test
    @DisplayName("未开启受众校验时直接放行")
    void disabledFilterPassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("受众不在允许列表时返回 403")
    void mismatchedAudienceIsForbidden() throws Exception {
        properties.setEnabled(true);
        properties.setAllowed(java.util.List.of(TokenAudience.ADMIN));
        when(requestUtils.getTokenFromRequest(org.mockito.ArgumentMatchers.any())).thenReturn("token");
        when(jwtUtils.getAudienceFromToken("token")).thenReturn(TokenAudience.MALL);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("AUDIENCE_NOT_ALLOWED");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("受众匹配时放行")
    void matchedAudiencePassesThrough() throws Exception {
        properties.setEnabled(true);
        properties.setAllowed(java.util.List.of(TokenAudience.ADMIN));
        when(requestUtils.getTokenFromRequest(org.mockito.ArgumentMatchers.any())).thenReturn("token");
        when(jwtUtils.getAudienceFromToken("token")).thenReturn(TokenAudience.ADMIN);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("兼容窗口内无 aud 的旧令牌放行")
    void legacyTokenIsAllowedDuringTransition() throws Exception {
        properties.setEnabled(true);
        properties.setAllowed(java.util.List.of(TokenAudience.ADMIN));
        properties.setAllowLegacyToken(true);
        when(requestUtils.getTokenFromRequest(org.mockito.ArgumentMatchers.any())).thenReturn("legacy");
        when(jwtUtils.getAudienceFromToken("legacy")).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("收紧后无 aud 的旧令牌返回 401")
    void legacyTokenIsRejectedAfterEnforcement() throws Exception {
        properties.setEnabled(true);
        properties.setAllowed(java.util.List.of(TokenAudience.ADMIN));
        properties.setAllowLegacyToken(false);
        when(requestUtils.getTokenFromRequest(org.mockito.ArgumentMatchers.any())).thenReturn("legacy");
        when(jwtUtils.getAudienceFromToken("legacy")).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("invalid_token_audience_missing");
    }
}
