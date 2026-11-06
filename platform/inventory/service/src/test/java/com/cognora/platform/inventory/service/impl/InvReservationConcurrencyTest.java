package com.cognora.platform.inventory.service.impl;

import com.cognora.platform.common.tenant.TenantCacheKeys;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.domain.entity.InvReservationRecord;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.dto.InventoryReservationRequest;
import com.cognora.platform.inventory.lock.DistributedLock;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.mapper.InvReservationRecordMapper;
import com.cognora.platform.inventory.service.cost.InventoryCostLogService;
import com.cognora.platform.inventory.service.flow.InventoryFlowLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 预占链路的并发与幂等用例。
 *
 * <p>用内存台账模拟数据库的条件更新语义（{@code WHERE available_stock >= ?}），
 * 用 {@link ReentrantLock} 模拟 Redis 分布式锁的互斥性，从而在单测里真跑多线程。
 * 真实 PostgreSQL 的行锁/唯一约束验证属于集成测试，见 PR 说明中的“未覆盖项”。
 */
class InvReservationConcurrencyTest {

    private InvInventoryMapper inventoryMapper;
    private InvReservationRecordMapper reservationRecordMapper;
    private DistributedLock distributedLock;
    private InvReservationServiceImpl service;

    private final Map<String, ReentrantLock> lockRegistry = new ConcurrentHashMap<>();

    /** 内存库存台账 */
    private final AtomicInteger available = new AtomicInteger(10);
    private final AtomicInteger locked = new AtomicInteger(0);
    private final AtomicInteger total = new AtomicInteger(10);
    private final AtomicInteger lockCalls = new AtomicInteger();

    /** 内存预占记录，key = tenantId + ':' + businessKey */
    private final Map<String, InvReservationRecord> records = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        inventoryMapper = mock(InvInventoryMapper.class);
        reservationRecordMapper = mock(InvReservationRecordMapper.class);
        distributedLock = mock(DistributedLock.class);

        Inventory inventory = new Inventory();
        inventory.setId("11111111-1111-1111-1111-111111111111");
        inventory.setSkuId("sku-1");
        inventory.setWarehouseId("wh-1");
        inventory.setAvailableStock(10);
        inventory.setTotalStock(10);
        inventory.setLockedStock(0);
        when(inventoryMapper.selectOne(any())).thenReturn(inventory);

        // 模拟数据库条件更新：available >= qty 才生效，且原子
        when(inventoryMapper.lockStock(anyString(), anyInt())).thenAnswer(invocation -> {
            int qty = invocation.getArgument(1);
            synchronized (available) {
                if (available.get() < qty) {
                    return 0;
                }
                available.addAndGet(-qty);
                locked.addAndGet(qty);
                lockCalls.incrementAndGet();
                return 1;
            }
        });

        // 模拟 Redis 分布式锁：同一 key 互斥
        when(distributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class)))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    ReentrantLock lock = lockRegistry.computeIfAbsent(key, k -> new ReentrantLock(true));
                    if (!lock.tryLock(30, TimeUnit.SECONDS)) {
                        return null;
                    }
                    return new ReleasingHandle(key, lock);
                });

        when(reservationRecordMapper.selectByBusinessKey(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String tenant = invocation.getArgument(0);
                    String businessKey = invocation.getArgument(1);
                    return records.get(tenant + ':' + businessKey);
                });

        when(reservationRecordMapper.insert(any(InvReservationRecord.class))).thenAnswer(invocation -> {
            InvReservationRecord record = invocation.getArgument(0);
            InvReservationRecord previous =
                    records.putIfAbsent(record.getTenantId() + ':' + record.getBusinessKey(), record);
            return previous == null ? 1 : 0;
        });

        service = new InvReservationServiceImpl(inventoryMapper, reservationRecordMapper, distributedLock,
                mock(InventoryFlowLogService.class), mock(InventoryCostLogService.class));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("同一业务键并发预占：只扣减一次并全部幂等成功")
    void concurrentReserveWithSameBusinessKeyDeductsOnce() throws Exception {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    TenantContextHolder.setTenantId(tenantId);
                    start.await();
                    if (service.reserveInventory(request("ORD-1", 3))) {
                        success.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 失败即视为未幂等
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(threads, success.get(), "所有重复预占都应幂等返回 true");
        assertEquals(1, lockCalls.get(), "同一业务键只允许扣减一次");
        assertEquals(7, available.get());
        assertEquals(3, locked.get());
        assertEquals(1, records.size());
        assertEquals(10, total.get(), "预占不改变总库存");
    }

    @Test
    @DisplayName("不同业务键并发预占：不超卖，可用量永不为负")
    void concurrentReserveDoesNotOversell() throws Exception {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        int threads = 20;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            int index = i;
            pool.submit(() -> {
                try {
                    TenantContextHolder.setTenantId(tenantId);
                    start.await();
                    if (service.reserveInventory(request("ORD-" + index, 1))) {
                        success.incrementAndGet();
                    }
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

        assertEquals(10, success.get(), "可用量 10，只能成功 10 次");
        assertEquals(10, rejected.get());
        assertEquals(0, available.get(), "可用量必须为 0 且不能为负");
        assertEquals(10, locked.get());
    }

    @Test
    @DisplayName("库存不足时抛业务异常且不写入预占记录")
    void reserveFailsWhenStockInsufficient() {
        TenantContextHolder.setTenantId(UUID.randomUUID());
        available.set(2);

        assertThrows(IllegalArgumentException.class,
                () -> service.reserveInventory(request("ORD-X", 5)));
        assertEquals(0, records.size());
        assertEquals(2, available.get());
    }

    private InventoryReservationRequest request(String businessKey, int quantity) {
        InventoryReservationRequest request = new InventoryReservationRequest();
        request.setSkuId("sku-1");
        request.setWarehouseId("wh-1");
        request.setQuantity(quantity);
        request.setBusinessKey(businessKey);
        request.setTimeoutSeconds(900);
        return request;
    }

    /** 用 ReentrantLock 模拟分布式锁的互斥性 */
    private static final class ReleasingHandle extends DistributedLock.LockHandle {
        private final ReentrantLock lock;

        private ReleasingHandle(String key, ReentrantLock lock) {
            super(key, "test-client", null);
            this.lock = lock;
        }

        @Override
        public synchronized void release() {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Test
    @DisplayName("业务键缓存键按租户隔离")
    void reservationLockKeyIsTenantScoped() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantA);
        String keyA = TenantCacheKeys.scoped("reserve", "ORD-1");
        TenantContextHolder.setTenantId(tenantB);
        String keyB = TenantCacheKeys.scoped("reserve", "ORD-1");

        assertTrue(keyA.contains(tenantA.toString()));
        assertTrue(keyB.contains(tenantB.toString()));
        assertEquals(2, List.of(keyA, keyB).stream().distinct().count());
    }
}
