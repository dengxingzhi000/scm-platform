package com.cognora.platform.inventory.service.cost;

import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 并发入库不丢更新：用内存台账模拟 {@code WHERE version = ?} 的条件更新。
 *
 * <p>两个线程先同时读到 version=0，然后一个 CAS 成功、另一个失败并重读重试；
 * 最终数量与均价都应等于两次入库的加权结果。
 */
class MovingAverageCostServiceConcurrencyTest {

  private static final String INVENTORY_ID = "11111111-1111-1111-1111-111111111111";

  @Test
  @DisplayName("两个并发入库都成功且均价不丢更新")
  void concurrentInboundDoesNotLoseAverageUpdate() throws Exception {
    AtomicInteger version = new AtomicInteger(0);
    AtomicInteger total = new AtomicInteger(0);
    AtomicReference<BigDecimal> average = new AtomicReference<>(BigDecimal.ZERO);
    AtomicInteger reads = new AtomicInteger();
    CyclicBarrier firstReads = new CyclicBarrier(2);

    InvInventoryMapper mapper = mock(InvInventoryMapper.class);

    when(mapper.selectById(INVENTORY_ID)).thenAnswer(invocation -> {
      Inventory snapshot = new Inventory();
      snapshot.setId(INVENTORY_ID);
      snapshot.setSkuId("sku-1");
      snapshot.setWarehouseId("wh-1");
      snapshot.setTotalStock(total.get());
      snapshot.setAvailableStock(total.get());
      snapshot.setAverageCost(average.get());
      snapshot.setVersion(version.get());
      if (reads.getAndIncrement() < 2) {
        firstReads.await(10, TimeUnit.SECONDS);
      }
      return snapshot;
    });

    when(mapper.applyInboundWithCost(anyString(), anyInt(), org.mockito.ArgumentMatchers.any(),
        anyInt(), anyInt(), anyString())).thenAnswer(invocation -> {
      synchronized (version) {
        int expectedVersion = invocation.getArgument(3);
        int quantity = invocation.getArgument(1);
        BigDecimal newAverage = invocation.getArgument(2);
        if (version.get() != expectedVersion) {
          return 0;
        }
        total.addAndGet(quantity);
        average.set(newAverage);
        version.incrementAndGet();
        return 1;
      }
    });

    MovingAverageCostService service = new MovingAverageCostService(mapper);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    List<Future<CostMovement>> futures = new ArrayList<>();
    futures.add(pool.submit(inbound(service, "10.00")));
    futures.add(pool.submit(inbound(service, "20.00")));

    for (Future<CostMovement> future : futures) {
      assertTrue(future.get(20, TimeUnit.SECONDS).getDirection().equals("IN"));
    }
    pool.shutdownNow();

    assertEquals(20, total.get(), "两次入库数量都不能丢");
    assertEquals(2, version.get(), "每次成功入库都应推进版本");
    assertEquals(new BigDecimal("15.000000"), average.get(),
        "最终均价必须是 10 与 20 的加权平均");
  }

  private static Callable<CostMovement> inbound(MovingAverageCostService service, String price) {
    return () -> service.applyInbound(INVENTORY_ID, 10, new BigDecimal(price), 1, "op-1");
  }
}
