package com.scmcloud.gateway.filter;

import com.scmcloud.gateway.properties.ApiSignatureProperties;
import com.scmcloud.gateway.util.CachedBodyRequestDecorator;
import com.scmcloud.gateway.util.SignatureAlgorithm;
import com.scmcloud.gateway.util.SignatureAlgorithmRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * API signature validation filter with configurable replay protection.
 * <p>
 * Verification flow: whitelist → header extraction → parameter validation →
 * timestamp freshness → nonce replay check (Redis) → HMAC verification →
 * nonce storage → forward.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiSignatureFilter implements GlobalFilter, Ordered {

    private static final DefaultDataBufferFactory BUFFER_FACTORY = new DefaultDataBufferFactory();
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final SignatureAlgorithmRegistry algorithmRegistry;
    private final ApiSignatureProperties properties;
    private final MeterRegistry meterRegistry;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange,@NonNull GatewayFilterChain chain) {
        meterRegistry.counter("gateway.signature.requests").increment();
        if (!properties.isEnabled() || isWhitelisted(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }
        return cacheRequestBody(exchange)
                .flatMap(cached -> validateAndForward(cached, chain));
    }

    // ── request body caching ──────────────────────────────────────────

    private Mono<ServerWebExchange> cacheRequestBody(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        if (request instanceof CachedBodyRequestDecorator) {
            return Mono.just(exchange);
        }
        return DataBufferUtils.join(request.getBody())
                .defaultIfEmpty(BUFFER_FACTORY.wrap(new byte[0]))
                .map(buffer -> {
                    try {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        return exchange.mutate()
                                .request(new CachedBodyRequestDecorator(request, bytes))
                                .build();
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                });
    }

    // ── signature validation pipeline ─────────────────────────────────

    private Mono<Void> validateAndForward(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getId();
        String path = request.getURI().getPath();

        // 1. extract & validate required headers
        String timestamp = request.getHeaders().getFirst("X-Timestamp");
        String nonce = request.getHeaders().getFirst("X-Nonce");
        String signature = request.getHeaders().getFirst("X-Signature");
        String appId = request.getHeaders().getFirst("X-App-Id");
        String version = request.getHeaders().getFirst("X-Sign-Version");

        if (isBlank(timestamp) || isBlank(nonce) || isBlank(signature) || isBlank(appId)) {
            return unauthorized(exchange, "MISSING_PARAMETERS", "Missing signature parameters");
        }

        // 2. timestamp freshness
        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return unauthorized(exchange, "INVALID_TIMESTAMP", "Invalid timestamp");
        }
        if (Math.abs(System.currentTimeMillis() - requestTime) > properties.getAllowedClockSkew().toMillis()) {
            return unauthorized(exchange, "REQUEST_EXPIRED", "Request expired");
        }

        // 3. resolve secret key
        String secretKey = properties.getAppSecrets().get(appId);
        if (secretKey == null) {
            meterRegistry.counter("gateway.signature.invalid_app").increment();
            log.warn("Unknown appId traceId={} appId={} path={}", traceId, appId, path);
            return unauthorized(exchange, "INVALID_APP_ID", "Invalid appId");
        }

        // 4. nonce replay check (Redis) → verify signature → store nonce → forward
        String nonceKey = properties.getNonceKeyPrefix() + appId + ":" + nonce;
        String resolvedVersion = isBlank(version) ? properties.getDefaultVersion() : version;
        SignatureAlgorithm algorithm = algorithmRegistry.getAlgorithm(resolvedVersion);

        return redisTemplate.hasKey(nonceKey)
                .flatMap(exists -> {
                    if (exists) {
                        meterRegistry.counter("gateway.signature.replay").increment();
                        log.warn("Replay detected traceId={} appId={} path={}", traceId, appId, path);
                        return unauthorized(exchange, "REPLAY", "Replay detected");
                    }

                    return verifyAndForward(algorithm, request, signature, appId, timestamp, nonce,
                            secretKey, nonceKey, traceId, path, chain, exchange);
                })
                .onErrorResume(e -> {
                    meterRegistry.counter("gateway.signature.errors").increment();
                    log.error("Signature validation error traceId={}", traceId, e);
                    return unauthorized(exchange, "INTERNAL_ERROR", "Signature validation error");
                });
    }

    private Mono<Void> verifyAndForward(SignatureAlgorithm algorithm,
                                        ServerHttpRequest request,
                                        String signature, String appId,
                                        String timestamp, String nonce,
                                        String secretKey, String nonceKey,
                                        String traceId, String path,
                                        GatewayFilterChain chain,
                                        ServerWebExchange exchange) {
        long start = System.currentTimeMillis();

        return algorithm.verify(request, signature, appId, timestamp, nonce, secretKey)
                .flatMap(valid -> {
                    recordVerificationMetrics(start, traceId, path, valid);

                    if (!valid) {
                        return unauthorized(exchange, "SIGNATURE_INVALID", "Signature verification failed");
                    }

                    return redisTemplate.opsForValue()
                            .set(nonceKey, "1", properties.getNonceTtl())
                            .then(chain.filter(exchange));
                });
    }

    // ── metrics ───────────────────────────────────────────────────────

    private void recordVerificationMetrics(long start, String traceId, String path, boolean valid) {
        long duration = System.currentTimeMillis() - start;
        meterRegistry.timer("gateway.signature.verification_duration")
                .record(Duration.ofMillis(duration));

        if (duration > 100) {
            meterRegistry.counter("gateway.signature.slow_verification").increment();
            log.warn("Slow signature verification: {}ms traceId={} path={}", duration, traceId, path);
        }
        if (!valid) {
            meterRegistry.counter("gateway.signature.invalid").increment();
            log.warn("Signature verification failed traceId={} path={}", traceId, path);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────

    private boolean isWhitelisted(String path) {
        return properties.getWhitelist().stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String code, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"code\":401,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                escapeJson(code), escapeJson(message),
                escapeJson(exchange.getRequest().getURI().getPath()));

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
