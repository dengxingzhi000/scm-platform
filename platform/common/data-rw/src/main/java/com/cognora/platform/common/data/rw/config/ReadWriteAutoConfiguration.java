package com.cognora.platform.common.data.rw.config;

import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAssistConfiguration;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration;
import com.cognora.platform.common.data.rw.aop.ReadWriteRoutingAspect;
import com.cognora.platform.common.data.rw.async.ReadWriteTaskDecorator;
import com.cognora.platform.common.data.rw.circuitbreaker.SlaveCircuitBreaker;
import com.cognora.platform.common.data.rw.dynamic.CountingDataSource;
import com.cognora.platform.common.data.rw.dynamic.DynamicDataSourceRefresher;
import com.cognora.platform.common.data.rw.endpoint.ReadWriteEndpoint;
import com.cognora.platform.common.data.rw.health.ReadWriteHealthIndicator;
import com.cognora.platform.common.data.rw.health.SlaveHealthChecker;
import com.cognora.platform.common.data.rw.loadbalance.*;
import com.cognora.platform.common.data.rw.metrics.ConnectionPoolMetrics;
import com.cognora.platform.common.data.rw.routing.ReadWriteRoutingDataSource;
import com.cognora.platform.common.data.rw.sql.SqlRoutingInterceptor;
import com.cognora.platform.common.data.rw.tracing.ReadWriteTracingInterceptor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Read-write separation auto-configuration.
 *
 * @author Deng
 * @since 2025-12-16
 */
