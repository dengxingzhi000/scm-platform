package com.scmcloud.analytics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the ClickHouse connection.
 * <p>
 * Mirrors keys under {@code clickhouse.*} in {@code application.yml}.
 */
@Data
@ConfigurationProperties(prefix = "clickhouse")
public class ClickHouseProperties {
    private String url;
    private String database;
    private String username;
    private String password;
}
