package com.cognora.ecom.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Payment-channel callbacks are authenticated by the channel signature, not by
 * a platform JWT. This chain matches only the callback paths and runs before the
 * shared JWT-protected chain; every other request keeps the common rules.
 */
@Configuration
public class PaymentCallbackSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain paymentCallbackSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/v1/payments/callback/**")
                .csrf(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
