package com.cognora.platform.common.data.rw.config;

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

    /**
     * Target used when neither the annotation nor the SQL layer made a decision
     * (current() == AUTO). Master is the safe default: an unannotated path can
     * never silently read a stale replica, including async threads that lost the
     * routing context.
     */
    private RoutingTarget defaultTarget = RoutingTarget.MASTER;

    private Duration replicationLagTolerance = Duration.ofSeconds(1);

    private Duration readMasterAfterWrite = Duration.ofSeconds(2);

    private boolean healthCheckEnabled = true;

    private Duration healthCheckInterval = Duration.ofSeconds(30);

    /**
     * Mark a slave unavailable on the first failed connection probe. When false the
     * slave stays eligible until the periodic health check removes it.
     */
    private boolean failFastOnSlaveConnectionFailure = true;

    /**
     * Timeout of the routing-level {@code Connection.isValid()} probe.
     */
    @Min(value = 1, message = "Validation timeout must be greater than 0")
    private int validationTimeoutSeconds = 3;

    @Valid
    private CircuitBreakerProperties circuitBreaker = new CircuitBreakerProperties();

    @Data
    public static class CircuitBreakerProperties {

        /**
         * Circuit breaking is executed through Sentinel when enabled. When disabled the
         * routing datasource still falls back to master on the first failed probe.
         */
        private boolean enabled = true;

        /**
         * Max acceptable connection-hold time in milliseconds. Requests slower than this
         * count towards {@code slow-ratio-threshold}.
         */
        @Min(value = 1, message = "Slow RT must be greater than 0")
        private long slowRtMs = 500;

        @DecimalMin(value = "0.0", message = "Slow ratio threshold must be >= 0")
        @DecimalMax(value = "1.0", message = "Slow ratio threshold must be <= 1")
        private double slowRatioThreshold = 0.5;

        @DecimalMin(value = "0.0", message = "Error ratio must be >= 0")
        @DecimalMax(value = "1.0", message = "Error ratio must be <= 1")
        private double errorRatio = 0.5;

        @Min(value = 1, message = "Time window must be greater than 0")
        private int timeWindowSeconds = 30;

        @Min(value = 1, message = "Min request amount must be greater than 0")
        private int minRequestAmount = 10;

        @Min(value = 100, message = "Stat interval must be at least 100ms")
        private int statIntervalMs = 10000;
    }

    /**
     * Whether an unavailable slave should turn the whole actuator health endpoint
     * DOWN. Default false: reads fall back to master, so the service is still able
     * to serve traffic and a replica outage must not trigger a pod restart.
     */
    private boolean slaveDownAffectsHealth = false;

    @Min(value = 1, message = "Failure threshold must be greater than 0")
    @Max(value = 100, message = "Failure threshold must not exceed 100")
    private int failureThreshold = 3;

    @Min(value = 0, message = "Slave retry count must not be negative")
    @Max(value = 10, message = "Slave retry count must not exceed 10")
    private int slaveRetryCount = 3;

    @Data
    @Valid
    public static class DataSourceGroup {

        @Valid
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
        private String connectionTestQuery;

        /**
         * HikariCP initializationFailTimeout in milliseconds. Negative means the pool
         * starts in the background and never fails the application context, which is
         * required for an unreachable replica at startup. Positive values fail fast.
         */
        private long initializationFailTimeout = -1;
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

    public enum RoutingTarget {
        MASTER,
        SLAVE
    }
}
