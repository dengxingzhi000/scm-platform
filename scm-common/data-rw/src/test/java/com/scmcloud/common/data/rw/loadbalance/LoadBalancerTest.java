package com.scmcloud.common.data.rw.loadbalance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("负载均衡器测试")
class LoadBalancerTest {

    private List<SlaveLoadBalancer.SlaveInfo> createSlaves(int count) {
        List<SlaveLoadBalancer.SlaveInfo> slaves = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            slaves.add(new SlaveLoadBalancer.SlaveInfo("slave" + i, 1, 0, true));
        }
        return slaves;
    }

    private List<SlaveLoadBalancer.SlaveInfo> createWeightedSlaves() {
        List<SlaveLoadBalancer.SlaveInfo> slaves = new ArrayList<>();
        slaves.add(new SlaveLoadBalancer.SlaveInfo("slave1", 5, 0, true));
        slaves.add(new SlaveLoadBalancer.SlaveInfo("slave2", 3, 0, true));
        slaves.add(new SlaveLoadBalancer.SlaveInfo("slave3", 2, 0, true));
        return slaves;
    }

    @Test
    @DisplayName("轮询负载均衡器应该均匀分配")
    void roundRobinShouldDistributeEvenly() {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer();
        List<SlaveLoadBalancer.SlaveInfo> slaves = createSlaves(3);

        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < 30; i++) {
            String selected = loadBalancer.select(slaves);
            counts.merge(selected, 1, Integer::sum);
        }

        assertEquals(10, counts.get("slave1"));
        assertEquals(10, counts.get("slave2"));
        assertEquals(10, counts.get("slave3"));
    }

    @Test
    @DisplayName("随机负载均衡器应该选择可用节点")
    void randomShouldSelectAvailableNode() {
        RandomLoadBalancer loadBalancer = new RandomLoadBalancer();
        List<SlaveLoadBalancer.SlaveInfo> slaves = createSlaves(3);

        for (int i = 0; i < 100; i++) {
            String selected = loadBalancer.select(slaves);
            assertNotNull(selected);
            assertTrue(selected.startsWith("slave"));
        }
    }

    @Test
    @DisplayName("加权轮询负载均衡器应该按权重分配")
    void weightedRoundRobinShouldDistributeByWeight() {
        WeightedRoundRobinLoadBalancer loadBalancer = new WeightedRoundRobinLoadBalancer();
        List<SlaveLoadBalancer.SlaveInfo> slaves = createWeightedSlaves();

        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            String selected = loadBalancer.select(slaves);
            counts.merge(selected, 1, Integer::sum);
        }

        // 权重 5:3:2，应该大致按此比例分配
        assertTrue(counts.get("slave1") > counts.get("slave2"));
        assertTrue(counts.get("slave2") > counts.get("slave3"));
    }

    @Test
    @DisplayName("加权随机负载均衡器应该按权重分配")
    void weightedRandomShouldDistributeByWeight() {
        WeightedRandomLoadBalancer loadBalancer = new WeightedRandomLoadBalancer();
        List<SlaveLoadBalancer.SlaveInfo> slaves = createWeightedSlaves();

        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            String selected = loadBalancer.select(slaves);
            counts.merge(selected, 1, Integer::sum);
        }

        // 权重 5:3:2，应该大致按此比例分配
        assertTrue(counts.get("slave1") > counts.get("slave2"));
        assertTrue(counts.get("slave2") > counts.get("slave3"));
    }

    @Test
    @DisplayName("最少连接负载均衡器应该选择连接数最少的节点")
    void leastConnectionsShouldSelectLeastConnected() {
        LeastConnectionsLoadBalancer loadBalancer = new LeastConnectionsLoadBalancer();
        List<SlaveLoadBalancer.SlaveInfo> slaves = new ArrayList<>();
        slaves.add(new SlaveLoadBalancer.SlaveInfo("slave1", 1, 10, true));
        slaves.add(new SlaveLoadBalancer.SlaveInfo("slave2", 1, 5, true));
        slaves.add(new SlaveLoadBalancer.SlaveInfo("slave3", 1, 15, true));

        String selected = loadBalancer.select(slaves);
        assertEquals("slave2", selected);
    }

    @Test
    @DisplayName("应该过滤不可用的节点")
    void shouldFilterUnavailableNodes() {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer();
        List<SlaveLoadBalancer.SlaveInfo> slaves = new ArrayList<>();
        slaves.add(new SlaveLoadBalancer.SlaveInfo("slave1", 1, 0, false));
        slaves.add(new SlaveLoadBalancer.SlaveInfo("slave2", 1, 0, true));
        slaves.add(new SlaveLoadBalancer.SlaveInfo("slave3", 1, 0, false));

        for (int i = 0; i < 10; i++) {
            assertEquals("slave2", loadBalancer.select(slaves));
        }
    }

    @Test
    @DisplayName("所有节点不可用时应该返回 null")
    void shouldReturnNullWhenAllUnavailable() {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer();
        List<SlaveLoadBalancer.SlaveInfo> slaves = new ArrayList<>();
        slaves.add(new SlaveLoadBalancer.SlaveInfo("slave1", 1, 0, false));
        slaves.add(new SlaveLoadBalancer.SlaveInfo("slave2", 1, 0, false));

        assertNull(loadBalancer.select(slaves));
    }

    @Test
    @DisplayName("空列表应该返回 null")
    void shouldReturnNullForEmptyList() {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer();
        assertNull(loadBalancer.select(List.of()));
        assertNull(loadBalancer.select(null));
    }
}
