package com.scmcloud.common.data.rw.loadbalance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("从库预热管理器测试")
class SlaveWarmupManagerTest {

    @Test
    @DisplayName("刚上线的从库应该有较低的权重")
    void newlyOnlineSlaveShouldHaveLowWeight() {
        SlaveWarmupManager warmupManager = new SlaveWarmupManager(60);
        warmupManager.markOnline("slave1");

        int effectiveWeight = warmupManager.getEffectiveWeight("slave1", 100);
        assertTrue(effectiveWeight < 100);
        assertTrue(effectiveWeight >= 1);
    }

    @Test
    @DisplayName("未记录的从库应该返回原始权重")
    void unknownSlaveShouldReturnOriginalWeight() {
        SlaveWarmupManager warmupManager = new SlaveWarmupManager(60);

        int effectiveWeight = warmupManager.getEffectiveWeight("slave1", 100);
        assertEquals(100, effectiveWeight);
    }

    @Test
    @DisplayName("正在预热的从库应该返回 true")
    void warmingUpSlaveShouldReturnTrue() {
        SlaveWarmupManager warmupManager = new SlaveWarmupManager(60);
        warmupManager.markOnline("slave1");

        assertTrue(warmupManager.isWarmingUp("slave1"));
    }

    @Test
    @DisplayName("未记录的从库应该返回 false")
    void unknownSlaveShouldReturnFalse() {
        SlaveWarmupManager warmupManager = new SlaveWarmupManager(60);

        assertFalse(warmupManager.isWarmingUp("slave1"));
    }

    @Test
    @DisplayName("预热进度应该在 0-100 之间")
    void warmupProgressShouldBeBetween0And100() {
        SlaveWarmupManager warmupManager = new SlaveWarmupManager(60);
        warmupManager.markOnline("slave1");

        int progress = warmupManager.getWarmupProgress("slave1");
        assertTrue(progress >= 0 && progress <= 100);
    }

    @Test
    @DisplayName("未记录的从库预热进度应该是 100")
    void unknownSlaveWarmupProgressShouldBe100() {
        SlaveWarmupManager warmupManager = new SlaveWarmupManager(60);

        assertEquals(100, warmupManager.getWarmupProgress("slave1"));
    }

    @Test
    @DisplayName("下线后应该清除预热状态")
    void offlineShouldClearWarmupState() {
        SlaveWarmupManager warmupManager = new SlaveWarmupManager(60);
        warmupManager.markOnline("slave1");
        warmupManager.markOffline("slave1");

        assertFalse(warmupManager.isWarmingUp("slave1"));
        assertEquals(100, warmupManager.getWarmupProgress("slave1"));
    }

    @Test
    @DisplayName("权重至少应该为 1")
    void weightShouldBeAtLeast1() {
        SlaveWarmupManager warmupManager = new SlaveWarmupManager(10000);
        warmupManager.markOnline("slave1");

        int effectiveWeight = warmupManager.getEffectiveWeight("slave1", 100);
        assertTrue(effectiveWeight >= 1);
    }
}
