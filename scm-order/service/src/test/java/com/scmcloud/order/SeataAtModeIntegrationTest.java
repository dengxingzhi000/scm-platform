package com.scmcloud.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scmcloud.inventory.api.InventoryDubboService;
import com.scmcloud.inventory.domain.entity.Inventory;
import com.scmcloud.inventory.mapper.InvInventoryMapper;
        import com.scmcloud.order.api.OrderDubboService;
import com.scmcloud.order.api.dto.OrderVO;
import com.scmcloud.order.api.request.CreateOrderRequest;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.mapper.OrdOrderMapper;
import com.scmcloud.order.service.dubbo.OrderDubboServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import com.scmcloud.common.domain.Quantity;

import static org.junit.jupiter.api.Assertions.*;

@RequiredArgsConstructor
@Slf4j
@SpringBootTest
@Tag("integration")
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Seata AT Mode Integration Test")
public class SeataAtModeIntegrationTest {

    private final OrderDubboServiceImpl orderService;

    private final OrdOrderMapper orderMapper;

    private final InvInventoryMapper inventoryMapper;

    @DubboReference(version = "1.0.0", group = "scm", check = false)
    private InventoryDubboService inventoryService;

    private static final Long TEST_SKU_ID = 9001L;
    private static final Long TEST_USER_ID = 1001L;

    @BeforeEach
    public void setup() {
        log.info("========================================");
        log.info("Start preparing test data");
        log.info("========================================");

        orderMapper.delete(
                new LambdaQueryWrapper<OrdOrder>()
                        .eq(OrdOrder::getUserId, TEST_USER_ID)
        );
        inventoryMapper.delete(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, TEST_SKU_ID)
        );

        Inventory inventory = new Inventory();
        inventory.setSkuId(String.valueOf(TEST_SKU_ID));
        inventory.setAvailableStock(100);
        inventory.setLockedStock(0);
        inventory.setWarehouseId("1");
        inventoryMapper.insert(inventory);

        log.info("Initialized stock: SKU={}, availableStock=100", TEST_SKU_ID);
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Scenario 1: Order created, stock deducted -> global commit")
    public void testCreateOrderSuccess_CommitTransaction() {
        log.info("========================================");
        log.info("Test Scenario 1: Order success, global transaction commit");
        log.info("========================================");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(TEST_USER_ID);
        request.setSkuId(TEST_SKU_ID);
        request.setSkuName("TestProduct");
        request.setQuantity(10);
        request.setUnitPrice(new BigDecimal("99.00"));
        request.setTotalAmount(new BigDecimal("990.00"));
        request.setRemark("AT mode test - success scenario");

        OrderVO orderVO = orderService.createOrder(request);

        assertNotNull(orderVO, "Order should be created successfully");
        assertNotNull(orderVO.getOrderNo(), "Order number should not be null");
        assertEquals("PENDING_PAYMENT", orderVO.getStatus(), "Order status should be PENDING_PAYMENT");

        log.info("Order created successfully: OrderNo={}", orderVO.getOrderNo());

        OrdOrder orderInDb = orderMapper.selectOne(
                new LambdaQueryWrapper<OrdOrder>()
                        .eq(OrdOrder::getOrderNo, orderVO.getOrderNo())
        );
        assertNotNull(orderInDb, "Order should exist in database");
        assertEquals(TEST_SKU_ID, orderInDb.getSkuId(), "SKU ID should match");
        assertEquals(Quantity.of(10), orderInDb.getQuantity(), "Quantity should match");

        log.info("Database order verification passed");

        Inventory inventoryInDb = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, TEST_SKU_ID)
        );
        assertNotNull(inventoryInDb, "Inventory record should exist");
        assertEquals(90, inventoryInDb.getAvailableStock(), "Available stock should be deducted by 10 (100 - 10 = 90)");

        log.info("Stock deduction verified: remainingStock={}", inventoryInDb.getAvailableStock());

        log.info("========================================");
        log.info("Scenario 1 test passed");
        log.info("========================================");
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Scenario 2: Order created, insufficient stock -> global rollback")
    public void testCreateOrderFailed_InsufficientStock_RollbackTransaction() {
        log.info("========================================");
        log.info("Test Scenario 2: Insufficient stock, global transaction rollback");
        log.info("========================================");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(TEST_USER_ID);
        request.setSkuId(TEST_SKU_ID);
        request.setSkuName("TestProduct");
        request.setQuantity(200);
        request.setUnitPrice(new BigDecimal("99.00"));
        request.setTotalAmount(new BigDecimal("19800.00"));
        request.setRemark("AT mode test - insufficient stock scenario");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(request);
        });

        log.info("Expected exception thrown: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("insufficient"), "Exception should contain 'insufficient'");

        Long orderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<OrdOrder>()
                        .eq(OrdOrder::getUserId, TEST_USER_ID)
        );
        assertEquals(0L, orderCount, "Database should have no order records (rolled back)");

        log.info("Order rollback verified: no orders in database");

        Inventory inventoryInDb = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, TEST_SKU_ID)
        );
        assertNotNull(inventoryInDb, "Inventory record should exist");
        assertEquals(100, inventoryInDb.getAvailableStock(), "Available stock should be unchanged (rollback succeeded)");

        log.info("Stock rollback verified: stock unchanged at 100");

        log.info("========================================");
        log.info("Scenario 2 test passed");
        log.info("========================================");
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Scenario 3: Concurrent order creation -> inventory consistency check")
    public void testConcurrentOrderCreation_InventoryConsistency() throws InterruptedException {
        log.info("========================================");
        log.info("Test Scenario 3: Concurrent order creation, inventory consistency check");
        log.info("========================================");

        int threadCount = 10;
        int quantityPerOrder = 5;

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    CreateOrderRequest request = new CreateOrderRequest();
                    request.setUserId(TEST_USER_ID + index);
                    request.setSkuId(TEST_SKU_ID);
                    request.setSkuName("TestProduct");
                    request.setQuantity(quantityPerOrder);
                    request.setUnitPrice(new BigDecimal("99.00"));
                    request.setTotalAmount(new BigDecimal("495.00"));
                    request.setRemark("Concurrent test - thread" + index);

                    orderService.createOrder(request);
                    log.info("Thread {} created order successfully", index);
                } catch (Exception e) {
                    log.error("Thread {} failed to create order: {}", index, e.getMessage());
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Inventory inventoryInDb = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, TEST_SKU_ID)
        );

        int expectedStock = 100 - (threadCount * quantityPerOrder);
        assertEquals(expectedStock, inventoryInDb.getAvailableStock(),
                String.format("Stock should be %d (100 - 10*5)", expectedStock));

        log.info("Concurrent inventory consistency verified: remainingStock={}", inventoryInDb.getAvailableStock());

        Long orderCount = orderMapper.selectCount(null);
        assertEquals(threadCount, orderCount, "Should have created 10 orders");

        log.info("Order count verified: created {} orders", orderCount);

        log.info("========================================");
        log.info("Scenario 3 test passed");
        log.info("========================================");
    }

    @AfterEach
    public void cleanup() {
        log.info("========================================");
        log.info("Cleaning up test data");
        log.info("========================================");

        orderMapper.delete(
                new LambdaQueryWrapper<OrdOrder>()
                        .ge(OrdOrder::getUserId, TEST_USER_ID)
        );

        inventoryMapper.delete(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, TEST_SKU_ID)
        );

        log.info("Test data cleanup completed");
    }
}
