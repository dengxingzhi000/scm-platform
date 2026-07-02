package com.scmcloud.gateway.filter;

import com.scmcloud.gateway.filter.support.IpAccessDecision;
import com.scmcloud.gateway.properties.IpAccessControlProperties;
import com.scmcloud.gateway.support.ip.ClientIpResolver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IpAccessControlFilter Tests")
class IpAccessControlFilterTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private Counter allowedCounter;

    @Mock
    private Counter blockedCounter;

    @InjectMocks
    private IpAccessControlFilter filter;

    private IpAccessControlProperties properties;
    private MockServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        properties = new IpAccessControlProperties();
        properties.setEnabled(true);
        properties.setWhitelistOnly(false);
        properties.setCacheTtl(Duration.ofMinutes(1));
        properties.setTrustedProxies(List.of("127.0.0.1/32", "10.0.0.0/8"));
        properties.setForwardedHeaders(List.of("X-Forwarded-For"));
        properties.setBlockMessage("Access denied");

        // Re-create filter with fresh properties
        filter = new IpAccessControlFilter(redisTemplate, properties, meterRegistry);

        when(meterRegistry.counter("gateway.ip.access.allowed")).thenReturn(allowedCounter);
        when(meterRegistry.counter("gateway.ip.access.blocked")).thenReturn(blockedCounter);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new InetSocketAddress(
                        InetAddress.getLoopbackAddress(), 12345))
                .build();
        exchange = MockServerWebExchange.from(request);

        when(chain.filter(exchange)).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Should skip filter when disabled")
    void testFilter_Disabled() {
        properties.setEnabled(false);
        filter = new IpAccessControlFilter(redisTemplate, properties, meterRegistry);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("Should allow non-blacklisted IP when not in whitelist-only mode")
    void testFilter_AllowNonBlacklisted() {
        when(redisTemplate.hasKey("security:ip:blacklist:127.0.0.1"))
                .thenReturn(Mono.just(false));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(allowedCounter).increment();
        verify(blockedCounter, never()).increment();
    }

    @Test
    @DisplayName("Should block blacklisted IP")
    void testFilter_BlockBlacklisted() {
        when(redisTemplate.hasKey("security:ip:blacklist:127.0.0.1"))
                .thenReturn(Mono.just(true));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, never()).filter(any());
        verify(blockedCounter).increment();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should block non-whitelisted IP in whitelist-only mode")
    void testFilter_WhitelistOnly_NotWhitelisted() {
        properties.setWhitelistOnly(true);
        filter = new IpAccessControlFilter(redisTemplate, properties, meterRegistry);
        when(meterRegistry.counter("gateway.ip.access.blocked")).thenReturn(blockedCounter);

        when(redisTemplate.hasKey("security:ip:blacklist:127.0.0.1"))
                .thenReturn(Mono.just(false));
        when(redisTemplate.hasKey("security:ip:whitelist:127.0.0.1"))
                .thenReturn(Mono.just(false));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, never()).filter(any());
        verify(blockedCounter).increment();
    }

    @Test
    @DisplayName("Should allow whitelisted IP in whitelist-only mode")
    void testFilter_WhitelistOnly_Whitelisted() {
        properties.setWhitelistOnly(true);
        filter = new IpAccessControlFilter(redisTemplate, properties, meterRegistry);
        when(meterRegistry.counter("gateway.ip.access.allowed")).thenReturn(allowedCounter);

        when(redisTemplate.hasKey("security:ip:blacklist:127.0.0.1"))
                .thenReturn(Mono.just(false));
        when(redisTemplate.hasKey("security:ip:whitelist:127.0.0.1"))
                .thenReturn(Mono.just(true));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(allowedCounter).increment();
    }

    @Test
    @DisplayName("Should use cached decision on second request")
    void testFilter_CacheHit() {
        when(redisTemplate.hasKey("security:ip:blacklist:127.0.0.1"))
                .thenReturn(Mono.just(false));

        // First request - populates cache
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Second request - should use cache, no Redis call
        MockServerHttpRequest request2 = MockServerHttpRequest
                .get("/api/test2")
                .remoteAddress(new InetSocketAddress(
                        InetAddress.getLoopbackAddress(), 12345))
                .build();
        MockServerWebExchange exchange2 = MockServerWebExchange.from(request2);
        when(chain.filter(exchange2)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange2, chain))
                .verifyComplete();

        // Redis should only be called once (for first request)
        verify(redisTemplate, times(1)).hasKey(anyString());
        verify(chain, times(2)).filter(any());
    }

    @Test
    @DisplayName("Should resolve client IP from X-Forwarded-For when proxy is trusted")
    void testFilter_ProxiedRequest() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new InetSocketAddress(
                        InetAddress.getLoopbackAddress(), 12345))
                .header("X-Forwarded-For", "192.168.1.100")
                .build();
        MockServerWebExchange proxiedExchange = MockServerWebExchange.from(request);

        when(redisTemplate.hasKey("security:ip:blacklist:192.168.1.100"))
                .thenReturn(Mono.just(false));
        when(chain.filter(proxiedExchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(proxiedExchange, chain))
                .verifyComplete();

        verify(redisTemplate).hasKey("security:ip:blacklist:192.168.1.100");
        verify(chain).filter(proxiedExchange);
    }

    @Test
    @DisplayName("Should return 403 with structured JSON on block")
    void testFilter_BlockResponseJson() {
        when(redisTemplate.hasKey("security:ip:blacklist:127.0.0.1"))
                .thenReturn(Mono.just(true));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getHeaders().getContentType())
                .hasToString("application/json");
    }

    @Test
    @DisplayName("Should have correct order")
    void testGetOrder() {
        assertThat(filter.getOrder()).isEqualTo(-100);
    }
}
