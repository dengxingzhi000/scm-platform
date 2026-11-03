package com.cognora.platform.common.data.rw.loadbalance;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("从库预热管理器测试")
class SlaveWarmupManagerTest {

    private final SlaveWarmupManager warmupManager = new SlaveWarmupManager(60);

    @AfterEach
    void tearDown() {
        warmupManager.shutdown();
    }

    @Test
    @DisplayName("刚上线的从库应该有较低的权重")
    void newlyOnlineSlaveShouldHaveLowWeight() {
        
        warmupManager.markOnline("slave1");

        int effectiveWeight = warmupManager.getEffectiveWeight("slave1", 100);
        assertTrue(effectiveWeight < 100);
        assertTrue(effectiveWeight >= 1);
    }

    @Test
    @DisplayName("未记录的从库应该返回原始权重")
    void unknownSlaveShouldReturnOriginalWeight() {
        

        int effectiveWeight = warmupManager.getEffectiveWeight("slave1", 100);
        assertEquals(100, effectiveWeight);
    }

    @Test
    @DisplayName("正在预热的从库应该返回 true")
    void warmingUpSlaveShouldReturnTrue() {
        
        warmupManager.markOnline("slave1");

        assertTrue(warmupManager.isWarmingUp("slave1"));
    }

    @Test
    @DisplayName("未记录的从库应该返回 false")
    void unknownSlaveShouldReturnFalse() {
        

        assertFalse(warmupManager.isWarmingUp("slave1"));
    }

    @Test
    @DisplayName("预热进度应该在 0-100 之间")
    void warmupProgressShouldBeBetween0And100() {
        
        warmupManager.markOnline("slave1");

        int progress = warmupManager.getWarmupProgress("slave1");
        assertTrue(progress >= 0 && progress <= 100);
    }

    @Test
    @DisplayName("未记录的从库预热进度应该是 100")
    void unknownSlaveWarmupProgressShouldBe100() {
        

        assertEquals(100, warmupManager.getWarmupProgress("slave1"));
    }

    @Test
    @DisplayName("下线后应该清除预热状态")
    void offlineShouldClearWarmupState() {
        
        warmupManager.markOnline("slave1");
        warmupManager.markOffline("slave1");

        assertFalse(warmupManager.isWarmingUp("slave1"));
        assertEquals(100, warmupManager.getWarmupProgress("slave1"));
    }

    @Test
    @DisplayName("权重至少应该为 1")
    void weightShouldBeAtLeast1() {
        
        warmupManager.markOnline("slave1");

        int effectiveWeight = warmupManager.getEffectiveWeight("slave1", 100);
        assertTrue(effectiveWeight >= 1);
    }

    @Test
    @DisplayName("异步预热成功应记录耗时，且不阻塞调用线程")
    void warmupShouldRecordElapsedTime() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(3)).thenReturn(true);

        SlaveWarmupManager.WarmupResult result =
                warmupManager.warmup("product", "slave1", dataSource).get(5, TimeUnit.SECONDS);

        assertTrue(result.success());
        assertTrue(result.elapsedMs() >= 0);
        assertTrue(warmupManager.getWarmupResults().containsKey("product.slave1"));
        assertTrue(warmupManager.isWarmingUp("slave1"), "预热成功后应开始权重爬升");
    }

    @Test
    @DisplayName("预热失败应记录失败原因并清除预热状态")
    void warmupFailureShouldRecordError() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("slave down"));

        SlaveWarmupManager.WarmupResult result =
                warmupManager.warmup("product", "slave1", dataSource).get(5, TimeUnit.SECONDS);

        assertFalse(result.success());
        assertEquals("slave down", result.error());
        assertFalse(warmupManager.isWarmingUp("slave1"));
    }

    @Test
    @DisplayName("warmupAll 应为每个从库各提交一次预热")
    void warmupAllShouldCoverEverySlave() throws Exception {
        DataSource slave1 = mock(DataSource.class);
        DataSource slave2 = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(connection.isValid(3)).thenReturn(true);
        when(slave1.getConnection()).thenReturn(connection);
        when(slave2.getConnection()).thenThrow(new SQLException("down"));

        warmupManager.warmupAll(Map.of("product", Map.of("slave1", slave1, "slave2", slave2)));

        long deadline = System.currentTimeMillis() + 5000;
        while (warmupManager.getWarmupResults().size() < 2 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }

        assertEquals(2, warmupManager.getWarmupResults().size());
        assertTrue(warmupManager.getWarmupResults().get("product.slave1").success());
        assertFalse(warmupManager.getWarmupResults().get("product.slave2").success());
    }
}
