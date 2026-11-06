package com.cognora.platform.inventory.security;

import com.cognora.platform.common.rest.signature.InternalSignatureSecretResolver;
import com.cognora.platform.common.rest.signature.InternalSignatureVerifier;
import com.cognora.platform.common.security.PermissionService;
import com.cognora.platform.common.security.config.ApiAccessControlProperties;
import com.cognora.platform.common.security.filter.InternalEndpointSignatureFilter;
import com.cognora.platform.common.security.metrics.SecurityMetrics;
import com.cognora.platform.common.web.domain.SecurityUser;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * 内部接口 release-expired 的鉴权用例。
 *
 * <p>该接口属于 {@code internalSignaturePaths}：即使请求携带登录态与库存权限，
 * 没有合法服务间签名也必须 403。
 */
class ReleaseExpiredAuthorizationTest {

  private static final String SECRET = "unit-test-internal-secret-0123456789abcdef";
  private static final String RELEASE_EXPIRED_PATH =
      "/api/v1/inventory/reservation/release-expired";

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("已登录且有库存权限，但无服务签名：release-expired 直接 403")
  void releaseExpiredWithoutSignatureIsForbiddenEvenForLoggedInUser() throws Exception {
    authenticate(Set.of("inventory:reservation:release", "inventory:inventory:adjust-inventory"));

    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean proceeded = new AtomicBoolean(false);

    filter().doFilter(new MockHttpServletRequest("POST", RELEASE_EXPIRED_PATH),
        response, chain(proceeded));

    assertFalse(proceeded.get());
    assertEquals(403, response.getStatus());
    assertTrue(response.getContentAsString().contains("ACCESS_DENIED"));
    assertTrue(response.getContentAsString().contains("valid service signature"));
    assertEquals(1.0,
        meterRegistry.counter(InternalEndpointSignatureFilter.METRIC_REJECTED).count());
  }

  @Test
  @DisplayName("未登录、无签名：release-expired 同样 403")
  void releaseExpiredWithoutAuthenticationIsForbidden() throws Exception {
    SecurityContextHolder.clearContext();

    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean proceeded = new AtomicBoolean(false);

    filter().doFilter(new MockHttpServletRequest("POST", RELEASE_EXPIRED_PATH),
        response, chain(proceeded));

    assertFalse(proceeded.get());
    assertEquals(403, response.getStatus());
  }

  @Test
  @DisplayName("携带合法服务间 HMAC 签名：release-expired 放行")
  void releaseExpiredWithValidSignaturePasses() throws Exception {
    MockHttpServletRequest request = signedRequest(RELEASE_EXPIRED_PATH);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean proceeded = new AtomicBoolean(false);

    filter().doFilter(request, response, chain(proceeded));

    assertTrue(proceeded.get());
    assertEquals(200, response.getStatus());
    assertEquals(Boolean.TRUE,
        request.getAttribute(InternalEndpointSignatureFilter.SIGNATURE_VERIFIED_ATTRIBUTE));
    assertEquals(0.0,
        meterRegistry.counter(InternalEndpointSignatureFilter.METRIC_REJECTED).count());
  }

  @Test
  @DisplayName("矩阵参数不能绕过签名校验")
  void matrixParameterCannotBypassSignature() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean proceeded = new AtomicBoolean(false);

    filter().doFilter(
        new MockHttpServletRequest("POST", RELEASE_EXPIRED_PATH + ";x=1"),
        response, chain(proceeded));

    assertFalse(proceeded.get());
    assertEquals(403, response.getStatus());
  }

  private InternalEndpointSignatureFilter filter() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("security.signature.app-secrets.internal-service", SECRET);
    return new InternalEndpointSignatureFilter(
        new InternalSignatureVerifier(new InternalSignatureSecretResolver(environment)),
        new SecurityMetrics(meterRegistry),
        new ApiAccessControlProperties(),
        mock(PermissionService.class));
  }

  private static MockHttpServletRequest signedRequest(String uri) throws Exception {
    long timestamp = System.currentTimeMillis();
    String nonce = "nonce-" + UUID.randomUUID();
    String appId = "scm-order";
    MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
    request.addHeader("X-App-Id", appId);
    request.addHeader("X-Timestamp", Long.toString(timestamp));
    request.addHeader("X-Nonce", nonce);
    request.addHeader("X-Signature",
        hmacHex(timestamp + nonce + appId + uri));
    return request;
  }

  private static String hmacHex(String content) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
  }

  private static void authenticate(Set<String> permissions) {
    UUID userId = UUID.randomUUID();
    SecurityUser user = SecurityUser.builder()
        .userId(userId)
        .username("user-" + userId)
        .permissions(permissions)
        .build();
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
  }

  private static FilterChain chain(AtomicBoolean proceeded) {
    return (request, response) -> proceeded.set(true);
  }
}
