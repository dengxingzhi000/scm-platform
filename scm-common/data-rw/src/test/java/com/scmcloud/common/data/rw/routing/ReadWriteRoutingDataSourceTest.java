package com.scmcloud.common.data.rw.routing;

import com.scmcloud.common.data.rw.config.ReadWriteProperties;
import com.scmcloud.common.data.rw.loadbalance.SlaveLoadBalancer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
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
        dataSource.afterPropertiesSet();
        return dataSource;
    }

    @Test
    @DisplayName("未配置从库时读操作应落在主库")
    void shouldRouteToMasterWhenNoSlaveConfigured() {
        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of());
        dataSource.setSlaveInfos(List.of());

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);

        assertEquals("master", dataSource.determineCurrentLookupKey());
    }

    @Test
    @DisplayName("未配置从库且处于 AUTO 时应落在主库")
    void shouldRouteAutoToMasterWhenNoSlaveConfigured() {
        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of());
        dataSource.setSlaveInfos(List.of());

        assertEquals("master", dataSource.determineCurrentLookupKey());
    }

    @Test
    @DisplayName("从库可用时应路由到从库")
    void shouldRouteToAvailableSlave() throws SQLException {
        DataSource slave = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(slave.getConnection()).thenReturn(connection);
        when(connection.isValid(3)).thenReturn(true);

        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of("slave1", slave));
        dataSource.setSlaveInfos(List.of(new SlaveLoadBalancer.SlaveInfo("slave1", 1, 0, true)));

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);

        assertEquals("slave1", dataSource.determineCurrentLookupKey());
    }

    @Test
    @DisplayName("从库连接不可用时应回退主库")
    void shouldFallbackToMasterWhenSlaveUnreachable() throws SQLException {
        DataSource slave = mock(DataSource.class);
        when(slave.getConnection()).thenThrow(new SQLException("slave down"));

        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of("slave1", slave));
        dataSource.setSlaveInfos(List.of(new SlaveLoadBalancer.SlaveInfo("slave1", 1, 0, true)));

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);

        assertEquals("master", dataSource.determineCurrentLookupKey());
    }

    @Test
    @DisplayName("写后读窗口内应强制主库")
    void shouldRouteToMasterWithinReadAfterWriteWindow() throws SQLException {
        DataSource slave = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(slave.getConnection()).thenReturn(connection);
        when(connection.isValid(3)).thenReturn(true);

        ReadWriteRoutingDataSource dataSource = newDataSource("order", Map.of("slave1", slave));
        dataSource.setSlaveInfos(List.of(new SlaveLoadBalancer.SlaveInfo("slave1", 1, 0, true)));

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);
        assertEquals("slave1", dataSource.determineCurrentLookupKey());

        ReadWriteRoutingContext.markWrite();
        assertEquals("master", dataSource.determineCurrentLookupKey());
    }
}
