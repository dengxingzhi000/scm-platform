package com.scmcloud.audit.security;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.scmcloud.common.rest.interceptor.RestClientRequestSignatureInterceptor;
import com.scmcloud.common.rest.signature.InternalSignatureSecretResolver;
import com.scmcloud.common.security.metrics.SecurityMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditWriteRequestGuardTest {

    private static final String SECRET = "test-internal-secret";

    private InternalSignatureSecretResolver secretResolver;
    private SimpleMeterRegistry meterRegistry;
    private AuditWriteRequestGuard guard;

    @BeforeEach
    void setUp() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("API_SECRET_INTERNAL_SERVICE", SECRET);
        secretResolver = new InternalSignatureSecretResolver(environment);
        meterRegistry = new SimpleMeterRegistry();
        guard = new AuditWriteRequestGuard(secretResolver, new SecurityMetrics(meterRegistry));
    }

    @Test
    void signedInternalWriteShouldPassThrough() throws Exception {
        MockHttpServletRequest request = request("POST", "/sys-audit-log");
        sign(request, System.currentTimeMillis(), UUID.randomUUID().toString().replace("-", ""));
        MockFilterChain chain = new MockFilterChain();

        guard.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest(), "signed write should reach the controller");
    }

    @Test
    void requestSignedByTheSharedInterceptorShouldBeAccepted() throws Exception {
        RestClientRequestSignatureInterceptor signer =
                new RestClientRequestSignatureInterceptor(secretResolver);
        HttpHeaders headers = new HttpHeaders();
        HttpRequest signedRequest = mock(HttpRequest.class);
        when(signedRequest.getURI()).thenReturn(URI.create("http://scm-audit:8210/sys-audit-log"));
        when(signedRequest.getHeaders()).thenReturn(headers);
        when(signedRequest.getMethod()).thenReturn(HttpMethod.POST);

        signer.intercept(signedRequest, new byte[0], mock(ClientHttpRequestExecution.class));

        MockHttpServletRequest request = request("POST", "/sys-audit-log");
        headers.forEach((name, values) -> values.forEach(value -> request.addHeader(name, value)));
        MockFilterChain chain = new MockFilterChain();

        guard.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest(), "the shared signer and the guard must agree");
    }

    @Test
    void unsignedWriteShouldBeRejected() throws Exception {
        MockHttpServletRequest request = request("POST", "/sys-audit-log");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        guard.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void tamperedSignatureShouldBeRejected() throws Exception {
        MockHttpServletRequest request = request("POST", "/sys-audit-log");
        sign(request, System.currentTimeMillis(), "nonce-1");
        request.removeHeader("X-Signature");
        request.addHeader("X-Signature", "deadbeef");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
    }

    @Test
    void staleTimestampShouldBeRejected() throws Exception {
        MockHttpServletRequest request = request("POST", "/sys-audit-log");
        sign(request, System.currentTimeMillis() - 10 * 60 * 1000L, "nonce-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
    }

    @Test
    void readRequestsShouldNotBeFiltered() throws Exception {
        MockHttpServletRequest request = request("GET", "/sys-audit-log/page");
        MockFilterChain chain = new MockFilterChain();

        guard.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest(), "read endpoints stay open");
    }

    @Test
    void writeToAnotherPathShouldNotBeFiltered() throws Exception {
        MockHttpServletRequest request = request("POST", "/sys-sensitive-operation-log");
        MockFilterChain chain = new MockFilterChain();

        guard.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest(), "guard only covers the audit write path");
    }

    @Test
    void matrixParameterWriteShouldBeRejected() throws Exception {
        MockHttpServletRequest request = request("POST", "/sys-audit-log;x=1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        guard.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest(), "the matrix form must not slip past the signature check");
    }

    @Test
    void trailingSlashWriteShouldBeRejected() throws Exception {
        MockHttpServletRequest request = request("POST", "/sys-audit-log/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        guard.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest(), "the trailing slash form must not slip past the signature check");
    }

    @Test
    void encodedPathWriteShouldBeRejected() throws Exception {
        MockHttpServletRequest request = request("POST", "/%73ys-audit-log");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        guard.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest(), "the encoded form must not slip past the signature check");
    }

    @Test
    void signatureOfAnotherPathMustNotBeReplayableOnTheWritePath() throws Exception {
        MockHttpServletRequest request = request("POST", "/sys-audit-log;x=1");
        sign(request, System.currentTimeMillis(), "nonce-1", "/sys-audit-log");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        guard.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest(), "the signature is bound to the exact request target");
    }

    @Test
    void rejectedWriteShouldBeCounted() throws Exception {
        MockHttpServletRequest request = request("POST", "/sys-audit-log");
        request.addHeader("X-App-Id", secretResolver.resolveAppId());

        guard.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertEquals(1.0, meterRegistry.counter(AuditWriteRequestGuard.METRIC_REJECTED).count());
    }

    @Test
    void acceptedWriteShouldNotBeCounted() throws Exception {
        MockHttpServletRequest request = request("POST", "/sys-audit-log");
        sign(request, System.currentTimeMillis(), "nonce-1");

        guard.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertEquals(0.0, meterRegistry.counter(AuditWriteRequestGuard.METRIC_REJECTED).count());
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        return request;
    }

    /**
     * Signs exactly like {@code RestClientRequestSignatureInterceptor} does, using the same secret
     * resolution: a drift between signer and verifier would fail this test.
     */
    private void sign(MockHttpServletRequest request, long timestamp, String nonce) {
        sign(request, timestamp, nonce, request.getRequestURI());
    }

    private void sign(MockHttpServletRequest request, long timestamp, String nonce, String signedUri) {
        String appId = secretResolver.resolveAppId();
        String secret = secretResolver.resolveSecret();
        String content = timestamp + nonce + appId + signedUri;
        String signature = SecureUtil.hmac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8))
                .digestHex(content);
        request.addHeader("X-Timestamp", String.valueOf(timestamp));
        request.addHeader("X-Nonce", nonce);
        request.addHeader("X-App-Id", appId);
        request.addHeader("X-Signature", signature);
    }
}
