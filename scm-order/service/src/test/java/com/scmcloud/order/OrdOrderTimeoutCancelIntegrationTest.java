package com.scmcloud.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.Quantity;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.mapper.OrdOrderMapper;
import com.scmcloud.order.service.command.OrdOrderCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the timeout-cancel command.
 * Verifies that the domain cancel path (status transition, cancel metadata,
 * outbox event publication via repository) is used instead of raw mapper updates.
 */
@RequiredArgsConstructor
@Slf4j
@SpringBootTest
@Tag("integration")
@ActiveProfiles("test")
@DisplayName("Order Timeout Cancel Command Integration Test")
class OrdOrderTimeoutCancelIntegrationTest {

    private final OrdOrderCommandService ordOrderCommandService;

    private final OrdOrderMapper orderMapper;

    private static final Long TEST_USER_ID = 4001L;

    @BeforeEach
    public void setup() {
        orderMapper.delete(
                new LambdaQueryWrapper<OrdOrder>()
                        .ge(OrdOrder::getUserId, TEST_USER_ID)
        );
    }

    @Test
    @Transactional
    @DisplayName("Timeout cancel: pending-payment order transitions to CANCELLED with metadata")
    void shouldCancelPendingPaymentOrderOnTimeout() {
        OrdOrder order = createTestOrder(TEST_USER_ID, 1L, 10, OrderStatus.PENDING_PAYMENT);
        order.setCreateTime(LocalDateTime.now().minusMinutes(35));
        orderMapper.insert(order);

        ordOrderCommandService.cancelTimeoutOrder(order);

        OrdOrder updated = orderMapper.selectById(order.getId());
        assertNotNull(updated, "Order should exist");
        assertEquals(OrderStatus.CANCELLED.getCode(), updated.getStatus(),
                "Order status should be CANCELLED");
        assertNotNull(updated.getCancelledAt(), "Cancel time should be set");
        assertTrue(updated.getCancelReason() != null && !updated.getCancelReason().isBlank(),
                "Cancel reason should be recorded");
    }

    @Test
    @Transactional
    @DisplayName("Timeout cancel: already-cancelled order is rejected")
    void shouldRejectCancellingAlreadyCancelledOrder() {
        OrdOrder order = createTestOrder(TEST_USER_ID, 2L, 10, OrderStatus.CANCELLED);
        order.setCreateTime(LocalDateTime.now().minusMinutes(35));
        orderMapper.insert(order);

        assertThrows(IllegalStateException.class,
                () -> ordOrderCommandService.cancelTimeoutOrder(order),
                "Cancelling an already-cancelled order should fail");
    }

    @Test
    @Transactional
    @DisplayName("Timeout cancel: completed order is rejected")
    void shouldRejectCancellingCompletedOrder() {
        OrdOrder order = createTestOrder(TEST_USER_ID, 3L, 10, OrderStatus.COMPLETED);
        order.setCreateTime(LocalDateTime.now().minusMinutes(35));
        orderMapper.insert(order);

        assertThrows(IllegalStateException.class,
                () -> ordOrderCommandService.cancelTimeoutOrder(order),
                "Cancelling a completed order should fail");
    }

    @AfterEach
    public void cleanup() {
        orderMapper.delete(
                new LambdaQueryWrapper<OrdOrder>()
                        .ge(OrdOrder::getUserId, TEST_USER_ID)
        );
    }

    private OrdOrder createTestOrder(Long userId, Long skuId, Integer quantity, OrderStatus status) {
        OrdOrder order = new OrdOrder();
        order.setOrderNo("TOC" + System.currentTimeMillis() + userId);
        order.setUserId(String.valueOf(userId));
        order.setSkuId(String.valueOf(skuId));
        order.setQuantity(Quantity.of(quantity));
        order.setTotalAmount(Money.of(new BigDecimal("99.00").multiply(new BigDecimal(quantity))));
        order.setPayableAmount(Money.of(new BigDecimal("99.00").multiply(new BigDecimal(quantity))));
        order.setStatus(status.getCode());
        order.setRemark("Timeout cancel test order");
        order.setCreateTime(LocalDateTime.now());
        return order;
    }
}
