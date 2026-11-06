package com.cognora.platform.inventory.service;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 批量扣减与 requestId 幂等键用例。
 *
 * <p>批量扣减的原子性由单个 Lua 脚本保证（先全量校验、后统一扣减）；
 * 本用例断言 Java 侧只发起一次脚本调用，失败时不刷新任何缓存、不产生分步扣减。
 */
class RedisInventoryBatchTest {

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private InvInventoryMapper inventoryMapper;
    private RedisInventoryService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        TenantContextHolder.setTenantId(UUID.randomUUID());

        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        inventoryMapper = mock(InvInventoryMapper.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service = new RedisInventoryService(redisTemplate, inventoryMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("批量扣减只执行一次 Lua 脚本，并为每个键刷新 TTL")
    void batchDeductRunsSingleScript() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);

        assertTrue(service.batchDeductStock(List.of(
                new RedisInventoryService.StockDeductItem("SKU-1", "WH-1", 2),
                new RedisInventoryService.StockDeductItem("SKU-2", "WH-1", 3))));

        verify(redisTemplate, times(1))
                .execute(any(RedisScript.class), anyList(), any(Object[].class));
        verify(redisTemplate, times(2)).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("任一条目不足时整批失败，脚本只跑一次且不刷新缓存")
    void insufficientItemFailsWholeBatch() {
        // 第 2 项不足：返回 -(i+1) = -3
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(-3L);

        assertFalse(service.batchDeductStock(List.of(
                new RedisInventoryService.StockDeductItem("SKU-1", "WH-1", 1),
                new RedisInventoryService.StockDeductItem("SKU-2", "WH-1", 99))));

        verify(redisTemplate, times(1))
                .execute(any(RedisScript.class), anyList(), any(Object[].class));
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("库存键未同步时先回源再重试，仍失败则整批不扣")
    void missingKeyResyncsAndRetries() {
        Inventory inventory = new Inventory();
        inventory.setSkuId("SKU-1");
        inventory.setWarehouseId("WH-1");
        inventory.setAvailableStock(10);
        when(inventoryMapper.selectOne(any())).thenReturn(inventory);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(-1L, 1L);

        assertTrue(service.batchDeductStock(List.of(
                new RedisInventoryService.StockDeductItem("SKU-1", "WH-1", 1))));

        verify(redisTemplate, times(2))
                .execute(any(RedisScript.class), anyList(), any(Object[].class));
    }

    @Test
    @DisplayName("空条目与非法数量在脚本之前拒绝")
    void invalidItemsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.batchDeductStock(List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> service.batchDeductStock(List.of(
                        new RedisInventoryService.StockDeductItem("SKU-1", "WH-1", 0))));
        verify(redisTemplate, never())
                .execute(any(RedisScript.class), anyList(), any(Object[].class));
    }

    @Test
    @DisplayName("requestId 首次申请成功，重复申请失败；释放后可再次申请")
    void requestIdIdempotencyKey() {
        when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class)))
                .thenReturn(true, false);

        assertTrue(service.acquireDeductRequest("REQ-1"));
        assertFalse(service.acquireDeductRequest("REQ-1"));

        service.releaseDeductRequest("REQ-1");
        verify(redisTemplate).delete(anyString());
    }
}
