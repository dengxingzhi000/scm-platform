package com.cognora.platform.audit.security;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuditWriteRequestGuardTest {

    private static final String SECRET = "test-internal-secret";
    private static final String APP_ID = "internal-service";

    private AuditWriteRequestGuard guard;

    @BeforeEach
    void setUp() {
        guard = new AuditWriteRequestGuard();
        ReflectionTestUtils.setField(guard, "secretKey", SECRET);
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
    void contextPathPrefixedWriteShouldStillBeGuarded() throws Exception {
        MockHttpServletRequest request = request("POST", "/audit/sys-audit-log");
        request.setContextPath("/audit");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
    }

    @Test
    void contextPathDeploymentShouldAcceptSignatureOverClientPath() throws Exception {
        MockHttpServletRequest request = request("POST", "/audit/sys-audit-log");
        request.setContextPath("/audit");
        sign(request, System.currentTimeMillis(), "nonce-1", "/sys-audit-log");
        MockFilterChain chain = new MockFilterChain();

        guard.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest(), "client signs the URI path without the context path");
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

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        return request;
    }

    private void sign(MockHttpServletRequest request, long timestamp, String nonce) {
        sign(request, timestamp, nonce, request.getRequestURI());
    }

    private void sign(MockHttpServletRequest request, long timestamp, String nonce, String path) {
        String content = timestamp + nonce + APP_ID + path;
        String signature = SecureUtil.hmac(HmacAlgorithm.HmacSHA256, SECRET.getBytes(StandardCharsets.UTF_8))
                .digestHex(content);
        request.addHeader("X-Timestamp", String.valueOf(timestamp));
        request.addHeader("X-Nonce", nonce);
        request.addHeader("X-App-Id", APP_ID);
        request.addHeader("X-Signature", signature);
    }
}
