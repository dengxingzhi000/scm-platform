package com.frog.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.frog.inventory.api.InventoryDubboService;
import com.frog.inventory.domain.entity.Inventory;
import com.frog.inventory.mapper.InvInventoryMapper;
import com.frog.order.api.OrderDubboService;
import com.frog.order.domain.entity.Order;
import com.frog.order.mapper.OrdOrderMapper;
import com.frog.order.service.impl.OrderDubboServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Seata AT 模式集成测试
 *
 * <p>测试场景：
 * 1. 订单创建成功 + 库存扣减成功 → 全局事务提交
 * 2. 订单创建成功 + 库存不足 → 全局事务回滚
 * 3. 订单创建失败 → 全局事务回滚
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Seata AT 模式集成测试")
public class SeataAtModeIntegrationTest {

    @Autowired
    private OrderDubboServiceImpl orderService;

    @Autowired
    private OrdOrderMapper orderMapper;

    @Autowired
    private InvInventoryMapper inventoryMapper;

    @DubboReference(version = "1.0.0", group = "scm", check = false)
    private InventoryDubboService inventoryService;

    private static final Long TEST_SKU_ID = 9001L;
    private static final Long TEST_USER_ID = 1001L;

    /**
     * 准备测试数据
     */
    @BeforeEach
    public void setup() {
        log.info("========================================");
        log.info("开始准备测试数据");
        log.info("========================================");

        // 清理测试数据
        orderMapper.delete(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, TEST_USER_ID)
        );
        inventoryMapper.delete(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, TEST_SKU_ID)
        );

        // 初始化库存
        Inventory inventory = new Inventory();
        inventory.setSkuId(TEST_SKU_ID);
        inventory.setAvailableStock(100);
        inventory.setLockedStock(0);
        inventory.setWarehouseId(1L);
        inventoryMapper.insert(inventory);

        log.info("✓ 初始化库存: SKU={}, 可用库存=100", TEST_SKU_ID);
    }

    /**
     * 场景 1: 订单创建成功，库存充足 → 全局事务提交
     */
    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("场景1: 订单创建成功 + 库存扣减成功 → 全局事务提交")
    public void testCreateOrderSuccess_CommitTransaction() {
        log.info("========================================");
        log.info("测试场景 1: 订单创建成功，全局事务提交");
        log.info("========================================");

        // 1. 准备请求
        OrderDubboService.CreateOrderRequest request = new OrderDubboService.CreateOrderRequest();
        request.setUserId(TEST_USER_ID);
        request.setSkuId(TEST_SKU_ID);
        request.setSkuName("测试商品");
        request.setQuantity(10);
        request.setUnitPrice(new BigDecimal("99.00"));
        request.setTotalAmount(new BigDecimal("990.00"));
        request.setRemark("AT模式测试-成功场景");

        // 2. 执行创建订单
        OrderDubboService.OrderVO orderVO = orderService.createOrder(request);

        // 3. 验证订单创建成功
        assertNotNull(orderVO, "订单应该创建成功");
        assertNotNull(orderVO.getOrderNo(), "订单号不应为空");
        assertEquals("PENDING_PAYMENT", orderVO.getStatus(), "订单状态应为待支付");

        log.info("✓ 订单创建成功: OrderNo={}", orderVO.getOrderNo());

        // 4. 验证数据库中订单记录存在
        Order orderInDb = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderVO.getOrderNo())
        );
        assertNotNull(orderInDb, "数据库中应该存在订单记录");
        assertEquals(TEST_SKU_ID, orderInDb.getSkuId(), "SKU ID 应该匹配");
        assertEquals(10, orderInDb.getQuantity(), "数量应该匹配");

        log.info("✓ 数据库订单验证通过");

        // 5. 验证库存扣减成功
        Inventory inventoryInDb = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, TEST_SKU_ID)
        );
        assertNotNull(inventoryInDb, "库存记录应该存在");
        assertEquals(90, inventoryInDb.getAvailableStock(), "可用库存应该扣减 10（100 - 10 = 90）");

        log.info("✓ 库存扣减验证通过: 剩余库存={}", inventoryInDb.getAvailableStock());

        log.info("========================================");
        log.info("场景 1 测试通过 ✓");
        log.info("========================================");
    }

    /**
     * 场景 2: 订单创建成功，但库存不足 → 全局事务回滚
     */
    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("场景2: 订单创建 + 库存不足 → 全局事务回滚")
    public void testCreateOrderFailed_InsufficientStock_RollbackTransaction() {
        log.info("========================================");
        log.info("测试场景 2: 库存不足，全局事务回滚");
        log.info("========================================");

        // 1. 准备请求（数量超过库存）
        OrderDubboService.CreateOrderRequest request = new OrderDubboService.CreateOrderRequest();
        request.setUserId(TEST_USER_ID);
        request.setSkuId(TEST_SKU_ID);
        request.setSkuName("测试商品");
        request.setQuantity(200);  // 需要 200，但库存只有 100
        request.setUnitPrice(new BigDecimal("99.00"));
        request.setTotalAmount(new BigDecimal("19800.00"));
        request.setRemark("AT模式测试-库存不足场景");

        // 2. 执行创建订单（应该抛出异常）
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(request);
        });

        log.info("✓ 预期异常抛出: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("库存不足"), "异常信息应包含'库存不足'");

        // 3. 验证订单未创建（回滚成功）
        Long orderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, TEST_USER_ID)
        );
        assertEquals(0L, orderCount, "数据库中不应该有订单记录（已回滚）");

        log.info("✓ 订单回滚验证通过: 数据库中无订单记录");

        // 4. 验证库存未扣减（回滚成功）
        Inventory inventoryInDb = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, TEST_SKU_ID)
        );
        assertNotNull(inventoryInDb, "库存记录应该存在");
        assertEquals(100, inventoryInDb.getAvailableStock(), "可用库存应该未变化（回滚成功）");

        log.info("✓ 库存回滚验证通过: 库存未扣减，仍为 100");

        log.info("========================================");
        log.info("场景 2 测试通过 ✓");
        log.info("========================================");
    }

    /**
     * 场景 3: 并发场景 - 多个订单同时创建
     */
    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("场景3: 并发创建订单 → 库存一致性验证")
    public void testConcurrentOrderCreation_InventoryConsistency() throws InterruptedException {
        log.info("========================================");
        log.info("测试场景 3: 并发订单创建，库存一致性验证");
        log.info("========================================");

        int threadCount = 10;
        int quantityPerOrder = 5;

        Thread[] threads = new Thread[threadCount];

        // 启动 10 个线程，每个线程创建一个订单，扣减 5 库存
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    OrderDubboService.CreateOrderRequest request = new OrderDubboService.CreateOrderRequest();
                    request.setUserId(TEST_USER_ID + index);
                    request.setSkuId(TEST_SKU_ID);
                    request.setSkuName("测试商品");
                    request.setQuantity(quantityPerOrder);
                    request.setUnitPrice(new BigDecimal("99.00"));
                    request.setTotalAmount(new BigDecimal("495.00"));
                    request.setRemark("并发测试-线程" + index);

                    orderService.createOrder(request);
                    log.info("✓ 线程 {} 创建订单成功", index);
                } catch (Exception e) {
                    log.error("✗ 线程 {} 创建订单失败: {}", index, e.getMessage());
                }
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证库存一致性
        Inventory inventoryInDb = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, TEST_SKU_ID)
        );

        int expectedStock = 100 - (threadCount * quantityPerOrder);  // 100 - 50 = 50
        assertEquals(expectedStock, inventoryInDb.getAvailableStock(),
                String.format("库存应该为 %d（100 - 10*5）", expectedStock));

        log.info("✓ 并发库存一致性验证通过: 剩余库存={}", inventoryInDb.getAvailableStock());

        // 验证订单数量
        Long orderCount = orderMapper.selectCount(null);
        assertEquals(threadCount, orderCount, "应该创建 10 个订单");

        log.info("✓ 订单数量验证通过: 共创建 {} 个订单", orderCount);

        log.info("========================================");
        log.info("场景 3 测试通过 ✓");
        log.info("========================================");
    }

    /**
     * 清理测试数据
     */
    @AfterEach
    public void cleanup() {
        log.info("========================================");
        log.info("清理测试数据");
        log.info("========================================");

        // 清理订单
        orderMapper.delete(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getUserId, TEST_USER_ID)
        );

        // 清理库存
        inventoryMapper.delete(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, TEST_SKU_ID)
        );

        log.info("✓ 测试数据清理完成");
    }
}