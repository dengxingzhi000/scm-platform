package com.scmcloud.common.security.config;

import com.scmcloud.common.security.filter.JwtAuthenticationFilter;
import com.scmcloud.common.security.filter.SqlInjectionFilter;
import com.scmcloud.common.security.handler.JwtAccessDeniedHandler;
import com.scmcloud.common.security.handler.JwtAuthenticationEntryPoint;
import com.scmcloud.common.security.stepup.StepUpFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.Customizer;
import org.springframework.util.StringUtils;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * SpringSecurity 閰嶇疆锟?
 *
 * @author Deng
 * createData 2025/10/11 10:37
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SqlInjectionFilter sqlInjectionFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    @Autowired(required = false)
    private StepUpFilter stepUpFilter;
    private final SecurityHeadersProperties securityHeadersProperties;

    /**
     * Spring Security 涓昏繃婊ゅ櫒锟?
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager)
            throws Exception {
        http
                // 1锔忊儯 绂佺敤 CSRF锛堜娇锟絁WT锟?
                .csrf(AbstractHttpConfigurer::disable)

                // 2锔忊儯 CORS 閰嶇疆
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3锔忊儯 鏃犵姸锟絊ession 绠$悊
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(true))

                // 4锔忊儯 寮傚父澶勭悊锛堣璇佷笌鎺堟潈锟?
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))

                // 5锔忊儯 瀹夊叏澶撮厤锟?
                // 瀹夊叏澶撮厤锟?
                .headers(headers -> {
                    if (!securityHeadersProperties.isEnabled()) {
                        return;
                    }
                    if (securityHeadersProperties.isHstsEnabled()) {
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(securityHeadersProperties.isHstsIncludeSubdomains())
                                .maxAgeInSeconds(securityHeadersProperties.getHstsMaxAgeSeconds()));
                    } else {
                        headers.httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable);
                    }

                    if (securityHeadersProperties.isFrameOptionsEnabled()) {
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
                    } else {
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable);
                    }

                    if (securityHeadersProperties.isContentTypeOptionsEnabled()) {
                        headers.contentTypeOptions(Customizer.withDefaults());
                    } else {
                        headers.contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable);
                    }

                    headers.xssProtection(HeadersConfigurer.XXssConfig::disable);

                    headers.contentSecurityPolicy(csp -> csp
                            .policyDirectives("default-src 'self'; " +
                                    "script-src 'self' https://cdn.jsdelivr.net; " +
                                    "style-src 'self' 'unsafe-inline'; " +
                                    "img-src 'self' data: https:; " +
                                    "frame-ancestors 'none'"));

                    if (securityHeadersProperties.isReferrerPolicyEnabled()) {
                        headers.referrerPolicy(referrer -> referrer.policy(
                                resolveReferrerPolicy(securityHeadersProperties.getReferrerPolicy())));
                    }
                })

                // 6锔忊儯 鎺堟潈瑙勫垯
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/public/**"
                        ).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())

                // 7锔忊儯 鎸囧畾璁よ瘉绠$悊鍣紙鏇夸唬 DaoAuthenticationProvider锟?
                .authenticationManager(authenticationManager)

                // 8锔忊儯 娣诲姞鑷畾涔夎繃婊ゅ櫒
                .addFilterBefore(sqlInjectionFilter, LogoutFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (stepUpFilter != null) {
            http.addFilterAfter(stepUpFilter, JwtAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "https://*.bank.com",
                "https://*.nearsync.com",
                "http://localhost:*"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Tenant-Id", "X-Request-ID", "X-Timestamp", "X-Nonce", "X-Signature", "X-App-Id", "X-Sign-Version"));
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Total-Count",
                "X-Request-ID",
                "X-StepUp-Required"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    private ReferrerPolicyHeaderWriter.ReferrerPolicy resolveReferrerPolicy(String configured) {
        ReferrerPolicyHeaderWriter.ReferrerPolicy defaultPolicy =
                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN;
        if (!StringUtils.hasText(configured)) {
            return defaultPolicy;
        }
        String normalized = configured.trim().toUpperCase().replace('-', '_');
        try {
            return ReferrerPolicyHeaderWriter.ReferrerPolicy.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return defaultPolicy;
        }
    }
}
