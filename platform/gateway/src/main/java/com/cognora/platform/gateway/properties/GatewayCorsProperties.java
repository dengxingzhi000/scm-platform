package com.cognora.platform.gateway.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Browser CORS settings for gateway-direct mode.
 *
 * <p>Values are origin <em>patterns</em> so that {@code http://localhost:*} covers
 * every local development port. Add the deployed SPA origin (for example
 * {@code http://192.168.0.121:3000}) to
 * {@code cognora.gateway.cors.allowed-origins} instead of editing the code.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "cognora.gateway.cors")
public class GatewayCorsProperties {

    /**
     * Allowed browser origins (Spring {@code CorsConfiguration} origin patterns).
     */
    private List<String> allowedOrigins = List.of("http://localhost:*", "http://192.168.0.121:3000");
}
