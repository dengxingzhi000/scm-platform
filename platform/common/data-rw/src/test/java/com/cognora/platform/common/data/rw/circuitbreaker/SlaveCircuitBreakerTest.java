package com.cognora.platform.common.data.rw.circuitbreaker;

import com.cognora.platform.common.data.rw.config.ReadWriteProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("从库熔断器测试")
class SlaveCircuitBreakerTest {

    private final ReadWriteProperties properties = new ReadWriteProperties();
    private SlaveCircuitBreaker breaker;
    private String group;

    @BeforeEach
    void setUp() {
        ReadWriteProperties.CircuitBreakerProperties cb = properties.getCircuitBreaker();
        cb.setMinRequestAmount(1);
        cb.setStatIntervalMs(1000);
        cb.setTimeWindowSeconds(1);
        cb.setErrorRatio(0.5);
        // keep the slow-request rule out of the way for this test
        cb.setSlowRtMs(60_000);
        cb.setSlowRatioThreshold(1.0);

        breaker = new SlaveCircuitBreaker(properties);
        group = "g" + System.nanoTime();
        breaker.registerGroups(Map.of(group, List.of("slave1")));
    }

    @Test
    @DisplayName("连续失败后熔断打开，期间直接返回 null 让调用方回退主库")
    void shouldOpenAfterFailuresAndFallback() throws Exception {
        assertThrows(SQLException.class,
                () -> breaker.connect(group, "slave1", () -> {
                    throw new SQLException("slave down");
                }));

        Connection connection = breaker.connect(group, "slave1", () -> mock(Connection.class));

        assertNull(connection, "熔断打开时必须返回 null，由数据源回退主库");
        assertTrue(breaker.isOpen(group, "slave1"));
        assertEquals(SlaveCircuitBreaker.CircuitBreakerState.OPEN, breaker.getState(group, "slave1"));
    }

    @Test
    @DisplayName("时间窗口结束后允许试探，成功则恢复 CLOSED")
    void shouldRecoverAfterTimeWindow() throws Exception {
        assertThrows(SQLException.class,
                () -> breaker.connect(group, "slave1", () -> {
                    throw new SQLException("slave down");
                }));
        assertNull(breaker.connect(group, "slave1", () -> mock(Connection.class)));
        assertTrue(breaker.isOpen(group, "slave1"));

        Thread.sleep(1100);
        assertFalse(breaker.isOpen(group, "slave1"), "窗口结束后应进入 HALF_OPEN 允许试探");

        Connection connection = breaker.connect(group, "slave1", () -> mock(Connection.class));
        assertNotNull(connection);
        connection.close();

        assertEquals(SlaveCircuitBreaker.CircuitBreakerState.CLOSED, breaker.getState(group, "slave1"));
    }

    @Test
    @DisplayName("关闭熔断配置时不走 Sentinel，直接返回连接")
    void shouldBeInertWhenDisabled() throws Exception {
        properties.getCircuitBreaker().setEnabled(false);
        SlaveCircuitBreaker disabled = new SlaveCircuitBreaker(properties);
        disabled.registerGroups(Map.of(group, List.of("slave1")));

        Connection connection = disabled.connect(group, "slave1", () -> mock(Connection.class));

        assertNotNull(connection);
        assertFalse(disabled.isOpen(group, "slave1"));
    }
}
