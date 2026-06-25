package com.scmcloud.common.data.rw.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-write separation configuration properties.
 *
 * @author Deng
 * @since 2025-12-16
 */
@Data
@Validated
@ConfigurationProperties(prefix = "spring.datasource.rw")
public class ReadWriteProperties {

    private boolean enabled = false;

    @Valid
    private Map<String, DataSourceGroup> groups = new HashMap<>();

    private LoadBalanceType loadBalance = LoadBalanceType.ROUND_ROBIN;

    private Duration replicationLagTolerance = Duration.ofSeconds(1);

    private Duration readMasterAfterWrite = Duration.ofSeconds(2);

    private boolean healthCheckEnabled = true;

    private Duration healthCheckInterval = Duration.ofSeconds(30);

    @Min(value = 1, message = "Failure threshold must be greater than 0")
    @Max(value = 100, message = "Failure threshold must not exceed 100")
    private int failureThreshold = 3;

    @Min(value = 0, message = "Slave retry count must not be negative")
    @Max(value = 10, message = "Slave retry count must not exceed 10")
    private int slaveRetryCount = 3;

    @Data
    @Valid
    public static class DataSourceGroup {

        @NotNull(message = "Master datasource config must not be null")
        private DataSourceConfig master;

        @Valid
        private List<SlaveDataSourceConfig> slaves = new ArrayList<>();

        private boolean slavesEnabled = true;

        private LoadBalanceType loadBalance;
    }

    @Data
    public static class DataSourceConfig {

        @NotBlank(message = "Datasource URL must not be blank")
        private String url;

        @NotBlank(message = "Username must not be blank")
        private String username;

        private String password;

        private String driverClassName = "org.postgresql.Driver";

        @Min(value = 1, message = "Minimum idle must be greater than 0")
        private int minimumIdle = 5;

        @Min(value = 1, message = "Maximum pool size must be greater than 0")
        @Max(value = 1000, message = "Maximum pool size must not exceed 1000")
        private int maximumPoolSize = 20;

        private Duration connectionTimeout = Duration.ofSeconds(30);
        private Duration idleTimeout = Duration.ofMinutes(10);
        private Duration maxLifetime = Duration.ofMinutes(30);
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SlaveDataSourceConfig extends DataSourceConfig {

        @NotBlank(message = "Slave name must not be blank")
        private String name = "slave";

        @Min(value = 1, message = "Weight must be greater than 0")
        @Max(value = 100, message = "Weight must not exceed 100")
        private int weight = 1;

        private boolean available = true;
    }

    public enum LoadBalanceType {
        ROUND_ROBIN,
        WEIGHTED_ROUND_ROBIN,
        RANDOM,
        WEIGHTED_RANDOM,
        LEAST_CONNECTIONS
    }
}
