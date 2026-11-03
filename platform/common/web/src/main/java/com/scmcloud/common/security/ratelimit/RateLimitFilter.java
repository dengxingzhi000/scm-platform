package com.scmcloud.common.security.ratelimit;

import com.scmcloud.common.redis.script.RedisLuaScriptLoader;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RateLimitFilter implements Filter {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_REQUESTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private static final RedisScript<Long> RATE_LIMIT_SCRIPT =
            RedisLuaScriptLoader.load("lua/rate-limit/sliding_window.lua", Long.class);

    public RateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        if (path.startsWith("/api/v1/auth/")) {
            String clientIp = getClientIP(httpRequest);
            String key = "rate_limit:" + path + ":" + clientIp;

            Long result = redisTemplate.execute(RATE_LIMIT_SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(MAX_REQUESTS),
                    String.valueOf(WINDOW.toMillis()),
                    String.valueOf(System.currentTimeMillis()),
                    UUID.randomUUID().toString());

            if (result != null && result == 0L) {
                httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                httpResponse.getWriter().write("{\"code\":429,\"message\":\"Too many requests\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
