package com.cognora.lms.logistics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 承运商轨迹回调以 HMAC-SHA256 签名鉴权，不携带平台 JWT。
 * 该链只匹配回调路径并先于公共 JWT 链执行，其余接口仍走公共鉴权规则。
 */
@Configuration
public class TmsCallbackSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain tmsCallbackSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/v1/tms-tracking/callback/**")
                .csrf(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
