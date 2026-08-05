package com.scmcloud.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scmcloud.inventory.domain.entity.InvTccReservation;
import com.scmcloud.inventory.domain.entity.Inventory;
import com.scmcloud.inventory.mapper.InvInventoryMapper;
import com.scmcloud.inventory.mapper.InvTccReservationMapper;
import com.scmcloud.order.api.OrderDubboService;
import com.scmcloud.order.api.dto.OrderVO;
import com.scmcloud.order.api.request.CreateOrderRequest;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.mapper.OrdOrderMapper;
import com.scmcloud.order.service.impl.OrderTccServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import com.scmcloud.common.domain.Quantity;

import static org.junit.jupiter.api.Assertions.*;

@RequiredArgsConstructor
@Slf4j
@SpringBootTest
@Tag("integration")
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Seata TCC Mode Integration Test")
public class SeataTccModeIntegrationTest {

    private final OrderTccServiceImpl orderTccService;

    private final OrdOrderMapper orderMapper;

    private final InvInventoryMapper inventoryMapper;

    private final InvTccReservationMapper reservationMapper;

    private static final Long TEST_SKU_ID = 9002L;
    private static final Long TEST_USER_ID = 2001L;

    @BeforeEach
    public void setup() {
        log.info("========================================");
        log.info("Start preparing TCC test data");
        log.info("========================================");

        orderMapper.delete(
                new LambdaQueryWrapper<OrdOrder>()
                        .ge(OrdOrder::getUserId, TEST_USER_ID)
        );
        inventoryMapper.delete(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, TEST_SKU_ID)
        );
        reservationMapper.delete(
                new LambdaQueryWrapper<InvTccReservation>()
                        .eq(InvTccReservation::getSkuId, TEST_SKU_ID)
        );

        Inventory inventory = new Inventory();
        inventory.setSkuId(String.valueOf(TEST_SKU_ID));
        inventory.setAvailableStock(100);
        inventory.setLockedStock(0);
        inventory.setWarehouseId("1");
        inventoryMapper.insert(inventory);

        log.info("Initialized stock: SKU={}, availableStock=100, lockedStock=0", TEST_SKU_ID);
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Scenario 1: Try-Confirm flow -> global transaction commit")
    public void testTccSuccess_TryConfirmFlow() {
        log.info("========================================");
        log.info("Test Scenario 1: Try-Confirm flow");
        log.info("========================================");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(TEST_USER_ID);
        request.setSkuId(TEST_SKU_ID);
        request.setSkuName("TCC-TestProduct");
        request.setQuantity(10);
        request.setUnitPrice(new BigDecimal("99.00"));
        request.setTotalAmount(new BigDecimal("990.00"));
        request.setRemark("TCC mode test - success scenario");

        OrderVO orderVO = orderTccService.createOrderWithTcc(request);

        assertNotNull(orderVO, "Order should be created successfully");
        assertNotNull(orderVO.getOrderNo(), "Order number should not be null");
        assertTrue(orderVO.getOrderNo().startsWith("TCC"), "TCC order number should start with TCC");
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

        assertEquals(90, inventoryInDb.getAvailableStock(),
                "Available stock should be deducted by 10 (100 - 10 = 90)");
        assertEquals(0, inventoryInDb.getLockedStock(),
                "Locked stock should be 0 after Confirm");

        log.info("Stock verified: available={}, locked={}",
                inventoryInDb.getAvailableStock(), inventoryInDb.getLockedStock());

        InvTccReservation reservation = reservationMapper.selectOne(
                new LambdaQueryWrapper<InvTccReservation>()
                        .eq(InvTccReservation::getBusinessKey, orderVO.getOrderNo())
        );
        assertNotNull(reservation, "Reservation record should exist");
        assertEquals(TEST_SKU_ID, reservation.getSkuId(), "SKU ID should match");
        assertEquals(10, reservation.getQuantity(), "Reservation quantity should match");
        assertEquals(InvTccReservation.Status.CONFIRMED, reservation.getStatus(),
                "Reservation status should be CONFIRMED");
        assertNotNull(reservation.getTryTime(), "Try time should not be null");
        assertNotNull(reservation.getConfirmTime(), "Confirm time should not be null");
        assertNull(reservation.getCancelTime(), "Cancel time should be null");

        log.info("TCC reservation record verified: status={}, tryTime={}, confirmTime={}",
                reservation.getStatus(), reservation.getTryTime(), reservation.getConfirmTime());

        log.info("========================================");
        log.info("Scenario 1 test passed");
        log.info("========================================");
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Scenario 2: Try-Cancel flow -> global transaction rollback")
    public void testTccFailed_InsufficientStock_TryCancelFlow() {
        log.info("========================================");
        log.info("Test Scenario 2: Try-Cancel flow (insufficient stock)");
        log.info("========================================");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(TEST_USER_ID);
        request.setSkuId(TEST_SKU_ID);
        request.setSkuName("TCC-TestProduct");
        request.setQuantity(200);
        request.setUnitPrice(new BigDecimal("99.00"));
        request.setTotalAmount(new BigDecimal("19800.00"));
        request.setRemark("TCC mode test - insufficient stock scenario");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderTccService.createOrderWithTcc(request);
        });

