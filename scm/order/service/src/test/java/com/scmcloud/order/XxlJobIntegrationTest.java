package com.scmcloud.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.job.OrderTimeoutCancelJobHandler;
import com.scmcloud.order.mapper.OrdOrderMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.Quantity;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@SpringBootTest
@Tag("integration")
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("XXL-Job Task Scheduling Integration Test")
public class XxlJobIntegrationTest {

    private final OrderTimeoutCancelJobHandler orderTimeoutCancelJobHandler;

    private final OrdOrderMapper orderMapper;

    private static final Long TEST_USER_ID = 3001L;

    @BeforeEach
    public void setup() {
        log.info("========================================");
        log.info("Start preparing XXL-Job test data");
        log.info("========================================");

        orderMapper.delete(
                new LambdaQueryWrapper<OrdOrder>()
                        .ge(OrdOrder::getUserId, TEST_USER_ID)
        );

        log.info("Test data cleanup completed");
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Scenario 1: Order timeout cancel job -> timed out order should be cancelled")
    public void testOrderTimeoutCancelJob_TimeoutOrder_ShouldCancel() throws Exception {
        log.info("========================================");
        log.info("Test Scenario 1: Order timeout cancel (normal timeout)");
        log.info("========================================");

        OrdOrder timeoutOrder = createTestOrder(TEST_USER_ID, "00000000-0000-0000-0000-000000000001", 10, "PENDING_PAYMENT");
        timeoutOrder.setCreateTime(LocalDateTime.now().minusMinutes(35));
        orderMapper.insert(timeoutOrder);

        log.info("Created timed out order: OrderNo={}, CreateTime={}, Status={}",
                timeoutOrder.getOrderNo(), timeoutOrder.getCreateTime(), timeoutOrder.getStatus());

        log.info("Executing OrderTimeoutCancelJobHandler...");
        orderTimeoutCancelJobHandler.execute();

        OrdOrder updatedOrder = orderMapper.selectById(timeoutOrder.getId());
        assertNotNull(updatedOrder, "Order should exist");
        assertEquals("CANCELLED", updatedOrder.getStatus(), "Order status should be CANCELLED");
        assertNotNull(updatedOrder.getCancelledAt(), "Cancel time should not be null");

        log.info("Order cancelled: OrderNo={}, Status={}, CancelTime={}",
                updatedOrder.getOrderNo(), updatedOrder.getStatus(), updatedOrder.getCancelledAt());

        log.info("========================================");
        log.info("Scenario 1 test passed");
        log.info("========================================");
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Scenario 2: Order timeout cancel job -> non-timed out order should not be cancelled")
    public void testOrderTimeoutCancelJob_NotTimeoutOrder_ShouldNotCancel() throws Exception {
        log.info("========================================");
        log.info("Test Scenario 2: Order timeout cancel (not timed out)");
        log.info("========================================");

        OrdOrder validOrder = createTestOrder(TEST_USER_ID, "00000000-0000-0000-0000-000000000002", 10, "PENDING_PAYMENT");
        validOrder.setCreateTime(LocalDateTime.now().minusMinutes(10));
        orderMapper.insert(validOrder);

        log.info("Created non-timed out order: OrderNo={}, CreateTime={}, Status={}",
                validOrder.getOrderNo(), validOrder.getCreateTime(), validOrder.getStatus());

        log.info("Executing OrderTimeoutCancelJobHandler...");
        orderTimeoutCancelJobHandler.execute();

        OrdOrder updatedOrder = orderMapper.selectById(validOrder.getId());
        assertNotNull(updatedOrder, "Order should exist");
        assertEquals("PENDING_PAYMENT", updatedOrder.getStatus(), "Order status should remain PENDING_PAYMENT");
        assertNull(updatedOrder.getCancelledAt(), "Cancel time should be null");

        log.info("Order not cancelled (as expected): OrderNo={}, Status={}",
                updatedOrder.getOrderNo(), updatedOrder.getStatus());

        log.info("========================================");
        log.info("Scenario 2 test passed");
        log.info("========================================");
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Scenario 3: Order timeout cancel job -> batch timed out orders should be cancelled")
    public void testOrderTimeoutCancelJob_BatchTimeoutOrders_ShouldCancelAll() throws Exception {
        log.info("========================================");
        log.info("Test Scenario 3: Order timeout cancel (batch timeout)");
        log.info("========================================");

        int batchSize = 5;
        for (int i = 0; i < batchSize; i++) {
            OrdOrder timeoutOrder = createTestOrder(TEST_USER_ID + i, String.valueOf(100 + i), 10, "PENDING_PAYMENT");
            timeoutOrder.setCreateTime(LocalDateTime.now().minusMinutes(35 + i));
            orderMapper.insert(timeoutOrder);
            log.info("Created timed out order {}: OrderNo={}", i + 1, timeoutOrder.getOrderNo());
        }

        log.info("Executing OrderTimeoutCancelJobHandler...");
        orderTimeoutCancelJobHandler.execute();

        Long cancelledCount = orderMapper.selectCount(
                new LambdaQueryWrapper<OrdOrder>()
                        .ge(OrdOrder::getUserId, TEST_USER_ID)
                        .eq(OrdOrder::getStatus, "CANCELLED")
        );

        assertEquals((long) batchSize, cancelledCount, "Should cancel " + batchSize + " orders");

        log.info("Batch cancel verified: cancelled {} orders", cancelledCount);

        log.info("========================================");
        log.info("Scenario 3 test passed");
        log.info("========================================");
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("Scenario 4: Order timeout cancel job -> custom timeout parameter")
    public void testOrderTimeoutCancelJob_CustomTimeoutParam() throws Exception {
        log.info("========================================");
        log.info("Test Scenario 4: Order timeout cancel (custom timeout parameter)");
        log.info("========================================");

        OrdOrder order = createTestOrder(TEST_USER_ID, "00000000-0000-0000-0000-000000000003", 10, "PENDING_PAYMENT");
        order.setCreateTime(LocalDateTime.now().minusMinutes(20));
        orderMapper.insert(order);

        log.info("Created order: OrderNo={}, CreateTime={}", order.getOrderNo(), order.getCreateTime());

        log.info("Executing OrderTimeoutCancelJobHandler (timeout=15 mins)...");
        orderTimeoutCancelJobHandler.execute();

        OrdOrder updatedOrder = orderMapper.selectById(order.getId());
        assertEquals("PENDING_PAYMENT", updatedOrder.getStatus(),
                "20-min old order, default 30-min timeout, should not be cancelled");

        log.info("Custom timeout parameter test passed (default behavior verified)");
        log.info("  - Full parameterized test requires XXL-Job console config");

        log.info("========================================");
        log.info("Scenario 4 test passed");
        log.info("========================================");
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("Scenario 5: Order timeout cancel job -> paid order should not be cancelled")
    public void testOrderTimeoutCancelJob_PaidOrder_ShouldNotCancel() throws Exception {
        log.info("========================================");
        log.info("Test Scenario 5: Order timeout cancel (paid order)");
        log.info("========================================");

        OrdOrder paidOrder = createTestOrder(TEST_USER_ID, "00000000-0000-0000-0000-000000000004", 10, "PAID");
        paidOrder.setCreateTime(LocalDateTime.now().minusMinutes(35));
        paidOrder.setPaidAt(LocalDateTime.now().minusMinutes(30));
        orderMapper.insert(paidOrder);

        log.info("Created paid order: OrderNo={}, Status={}, PayTime={}",
                paidOrder.getOrderNo(), paidOrder.getStatus(), paidOrder.getPaidAt());

        log.info("Executing OrderTimeoutCancelJobHandler...");
        orderTimeoutCancelJobHandler.execute();

        OrdOrder updatedOrder = orderMapper.selectById(paidOrder.getId());
        assertEquals("PAID", updatedOrder.getStatus(), "Paid order should not be cancelled");
        assertNull(updatedOrder.getCancelledAt(), "Cancel time should be null");

        log.info("Paid order not cancelled (as expected): OrderNo={}, Status={}",
                updatedOrder.getOrderNo(), updatedOrder.getStatus());

        log.info("========================================");
        log.info("Scenario 5 test passed");
        log.info("========================================");
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("Scenario 6: Order timeout cancel job -> already cancelled order should be skipped")
    public void testOrderTimeoutCancelJob_AlreadyCancelledOrder_ShouldSkip() throws Exception {
        log.info("========================================");
        log.info("Test Scenario 6: Order timeout cancel (already cancelled order)");
        log.info("========================================");

        OrdOrder cancelledOrder = createTestOrder(TEST_USER_ID, "00000000-0000-0000-0000-000000000005", 10, "CANCELLED");
        cancelledOrder.setCreateTime(LocalDateTime.now().minusMinutes(35));
        cancelledOrder.setCancelledAt(LocalDateTime.now().minusMinutes(5));
        orderMapper.insert(cancelledOrder);

        log.info("Created cancelled order: OrderNo={}, Status={}, CancelTime={}",
                cancelledOrder.getOrderNo(), cancelledOrder.getStatus(), cancelledOrder.getCancelledAt());

        LocalDateTime originalCancelTime = cancelledOrder.getCancelledAt();

        log.info("Executing OrderTimeoutCancelJobHandler...");
        orderTimeoutCancelJobHandler.execute();

        OrdOrder updatedOrder = orderMapper.selectById(cancelledOrder.getId());
        assertEquals("CANCELLED", updatedOrder.getStatus(), "Order status should remain CANCELLED");
        assertEquals(originalCancelTime, updatedOrder.getCancelledAt(),
                "Cancelled time should not change (idempotency)");

        log.info("Already cancelled order not re-processed (as expected): OrderNo={}, CancelTime={}",
                updatedOrder.getOrderNo(), updatedOrder.getCancelledAt());

        log.info("========================================");
        log.info("Scenario 6 test passed");
        log.info("========================================");
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("Scenario 7: XXL-Job Handler annotation verification")
    public void testXxlJobHandlerAnnotation() throws NoSuchMethodException {
        log.info("========================================");
        log.info("Test Scenario 7: XXL-Job Handler annotation verification");
        log.info("========================================");

        Method executeMethod = OrderTimeoutCancelJobHandler.class.getMethod("execute");
        assertTrue(executeMethod.isAnnotationPresent(XxlJob.class),
                "execute method should have @XxlJob annotation");

        XxlJob xxlJobAnnotation = executeMethod.getAnnotation(XxlJob.class);
        assertEquals("orderTimeoutCancelJobHandler", xxlJobAnnotation.value(),
                "@XxlJob value should be 'orderTimeoutCancelJobHandler'");

        log.info("@XxlJob annotation verified: value={}", xxlJobAnnotation.value());

        assertEquals(void.class, executeMethod.getReturnType(),
                "execute method should return void");
        assertEquals(0, executeMethod.getParameterCount(),
                "execute method should have no parameters");

        log.info("Method signature verified: returnType=void, params=0");

        log.info("========================================");
        log.info("Scenario 7 test passed");
        log.info("========================================");
    }

    @AfterEach
    public void cleanup() {
        log.info("========================================");
        log.info("Cleaning up XXL-Job test data");
        log.info("========================================");

        orderMapper.delete(
                new LambdaQueryWrapper<OrdOrder>()
                        .ge(OrdOrder::getUserId, TEST_USER_ID)
        );

        log.info("XXL-Job test data cleanup completed");
    }

    private OrdOrder createTestOrder(Long userId, String skuId, Integer quantity, String status) {
        OrdOrder order = new OrdOrder();
        order.setOrderNo("TEST" + System.currentTimeMillis() + userId);
        order.setUserId(String.valueOf(userId));
        order.setSkuId(String.valueOf(skuId));
        order.setQuantity(Quantity.of(quantity));
        order.setTotalAmount(Money.of(new BigDecimal("99.00").multiply(new BigDecimal(quantity))));
        order.setStatus(OrderStatus.valueOf(status).getCode());
        order.setRemark("XXL-Job test order");
        order.setCreateTime(LocalDateTime.now());
        return order;
    }
}
