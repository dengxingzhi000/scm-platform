package com.cognora.platform.common.data.rw.routing;

import com.cognora.platform.common.data.rw.circuitbreaker.SlaveCircuitBreaker;
import com.cognora.platform.common.data.rw.config.ReadWriteProperties;
import com.cognora.platform.common.data.rw.dynamic.CountingDataSource;
import com.cognora.platform.common.data.rw.loadbalance.SlaveLoadBalancer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("读写路由数据源测试")
class ReadWriteRoutingDataSourceTest {

    private final ReadWriteProperties properties = new ReadWriteProperties();

    @AfterEach
    void tearDown() {
        ReadWriteRoutingContext.clear();
    }

    private ReadWriteRoutingDataSource newDataSource(String group, Map<String, DataSource> slaves) {
        DataSource master = mock(DataSource.class);
        ReadWriteRoutingDataSource dataSource = new ReadWriteRoutingDataSource(
                group,
                master,
                slaves,
                properties,
                SlaveLoadBalancer.create(ReadWriteProperties.LoadBalanceType.ROUND_ROBIN),
                null);
        dataSource.setSlaveInfos(slaves.keySet().stream()
                .map(name -> new SlaveLoadBalancer.SlaveInfo(name, 1, 0, true))
                .toList());
        dataSource.afterPropertiesSet();
        return dataSource;
    }

    private DataSource healthySlave() throws SQLException {
        DataSource slave = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(slave.getConnection()).thenReturn(connection);
        when(connection.isValid(properties.getValidationTimeoutSeconds())).thenReturn(true);
        return slave;
    }

