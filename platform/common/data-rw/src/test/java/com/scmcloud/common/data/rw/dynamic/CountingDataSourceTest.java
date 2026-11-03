package com.scmcloud.common.data.rw.dynamic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("从库连接计数数据源测试")
class CountingDataSourceTest {

    @Test
    @DisplayName("获取连接后计数 +1，关闭后 -1")
    void shouldCountHeldConnection() throws Exception {
        DataSource delegate = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(delegate.getConnection()).thenReturn(connection);

        CountingDataSource counting = new CountingDataSource(delegate);
        assertEquals(0, counting.getActiveConnections());

        Connection wrapper = counting.getConnection();
        assertEquals(1, counting.getActiveConnections());

        wrapper.close();
        assertEquals(0, counting.getActiveConnections());
        verify(connection).close();
    }

    @Test
    @DisplayName("重复 close 只能归还一次")
    void doubleCloseShouldDecrementOnce() throws Exception {
        DataSource delegate = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(delegate.getConnection()).thenReturn(connection);

        CountingDataSource counting = new CountingDataSource(delegate);
        Connection wrapper = counting.getConnection();
        wrapper.close();
        wrapper.close();

        assertEquals(0, counting.getActiveConnections());
    }

    @Test
    @DisplayName("获取连接失败时必须回滚计数")
    void failedAcquisitionShouldNotLeakCount() throws Exception {
        DataSource delegate = mock(DataSource.class);
        when(delegate.getConnection()).thenThrow(new SQLException("down"));

        CountingDataSource counting = new CountingDataSource(delegate);

        try {
            counting.getConnection();
        } catch (SQLException expected) {
            // ignore
        }

        assertEquals(0, counting.getActiveConnections(), "异常路径不能把活跃计数留在 1");
    }

    @Test
    @DisplayName("带用户名密码的获取路径也要计数")
    void shouldCountConnectionWithCredentials() throws Exception {
        DataSource delegate = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(delegate.getConnection("u", "p")).thenReturn(connection);

        CountingDataSource counting = new CountingDataSource(delegate);
        Connection wrapper = counting.getConnection("u", "p");
        assertEquals(1, counting.getActiveConnections());
        wrapper.close();
        assertEquals(0, counting.getActiveConnections());
    }
}
