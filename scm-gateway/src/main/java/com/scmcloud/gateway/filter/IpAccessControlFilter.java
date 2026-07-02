package com.scmcloud.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.scmcloud.gateway.filter.support.IpAccessDecision;
import com.scmcloud.gateway.properties.IpAccessControlProperties;
import com.scmcloud.gateway.support.ip.ClientIpResolver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 访问控制过滤器
 *
 * <p>功能：
 * <ul>
 *   <li>基于 Redis 的 IP 黑白名单（动态更新，无需重启）</li>
 *   <li>可信代理验证，防止 X-Forwarded-For 伪造</li>
 *   <li>本地缓存，减少 Redis 查询</li>
 *   <li>结构化 JSON 错误响应</li>
 *   <li>Metrics 埋点，支持 Prometheus 监控</li>
 * </ul>
 *
 * @author deng
 * @version 4.0
 */
@Component
@Slf4j
public class IpAccessControlFilter implements GlobalFilter, Ordered {
    private static final String IP_WHITELIST_KEY = "security:ip:whitelist:";
    private static final String IP_BLACKLIST_KEY = "security:ip:blacklist:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final IpAccessControlProperties properties;
    private final ClientIpResolver clientIpResolver;

    private final ConcurrentHashMap<String, CacheEntry> decisionCache = new ConcurrentHashMap<>();
    private final long cacheTtlMillis;
    private final long cacheMaxSize;

    private final Counter allowedCounter;
    private final Counter blockedCounter;

    public IpAccessControlFilter(ReactiveRedisTemplate<String, String> redisTemplate,
                                 IpAccessControlProperties properties,
                                 MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.clientIpResolver = new ClientIpResolver(properties);
        this.cacheTtlMillis = resolveTtl(properties.getCacheTtl());
        this.cacheMaxSize = Math.max(1000, properties.getCacheMaxSize());
        this.allowedCounter = meterRegistry.counter("gateway.ip.access.allowed");
        this.blockedCounter = meterRegistry.counter("gateway.ip.access.blocked");
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        String clientIp = clientIpResolver.resolve(exchange);
        if (!StringUtils.hasText(clientIp)) {
            log.warn("Unable to resolve client IP for {}", exchange.getRequest().getURI());
            blockedCounter.increment();
            return blockRequest(exchange, "UNRESOLVED_IP", clientIp);
        }

        IpAccessDecision cachedDecision = getCachedDecision(clientIp);
        if (cachedDecision != null) {
            return handleDecision(cachedDecision, clientIp, exchange, chain);
        }

        return evaluateAccess(clientIp)
                .doOnNext(decision -> putDecision(clientIp, decision))
                .flatMap(decision -> handleDecision(decision, clientIp, exchange, chain));
    }

    private Mono<IpAccessDecision> evaluateAccess(String clientIp) {
        return redisTemplate.hasKey(IP_BLACKLIST_KEY + clientIp)
                .flatMap(inBlacklist -> {
                    if (inBlacklist) {
                        return Mono.just(IpAccessDecision.deny("BLACKLIST"));
                    }
                    if (!properties.isWhitelistOnly()) {
                        return Mono.just(IpAccessDecision.allow());
                    }
                    return redisTemplate.hasKey(IP_WHITELIST_KEY + clientIp)
                            .map(inWhitelist -> inWhitelist
                                    ? IpAccessDecision.allow()
                                    : IpAccessDecision.deny("WHITELIST_ONLY"));
                })
                .defaultIfEmpty(properties.isWhitelistOnly()
                        ? IpAccessDecision.deny("WHITELIST_ONLY")
                        : IpAccessDecision.allow());
    }

    private Mono<Void> handleDecision(IpAccessDecision decision,
                                      String clientIp,
                                      ServerWebExchange exchange,
                                      GatewayFilterChain chain) {
        if (decision.allowed()) {
            allowedCounter.increment();
            return chain.filter(exchange);
        }
        blockedCounter.increment();
        log.warn("Blocked client IP {} by reason {}", clientIp, decision.reason());
        return blockRequest(exchange, decision.reason(), clientIp);
    }

    private Mono<Void> blockRequest(ServerWebExchange exchange, String reason, String clientIp) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().setCacheControl("no-store");

        String message = StringUtils.hasText(properties.getBlockMessage())
                ? properties.getBlockMessage()
                : "Request blocked";

        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", String.valueOf(HttpStatus.FORBIDDEN.value()));
        body.put("message", message);
        body.put("reason", reason);
        body.put("ip", clientIp != null ? clientIp : "");
        String json = JSON.toJSONString(body);

        return response.writeWith(Mono.just(
                response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8))
        ));
    }

    private IpAccessDecision getCachedDecision(String clientIp) {
        CacheEntry entry = decisionCache.get(clientIp);
        if (entry != null && !entry.isExpired()) {
            return entry.decision;
        }
        if (entry != null) {
            decisionCache.remove(clientIp);
        }
        return null;
    }

    private void putDecision(String clientIp, IpAccessDecision decision) {
        if (decisionCache.size() >= cacheMaxSize) {
            decisionCache.entrySet().removeIf(e -> e.getValue().isExpired());
        }
        decisionCache.put(clientIp, CacheEntry.of(decision, cacheTtlMillis));
    }

    private long resolveTtl(java.time.Duration ttl) {
        long millis = ttl != null ? ttl.toMillis() : java.time.Duration.ofMinutes(1).toMillis();
        return Math.max(millis, 1000);
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private record CacheEntry(IpAccessDecision decision, long expireAt) {
        static CacheEntry of(IpAccessDecision decision, long ttlMillis) {
            return new CacheEntry(decision, System.currentTimeMillis() + ttlMillis);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