@AutoConfiguration(before = {
        DynamicDataSourceAssistConfiguration.class,
        DynamicDataSourceAutoConfiguration.class,
        DataSourceAutoConfiguration.class
})
@EnableConfigurationProperties(ReadWriteProperties.class)
@ConditionalOnProperty(
        prefix = "spring.datasource.rw",
        name = "enabled",
        havingValue = "true"
)
@EnableScheduling
public class ReadWriteAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ReadWriteAutoConfiguration.class);

    private final ReadWriteProperties properties;
    private final MeterRegistry meterRegistry;
    private final Environment environment;

    private final List<HikariDataSource> allDataSources = new ArrayList<>();
    private final Map<String, ReadWriteRoutingDataSource> routingDataSources = new ConcurrentHashMap<>();
    private final Map<String, Map<String, DataSource>> slaveDataSourcesMap = new ConcurrentHashMap<>();

    private final SlaveCircuitBreaker circuitBreaker;
    private final SlaveWarmupManager warmupManager;

    public ReadWriteAutoConfiguration(ReadWriteProperties properties,
                                       ObjectProvider<MeterRegistry> meterRegistryProvider, Environment environment) {
        this.properties = properties;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.environment = environment;
        this.circuitBreaker = new SlaveCircuitBreaker(properties);
        this.warmupManager = new SlaveWarmupManager();

        if (properties.getGroups().isEmpty()) {
            log.warn("[RW-Config] spring.datasource.rw.enabled=true but no groups are configured. "
                    + "Read-write routing is NOT active; the service keeps using its single spring.datasource datasource. "
                    + "Configure spring.datasource.rw.groups.<name>.master to enable it.");
            return;
        }

        log.info("[RW-Config] Initializing read-write separation with {} group(s), default target: {}",
                properties.getGroups().size(), properties.getDefaultTarget());

        initializeDataSources();
    }

    private void initializeDataSources() {
        int initialSize = allDataSources.size();
        try {
            for (Map.Entry<String, ReadWriteProperties.DataSourceGroup> entry :
                    properties.getGroups().entrySet()) {

                String groupName = entry.getKey();
                ReadWriteProperties.DataSourceGroup group = entry.getValue();

                if (group.getMaster() == null) {
                    throw new IllegalStateException("[RW-Config] Group [" + groupName
                            + "] has no master datasource. Configure spring.datasource.rw.groups."
                            + groupName + ".master");
                }

                Map<String, DataSource> slaveDataSources = new HashMap<>();
                List<SlaveLoadBalancer.SlaveInfo> slaveInfos = new ArrayList<>();

                if (group.isSlavesEnabled()) {
                    for (ReadWriteProperties.SlaveDataSourceConfig slaveConfig : group.getSlaves()) {
                        String slaveName = slaveConfig.getName();
                        HikariDataSource slaveDataSource = createDataSource(
                                groupName + "-" + slaveName, slaveConfig, groupName);
                        allDataSources.add(slaveDataSource);
                        // Count live connections for LEAST_CONNECTIONS; the wrapper
                        // decrements on close and on failed acquisition.
                        slaveDataSources.put(slaveName, new CountingDataSource(slaveDataSource));

                        slaveInfos.add(new SlaveLoadBalancer.SlaveInfo(
                                slaveName,
                                slaveConfig.getWeight(),
                                0,
                                slaveConfig.isAvailable()
                        ));
                    }
                } else {
                    log.info("[RW-Config] Group [{}] has slaves disabled; all reads route to master", groupName);
                }

                HikariDataSource masterDataSource = createDataSource(
                        groupName + "-master", group.getMaster(), groupName);
                allDataSources.add(masterDataSource);

                slaveDataSourcesMap.put(groupName, slaveDataSources);

                SlaveLoadBalancer loadBalancer = createLoadBalancer(
                        group.getLoadBalance() != null ? group.getLoadBalance() : properties.getLoadBalance());
                if (loadBalancer instanceof AbstractLoadBalancer abstractLoadBalancer) {
                    abstractLoadBalancer.setWarmupManager(warmupManager);
                }

                ReadWriteRoutingDataSource routingDataSource = new ReadWriteRoutingDataSource(
                        groupName,
                        masterDataSource,
                        slaveDataSources,
                        properties,
                        loadBalancer,
                        meterRegistry,
                        circuitBreaker
                );
                routingDataSource.setSlaveInfos(slaveInfos);
                routingDataSource.afterPropertiesSet();

                routingDataSources.put(groupName, routingDataSource);

                log.info("[RW-Config] Group [{}] configured. Master: {}, slaves: {}, loadBalance: {}",
                        groupName,
                        maskUrl(group.getMaster().getUrl()),
                        slaveDataSources.keySet(),
                        loadBalancer.getClass().getSimpleName());
            }
            registerCircuitBreakerRules();
        } catch (Exception e) {
            cleanupDataSources(initialSize);
            throw e;
        }
    }

    private void registerCircuitBreakerRules() {
        if (routingDataSources.isEmpty()) {
            return;
        }
        Map<String, java.util.Collection<String>> groupSlaves = new java.util.LinkedHashMap<>();
        slaveDataSourcesMap.forEach((group, slaves) -> groupSlaves.put(group, slaves.keySet()));
        circuitBreaker.registerGroups(groupSlaves);
    }

    private void cleanupDataSources(int fromIndex) {
        for (int i = allDataSources.size() - 1; i >= fromIndex; i--) {
            try {
                allDataSources.get(i).close();
            } catch (Exception ignored) {
            }
        }
        allDataSources.subList(fromIndex, allDataSources.size()).clear();
    }

    private HikariDataSource createDataSource(String poolName,
                                               ReadWriteProperties.DataSourceConfig config,
                                               String groupName) {
        if (config.getUrl() == null || config.getUrl().isBlank()) {
            throw new IllegalStateException("[RW-Config] Datasource [" + poolName + "] of group ["
                    + groupName + "] has a blank url. Check spring.datasource.rw.groups." + groupName);
        }
        if (config.getUsername() == null || config.getUsername().isBlank()) {
            throw new IllegalStateException("[RW-Config] Datasource [" + poolName + "] of group ["
                    + groupName + "] has a blank username. Check spring.datasource.rw.groups." + groupName);
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName(poolName);
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(config.getDriverClassName());

        hikariConfig.setMinimumIdle(config.getMinimumIdle());
        hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout().toMillis());
        hikariConfig.setIdleTimeout(config.getIdleTimeout().toMillis());
        hikariConfig.setMaxLifetime(config.getMaxLifetime().toMillis());

        // Negative => the pool starts in the background. An unreachable slave at
        // startup must never fail the application context.
        hikariConfig.setInitializationFailTimeout(config.getInitializationFailTimeout());

        if (config.getConnectionTestQuery() != null) {
            hikariConfig.setConnectionTestQuery(config.getConnectionTestQuery());
        }

        return new HikariDataSource(hikariConfig);
    }

    private SlaveLoadBalancer createLoadBalancer(ReadWriteProperties.LoadBalanceType type) {
        return SlaveLoadBalancer.create(type);
    }

    private String maskUrl(String url) {
        if (url == null) {
            return "null";
        }
        return url.replaceAll("password=[^&]*", "password=***");
    }

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    @ConditionalOnProperty(
            prefix = "spring.datasource.dynamic",
            name = "enabled",
            havingValue = "false"
    )
    @Conditional(GroupsConfiguredCondition.class)
    public DataSource dataSource() {
        String defaultGroup = routingDataSources.keySet().iterator().next();
        log.info("[RW-Config] Using group [{}] as the primary DataSource", defaultGroup);
        return routingDataSources.get(defaultGroup);
    }

    @Bean
    @ConditionalOnMissingBean(DynamicDataSourceProvider.class)
    @ConditionalOnClass(name = "com.baomidou.dynamic.datasource.DynamicRoutingDataSource")
    @ConditionalOnProperty(
            prefix = "spring.datasource.dynamic",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    @Conditional(GroupsConfiguredCondition.class)
    public DynamicDataSourceProvider readWriteDynamicDataSourceProvider() {
        return () -> {
            Map<String, DataSource> dataSources = new HashMap<>(routingDataSources);
            log.info("[RW-Config] Exposed {} rw datasource group(s) to DynamicDatasource: {}. "
                    + "spring.datasource.dynamic.primary must point at one of them.",
                    dataSources.size(), dataSources.keySet());
            return dataSources;
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @Conditional(GroupsConfiguredCondition.class)
    public ReadWriteDataSourceProvider readWriteDataSourceProvider() {
        return new ReadWriteDataSourceProvider(routingDataSources);
    }

    @Bean
    @ConditionalOnMissingBean
    @Conditional(GroupsConfiguredCondition.class)
    public ReadWriteRoutingAspect readWriteRoutingAspect() {
        log.debug("[RW-Config] Registering ReadWriteRoutingAspect");
        return new ReadWriteRoutingAspect();
    }

    @Bean
    @Conditional(GroupsConfiguredCondition.class)
    public SlaveCircuitBreaker slaveCircuitBreaker() {
        return circuitBreaker;
    }

    @Bean
    @Conditional(GroupsConfiguredCondition.class)
    public SlaveWarmupManager slaveWarmupManager() {
        return warmupManager;
    }

    /**
     * Tracing advisor must run inside {@link ReadWriteRoutingAspect} so it sees the
     * pushed scope, and outside the transaction interceptor so it covers the whole
     * service call.
     */
    @Bean
    @Conditional(GroupsConfiguredCondition.class)
    public Advisor readWriteTracingAdvisor() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(
                "@annotation(com.cognora.platform.common.data.rw.annotation.Master)"
                        + " || @annotation(com.cognora.platform.common.data.rw.annotation.Slave)"
                        + " || @within(com.cognora.platform.common.data.rw.annotation.Master)"
                        + " || @within(com.cognora.platform.common.data.rw.annotation.Slave)"
                        + " || @annotation(org.springframework.transaction.annotation.Transactional)"
                        + " || @within(org.springframework.transaction.annotation.Transactional)");
        DefaultPointcutAdvisor advisor =
                new DefaultPointcutAdvisor(pointcut, new ReadWriteTracingInterceptor());
        advisor.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        log.debug("[RW-Config] Registering ReadWriteTracingAdvisor");
        return advisor;
    }

    @Bean
    @Conditional(GroupsConfiguredCondition.class)
    public SmartInitializingSingleton readWriteDatasourceConflictChecker(ConfigurableApplicationContext context) {
        var groups = java.util.Set.copyOf(routingDataSources.keySet());
        return () -> new ReadWriteDatasourceConflictChecker(context, groups).check();
    }

    /**
     * Pre-warm slave connections after the context is ready. The work is submitted to
     * the warmup manager's daemon executor, so a slow or dead replica never blocks
     * application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (slaveDataSourcesMap.isEmpty()) {
            return;
        }
        int count = slaveDataSourcesMap.values().stream().mapToInt(Map::size).sum();
        if (count == 0) {
            return;
        }
        log.info("[RW-Config] Application ready, prewarming {} slave datasource(s) asynchronously", count);
        warmupManager.warmupAll(slaveDataSourcesMap);
    }

    @Bean
    @ConditionalOnMissingBean(TaskDecorator.class)
    @ConditionalOnClass(name = "org.springframework.core.task.TaskDecorator")
    @Conditional(GroupsConfiguredCondition.class)
    public TaskDecorator readWriteTaskDecorator() {
        log.info("[RW-Config] Registering ReadWriteTaskDecorator for async routing-context propagation");
        return new ReadWriteTaskDecorator();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "spring.datasource.rw",
            name = "health-check-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public SlaveHealthChecker slaveHealthChecker() {
        log.debug("[RW-Config] Registering SlaveHealthChecker with interval: {}",
                properties.getHealthCheckInterval());

        return new SlaveHealthChecker(
                routingDataSources,
                slaveDataSourcesMap,
                properties,
                meterRegistry
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.apache.ibatis.plugin.Interceptor")
    public SqlRoutingInterceptor sqlRoutingInterceptor() {
        log.debug("[RW-Config] Registering SqlRoutingInterceptor for SQL-based routing");
        return new SqlRoutingInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicDataSourceRefresher dynamicDataSourceRefresher() {
        log.debug("[RW-Config] Registering DynamicDataSourceRefresher for config refresh");
        return new DynamicDataSourceRefresher(properties, routingDataSources, environment);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConnectionPoolMetrics connectionPoolMetrics() {
        ConnectionPoolMetrics metrics = new ConnectionPoolMetrics(meterRegistry);

        for (HikariDataSource ds : allDataSources) {
            String poolName = ds.getPoolName();
            String[] parts = poolName.split("-", 2);
            if (parts.length == 2) {
                metrics.registerDataSource(parts[0], parts[1], ds);
            }
        }

        log.debug("[RW-Config] Registered ConnectionPoolMetrics for {} datasources",
                allDataSources.size());
        return metrics;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.boot.health.contributor.HealthIndicator")
    @Conditional(GroupsConfiguredCondition.class)
    @ConditionalOnProperty(
            prefix = "spring.datasource.rw",
            name = "health-check-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public ReadWriteHealthIndicator readWriteHealthIndicator(
            SlaveHealthChecker healthChecker, ReadWriteDataSourceProvider dataSourceProvider) {
        log.debug("[RW-Config] Registering ReadWriteHealthIndicator for Actuator");
        return new ReadWriteHealthIndicator(healthChecker, dataSourceProvider, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    @Conditional(GroupsConfiguredCondition.class)
    @ConditionalOnProperty(
            prefix = "spring.datasource.rw",
            name = "health-check-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public ReadWriteEndpoint readWriteEndpoint(
            ReadWriteDataSourceProvider dataSourceProvider, SlaveHealthChecker healthChecker) {
        log.debug("[RW-Config] Registering ReadWriteEndpoint for Actuator");
        return new ReadWriteEndpoint(dataSourceProvider, healthChecker, properties,
                circuitBreaker, warmupManager);
    }

    @PreDestroy
    public void destroy() {
        log.debug("[RW-Config] Shutting down all datasources...");
        warmupManager.shutdown();
        for (HikariDataSource dataSource : allDataSources) {
            try {
                if (!dataSource.isClosed()) {
                    dataSource.close();
                    log.debug("[RW-Config] Closed datasource: {}", dataSource.getPoolName());
                }
            } catch (Exception e) {
                log.warn("[RW-Config] Error closing datasource {}: {}",
                        dataSource.getPoolName(), e.getMessage());
            }
        }
    }

    /**
     * Only publish the RW datasource beans when at least one group is configured.
     * Otherwise the service keeps its regular single datasource and startup is not
     * broken by a half-filled configuration.
     */
    static class GroupsConfiguredCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ReadWriteProperties bound = Binder.get(context.getEnvironment())
                    .bind("spring.datasource.rw", ReadWriteProperties.class)
                    .orElse(null);
            return bound != null && bound.getGroups() != null && !bound.getGroups().isEmpty();
        }
    }

    public record ReadWriteDataSourceProvider(
            Map<String, ReadWriteRoutingDataSource> routingDataSources
    ) {
        public ReadWriteRoutingDataSource getDataSource(String groupName) {
            ReadWriteRoutingDataSource ds = routingDataSources.get(groupName);
            if (ds == null) {
                throw new IllegalArgumentException(
                        "No datasource group found: " + groupName +
                                ". Available groups: " + routingDataSources.keySet());
            }
            return ds;
        }

        public java.util.Set<String> getGroupNames() {
            return routingDataSources.keySet();
        }
    }
}