    @Test
    @DisplayName("未配置从库时读操作应落在主库")
    void shouldRouteToMasterWhenNoSlaveConfigured() {
        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of());

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);

        assertEquals("master", dataSource.determineCurrentLookupKey());
        assertEquals(0L, dataSource.getRouteCounts().get("slave"));
    }

    @Test
    @DisplayName("未配置从库且处于 AUTO 时应落在主库")
    void shouldRouteAutoToMasterWhenNoSlaveConfigured() {
        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of());

        assertEquals("master", dataSource.determineCurrentLookupKey());
    }

    @Test
    @DisplayName("AUTO 默认走主库，即使从库可用（避免丢失上下文的异步线程读到从库）")
    void autoShouldRouteToMasterByDefault() throws SQLException {
        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of("slave1", healthySlave()));

        assertEquals("master", dataSource.determineCurrentLookupKey());
        assertEquals(1L, dataSource.getRouteCounts().get("master"));
        assertEquals(0L, dataSource.getRouteCounts().get("slave"));
    }

    @Test
    @DisplayName("default-target=SLAVE 时 AUTO 才走从库")
    void autoShouldRouteToSlaveWhenConfigured() throws SQLException {
        properties.setDefaultTarget(ReadWriteProperties.RoutingTarget.SLAVE);
        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of("slave1", healthySlave()));

        assertEquals("slave1", dataSource.determineCurrentLookupKey());
        assertEquals(1L, dataSource.getRouteCounts().get("slave"));
    }

    @Test
    @DisplayName("从库可用时应路由到从库")
    void shouldRouteToAvailableSlave() throws SQLException {
        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of("slave1", healthySlave()));

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);

        assertEquals("slave1", dataSource.determineCurrentLookupKey());
    }

    @Test
    @DisplayName("从库连接不可用时应回退主库")
    void shouldFallbackToMasterWhenSlaveUnreachable() throws SQLException {
        DataSource slave = mock(DataSource.class);
        when(slave.getConnection()).thenThrow(new SQLException("slave down"));

        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of("slave1", slave));

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);

        assertEquals("master", dataSource.determineCurrentLookupKey());
        assertEquals(1L, dataSource.getRouteCounts().get("fallback"));
    }

    @Test
    @DisplayName("首次连接失败应立即熔断该从库，后续请求不再反复探测")
    void failedProbeShouldCircuitBreakSlave() throws SQLException {
        DataSource slave = mock(DataSource.class);
        when(slave.getConnection()).thenThrow(new SQLException("slave down"));

        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of("slave1", slave));
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);

        assertEquals("master", dataSource.determineCurrentLookupKey());
        assertFalse(dataSource.getSlaveAvailability().get("slave1"),
                "首次探测失败后必须把从库标记为不可用");

        assertEquals("master", dataSource.determineCurrentLookupKey());
        verify(slave, times(1)).getConnection();
    }

    @Test
    @DisplayName("fallbackToMaster=false 且无从库时必须失败，而不是静默走主库")
    void shouldThrowWhenFallbackDisabledAndNoSlave() {
        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of());
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, null, false);

        ReadWriteRoutingException exception = assertThrows(ReadWriteRoutingException.class,
                dataSource::determineCurrentLookupKey);

        assertTrue(exception.getMessage().contains("fallbackToMaster=false"));
    }

    @Test
    @DisplayName("指定的从库不可用且禁用回退时应失败")
    void shouldThrowWhenSpecifiedSlaveUnavailableAndFallbackDisabled() throws SQLException {
        DataSource slave = mock(DataSource.class);
        when(slave.getConnection()).thenThrow(new SQLException("slave down"));

        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of("slave1", slave));
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, "slave1", false);

        assertThrows(ReadWriteRoutingException.class, dataSource::determineCurrentLookupKey);
    }

    @Test
    @DisplayName("恢复可用的从库应清掉探测缓存")
    void markSlaveAvailableShouldResetProbeCache() throws SQLException {
        DataSource slave = mock(DataSource.class);
        when(slave.getConnection()).thenThrow(new SQLException("slave down"));

        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of("slave1", slave));
        dataSource.markSlaveUnavailable("slave1");
        assertFalse(dataSource.getSlaveAvailability().get("slave1"));

        dataSource.markSlaveAvailable("slave1");
        assertTrue(dataSource.getSlaveAvailability().get("slave1"));
    }

    @Test
    @DisplayName("写后读窗口内应强制主库")
    void shouldRouteToMasterWithinReadAfterWriteWindow() throws SQLException {
        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of("slave1", healthySlave()));

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);
        assertEquals("slave1", dataSource.determineCurrentLookupKey());

        ReadWriteRoutingContext.markWrite();
        assertEquals("master", dataSource.determineCurrentLookupKey());
    }

    @Test
    @DisplayName("路由计数应区分主库/从库/回退")
    void shouldCountRoutes() throws SQLException {
        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of("slave1", healthySlave()));

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);
        dataSource.determineCurrentLookupKey();
        dataSource.determineCurrentLookupKey();
        ReadWriteRoutingContext.pop();

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
        dataSource.determineCurrentLookupKey();
        ReadWriteRoutingContext.pop();

        assertEquals(1L, dataSource.getRouteCounts().get("master"));
        assertEquals(2L, dataSource.getRouteCounts().get("slave"));
        assertEquals(0L, dataSource.getRouteCounts().get("fallback"));
    }

    @Test
    @DisplayName("未启用 fail-fast 时探测失败不应标记从库不可用")
    void probeFailureShouldKeepSlaveEligibleWhenFailFastDisabled() throws SQLException {
        properties.setFailFastOnSlaveConnectionFailure(false);

        DataSource slave = mock(DataSource.class);
        when(slave.getConnection()).thenThrow(new SQLException("slave down"));

        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of("slave1", slave));
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);

        assertEquals("master", dataSource.determineCurrentLookupKey());
        assertTrue(dataSource.getSlaveAvailability().get("slave1"),
                "关闭 fail-fast 后应由健康检查决定从库可用性");
    }

    @Test
    @DisplayName("LEAST_CONNECTIONS 在多从库时应选择活跃连接数更少的那个")
    void leastConnectionsShouldPickIdleSlave() throws Exception {
        DataSource master = mock(DataSource.class);
        DataSource slave1Delegate = mock(DataSource.class);
        DataSource slave2Delegate = mock(DataSource.class);
        Connection connection1 = mock(Connection.class);
        Connection connection2 = mock(Connection.class);
        when(slave1Delegate.getConnection()).thenReturn(connection1);
        when(slave2Delegate.getConnection()).thenReturn(connection2);
        when(connection1.isValid(properties.getValidationTimeoutSeconds())).thenReturn(true);
        when(connection2.isValid(properties.getValidationTimeoutSeconds())).thenReturn(true);

        CountingDataSource slave1 = new CountingDataSource(slave1Delegate);
        CountingDataSource slave2 = new CountingDataSource(slave2Delegate);

        // slave1 holds one connection open -> it must not be selected
        Connection held = slave1.getConnection();

        ReadWriteRoutingDataSource dataSource = new ReadWriteRoutingDataSource(
                "order", master, Map.of("slave1", slave1, "slave2", slave2), properties,
                SlaveLoadBalancer.create(ReadWriteProperties.LoadBalanceType.LEAST_CONNECTIONS),
                null);
        dataSource.setSlaveInfos(List.of(
                new SlaveLoadBalancer.SlaveInfo("slave1", 1, 0, true),
                new SlaveLoadBalancer.SlaveInfo("slave2", 1, 0, true)));
        dataSource.afterPropertiesSet();

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);
        assertEquals("slave2", dataSource.determineCurrentLookupKey());

        held.close();
        assertEquals(0, slave1.getActiveConnections());
    }

    @Test
    @DisplayName("从库查询失败触发熔断，后续请求回退主库并计 fallback")
    void circuitOpenShouldFallbackToMaster() throws Exception {
        ReadWriteProperties circuitProperties = new ReadWriteProperties();
        circuitProperties.getCircuitBreaker().setMinRequestAmount(1);
        circuitProperties.getCircuitBreaker().setStatIntervalMs(1000);
        circuitProperties.getCircuitBreaker().setTimeWindowSeconds(1);
        circuitProperties.getCircuitBreaker().setSlowRtMs(60_000);
        circuitProperties.getCircuitBreaker().setSlowRatioThreshold(1.0);

        SlaveCircuitBreaker breaker = new SlaveCircuitBreaker(circuitProperties);
        breaker.registerGroups(Map.of("order", List.of("slave1")));

        DataSource master = mock(DataSource.class);
        Connection masterConnection = mock(Connection.class);
        when(master.getConnection()).thenReturn(masterConnection);

        DataSource slave = mock(DataSource.class);
        Connection slaveConnection = mock(Connection.class);
        when(slave.getConnection()).thenReturn(slaveConnection);
        when(slaveConnection.isValid(circuitProperties.getValidationTimeoutSeconds())).thenReturn(true);
        when(slaveConnection.createStatement()).thenThrow(new SQLException("query failed"));

        ReadWriteRoutingDataSource dataSource = new ReadWriteRoutingDataSource(
                "order", master, Map.of("slave1", slave), circuitProperties,
                SlaveLoadBalancer.create(ReadWriteProperties.LoadBalanceType.ROUND_ROBIN),
                null, breaker);
        dataSource.setSlaveInfos(List.of(new SlaveLoadBalancer.SlaveInfo("slave1", 1, 0, true)));
        dataSource.afterPropertiesSet();

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);

        Connection first = dataSource.getConnection();
        assertNotSame(masterConnection, first, "第一次应拿到从库连接");
        try {
            first.createStatement();
            fail("expected SQLException from the slave");
        } catch (SQLException expected) {
            // traced by the circuit breaker
        } finally {
            first.close();
        }

        Connection second = dataSource.getConnection();
        assertSame(masterConnection, second, "熔断后必须回退主库");
        assertTrue(dataSource.getRouteCounts().get("fallback") >= 1);
        assertEquals("master", ReadWriteRoutingContext.currentRoute().target());
        assertTrue(ReadWriteRoutingContext.currentRoute().fallback());
    }
}