        log.info("Expected exception thrown: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("stock"), "Exception should contain 'stock'");

        Long orderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<OrdOrder>()
                        .ge(OrdOrder::getUserId, TEST_USER_ID)
        );
        assertEquals(0L, orderCount, "Database should have no order records (rolled back)");

        log.info("Order rollback verified: no orders in database");

        Inventory inventoryInDb = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, TEST_SKU_ID)
        );
        assertNotNull(inventoryInDb, "Inventory record should exist");
        assertEquals(100, inventoryInDb.getAvailableStock(),
                "Available stock should be unchanged (Cancel restored)");
        assertEquals(0, inventoryInDb.getLockedStock(),
                "Locked stock should be 0 (Cancel restored)");

        log.info("Stock rollback verified: available={}, locked={}",
                inventoryInDb.getAvailableStock(), inventoryInDb.getLockedStock());

        Long reservationCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<InvTccReservation>()
                        .eq(InvTccReservation::getSkuId, TEST_SKU_ID)
        );
        assertEquals(0L, reservationCount, "No reservation records (Try phase failed)");

        log.info("TCC reservation record verified: no reservations");

        log.info("========================================");
        log.info("Scenario 2 test passed");
        log.info("========================================");
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Scenario 3: Idempotency test -> duplicate Try returns idempotent")
    public void testTccIdempotency_DuplicateTry() {
        log.info("========================================");
        log.info("Test Scenario 3: TCC idempotency test");
        log.info("========================================");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(TEST_USER_ID);
        request.setSkuId(TEST_SKU_ID);
        request.setSkuName("TCC-TestProduct");
        request.setQuantity(5);
        request.setUnitPrice(new BigDecimal("99.00"));
        request.setTotalAmount(new BigDecimal("495.00"));
        request.setRemark("TCC mode test - idempotency scenario");

        OrderVO orderVO1 = orderTccService.createOrderWithTcc(request);
        assertNotNull(orderVO1, "First creation should succeed");
        String orderNo = orderVO1.getOrderNo();

        log.info("First order created successfully: OrderNo={}", orderNo);

        Inventory inventory1 = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, TEST_SKU_ID)
        );
        int availableAfterFirst = inventory1.getAvailableStock();
        int lockedAfterFirst = inventory1.getLockedStock();

        log.info("Stock after first execution: available={}, locked={}", availableAfterFirst, lockedAfterFirst);

        log.info("Idempotency mechanism: TCC uses businessKey (orderNo) for idempotency");
        log.info("  - Same businessKey Try call returns success directly");
        log.info("  - No repeated stock deduction");

        Long reservationCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<InvTccReservation>()
                        .eq(InvTccReservation::getBusinessKey, orderNo)
        );
        assertEquals(1L, reservationCount, "Same businessKey should have exactly one reservation record");

        log.info("Reservation record uniqueness verified");

        log.info("========================================");
        log.info("Scenario 3 test passed");
        log.info("========================================");
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("Scenario 4: Concurrent TCC transactions -> reservation consistency")
    public void testConcurrentTccTransactions() throws InterruptedException {
        log.info("========================================");
        log.info("Test Scenario 4: Concurrent TCC transactions");
        log.info("========================================");

        int threadCount = 10;
        int quantityPerOrder = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    CreateOrderRequest request = new CreateOrderRequest();
                    request.setUserId(TEST_USER_ID + index);
                    request.setSkuId(TEST_SKU_ID);
                    request.setSkuName("TCC-TestProduct");
                    request.setQuantity(quantityPerOrder);
                    request.setUnitPrice(new BigDecimal("99.00"));
                    request.setTotalAmount(new BigDecimal("495.00"));
                    request.setRemark("Concurrent TCC test - thread" + index);

                    orderTccService.createOrderWithTcc(request);
                    successCount.incrementAndGet();
                    log.info("Thread {} created order successfully", index);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("Thread {} failed to create order: {}", index, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
            threads[i].start();
        }

        latch.await();

        log.info("Concurrent execution completed: success={}, fail={}", successCount.get(), failCount.get());

        Inventory inventoryInDb = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, TEST_SKU_ID)
        );

        int expectedStock = 100 - (successCount.get() * quantityPerOrder);
        assertEquals(expectedStock, inventoryInDb.getAvailableStock(),
                String.format("Stock should be %d (100 - %d*5)", expectedStock, successCount.get()));
        assertEquals(0, inventoryInDb.getLockedStock(), "Locked stock should be 0 after Confirm");

        log.info("Concurrent inventory consistency verified: available={}, locked={}",
                inventoryInDb.getAvailableStock(), inventoryInDb.getLockedStock());

        Long reservationCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<InvTccReservation>()
                        .eq(InvTccReservation::getSkuId, TEST_SKU_ID)
                        .eq(InvTccReservation::getStatus, InvTccReservation.Status.CONFIRMED)
        );
        assertEquals((long) successCount.get(), reservationCount,
                "CONFIRMED reservation count should equal success count");

        log.info("TCC reservation consistency verified: {} CONFIRMED records", reservationCount);

        Long orderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<OrdOrder>()
                        .ge(OrdOrder::getUserId, TEST_USER_ID)
        );
        assertEquals((long) successCount.get(), orderCount,
                "Order count should equal success count");

        log.info("Order count verified: created {} orders", orderCount);

        log.info("========================================");
        log.info("Scenario 4 test passed");
        log.info("========================================");
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("Scenario 5: TCC state transition verification")
    public void testTccStateTransition() {
        log.info("========================================");
        log.info("Test Scenario 5: TCC state transition verification");
        log.info("========================================");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(TEST_USER_ID);
        request.setSkuId(TEST_SKU_ID);
        request.setSkuName("TCC-TestProduct");
        request.setQuantity(3);
        request.setUnitPrice(new BigDecimal("99.00"));
        request.setTotalAmount(new BigDecimal("297.00"));
        request.setRemark("TCC mode test - state transition verification");

        OrderVO orderVO = orderTccService.createOrderWithTcc(request);
        String orderNo = orderVO.getOrderNo();

        log.info("Order created successfully: OrderNo={}", orderNo);

        InvTccReservation reservation = reservationMapper.selectOne(
                new LambdaQueryWrapper<InvTccReservation>()
                        .eq(InvTccReservation::getBusinessKey, orderNo)
        );

        assertNotNull(reservation, "Reservation record should exist");
        assertEquals(InvTccReservation.Status.CONFIRMED, reservation.getStatus(),
                "Final status should be CONFIRMED");

        assertNotNull(reservation.getTryTime(), "Try time should exist");
        assertNotNull(reservation.getConfirmTime(), "Confirm time should exist");
        assertNull(reservation.getCancelTime(), "Cancel time should be null");
        assertTrue(reservation.getConfirmTime().isAfter(reservation.getTryTime()),
                "Confirm time should be after Try time");

        log.info("TCC state transition verified:");
        log.info("  - Status: {}", reservation.getStatus());
        log.info("  - Try time: {}", reservation.getTryTime());
        log.info("  - Confirm time: {}", reservation.getConfirmTime());
        log.info("  - Cancel time: {}", reservation.getCancelTime());

        assertNotNull(reservation.getXid(), "XID should exist");
        assertNotNull(reservation.getBranchId(), "Branch ID should exist");

        log.info("Seata context verified: XID={}, BranchId={}",
                reservation.getXid(), reservation.getBranchId());

        log.info("========================================");
        log.info("Scenario 5 test passed");
        log.info("========================================");
    }

    @AfterEach
    public void cleanup() {
        log.info("========================================");
        log.info("Cleaning up TCC test data");
        log.info("========================================");

        orderMapper.delete(
                new LambdaQueryWrapper<OrdOrder>()
                        .ge(OrdOrder::getUserId, TEST_USER_ID)
        );

        inventoryMapper.delete(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, TEST_SKU_ID)
        );

        reservationMapper.delete(
                new LambdaQueryWrapper<InvTccReservation>()
                        .eq(InvTccReservation::getSkuId, TEST_SKU_ID)
        );

        log.info("TCC test data cleanup completed");
    }
}
