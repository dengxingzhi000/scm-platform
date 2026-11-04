package com.cognora.platform.inventory.service.command;

import com.cognora.platform.common.integration.outbox.OutboxService;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.dto.InventoryAdjustRequest;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 库存调整的并发与超扣用例。
 *
 * <p>用 AtomicInteger 模拟数据库条件更新
 * {@code WHERE available_stock + quantity >= 0} 的原子性。
 */
class InventoryAdjustConcurrencyTest {

    private InvInventoryMapper inventoryMapper;
    private OutboxService outboxService;
    private InvInventoryCommandService service;

    private final AtomicInteger available = new AtomicInteger(5);
    private final AtomicInteger total = new AtomicInteger(5);

    @BeforeEach
    void setUp() {
        inventoryMapper = mock(InvInventoryMapper.class);
        outboxService = mock(OutboxService.class);

        Inventory inventory = new Inventory();
        inventory.setId("11111111-1111-1111-1111-111111111111");
        inventory.setSkuId("sku-1");
        inventory.setWarehouseId("wh-1");
        inventory.setAvailableStock(5);
        inventory.setTotalStock(5);
        inventory.setLockedStock(0);
        inventory.setSafetyStock(0);
        when(inventoryMapper.selectOne(any())).thenReturn(inventory);

        when(inventoryMapper.applyAdjustment(anyString(), anyInt(), anyInt(), any()))
                .thenAnswer(invocation -> {
                    int quantity = invocation.getArgument(1);
                    synchronized (available) {
                        if (available.get() + quantity < 0) {
                            return 0;
                        }
                        available.addAndGet(quantity);
                        total.addAndGet(quantity);
                        return 1;
                    }
                });

        service = new InvInventoryCommandService(inventoryMapper, outboxService);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("并发扣减不超扣：可用量永不为负")
    void concurrentDeductionNeverGoesNegative() throws Exception {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        int threads = 20;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    TenantContextHolder.setTenantId(tenantId);
                    start.await();
                    service.adjustInventory(adjust(-1));
                    success.incrementAndGet();
                } catch (IllegalArgumentException e) {
                    rejected.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(5, success.get(), "库存 5，只允许成功扣减 5 次");
        assertEquals(15, rejected.get());
        assertEquals(0, available.get(), "可用量不得为负");
    }

    @Test
    @DisplayName("扣减超过可用量时抛业务异常且不写 outbox")
    void deductionBeyondAvailableIsRejected() {
        TenantContextHolder.setTenantId(UUID.randomUUID());
        available.set(1);

        assertThrows(IllegalArgumentException.class, () -> service.adjustInventory(adjust(-3)));
        verify(outboxService, never()).save(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("库存不存在且为扣减时直接拒绝")
    void deductFromMissingInventoryIsRejected() {
        TenantContextHolder.setTenantId(UUID.randomUUID());
        when(inventoryMapper.selectOne(any())).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.adjustInventory(adjust(-1)));
        verify(inventoryMapper, never()).insert(any(Inventory.class));
    }

    private InventoryAdjustRequest adjust(int quantity) {
        InventoryAdjustRequest request = new InventoryAdjustRequest();
        request.setSkuId("sku-1");
        request.setWarehouseId("wh-1");
        request.setQuantity(quantity);
        request.setAdjustType(quantity < 0 ? 2 : 1);
        request.setOperatorId("op-1");
        return request;
    }
}
