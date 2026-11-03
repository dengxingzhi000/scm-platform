package com.scmcloud.common.sentinel.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.monitoring.config.MonitoringProperties;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.sentinel.exception.SentinelExceptionHandlerStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(MonitoringProperties.class)
@RequiredArgsConstructor
public class SentinelAutoConfiguration {
    private final MonitoringProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public BlockExceptionHandler blockExceptionHandler(ObjectMapper objectMapper,
                                                       List<SentinelExceptionHandlerStrategy> strategies) {

        return (request, response, e) -> {
            ApiResponse<Void> result = strategies.stream()
                    .filter(strategy -> strategy.supports(e))
                    .findFirst()
                    .map(strategy -> strategy.handle(e))
                    .orElse(ApiResponse.fail(429, "Too many requests"));
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), result);
        };
    }

    @Bean
    public SentinelRuleRefresher sentinelRuleRefresher() {
        return new SentinelRuleRefresher(properties);
    }
}
