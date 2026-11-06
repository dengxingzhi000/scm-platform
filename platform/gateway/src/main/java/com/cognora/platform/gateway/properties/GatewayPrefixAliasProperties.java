package com.cognora.platform.gateway.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Switch for the transitional legacy-prefix aliases.
 *
 * <p>Disabled by default. Enabling it while any target service still registers
 * under its old name makes the alias shadow the discovery-locator route on the
 * same path (aliases use {@code order: -1}) and return 503, because the alias
 * targets the canonical {@code lb://platform-*} id.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "cognora.gateway.prefix-aliases")
public class GatewayPrefixAliasProperties {

    /**
     * When false, no alias route definition is registered at all and legacy paths
     * keep resolving through the discovery locator, exactly as before.
     */
    private boolean enabled = false;
}
