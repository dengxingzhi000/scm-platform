package com.cognora.platform.gateway.config;

import com.cognora.platform.gateway.properties.GatewayCorsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayCorsConfigTest {

    @Test
    void defaultOriginsCoverLocalDevAndVmSpa() {
        assertThat(new GatewayCorsProperties().getAllowedOrigins())
                .containsExactly("http://localhost:*", "http://192.168.0.121:3000");
    }

    @Test
    void corsSourceAllowsConfiguredOriginsOnly() {
        CorsConfigurationSource source = new GatewayCorsConfig()
                .corsConfigurationSource(new GatewayCorsProperties());
        CorsConfiguration configuration = source.getCorsConfiguration(
                MockServerWebExchange.from(MockServerHttpRequest.post("/scm-order/api/v1/orders")));

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("http://192.168.0.121:3000"))
                .isEqualTo("http://192.168.0.121:3000");
        assertThat(configuration.checkOrigin("http://localhost:3000")).isEqualTo("http://localhost:3000");
        assertThat(configuration.checkOrigin("http://attacker.example.com")).isNull();
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.getAllowedMethods())
                .contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD");
    }
}
