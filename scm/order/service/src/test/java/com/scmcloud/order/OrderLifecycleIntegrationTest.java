package com.scmcloud.order;

import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.domain.repository.OrdOrderRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.scmcloud.common.domain.Money;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;

/**
 * Integration test for full order lifecycle.
 * Tests the order aggregate with real database.
 */
@SpringBootTest
@Tag("integration")
@ActiveProfiles("test")
class OrderLifecycleIntegrationTest {

    @Autowired
    private OrdOrderRepository orderRepository;

    @Test
    @Transactional
    void shouldCreateOrderAndTransitionStatuses() {
        // Create order
        OrdOrder order = new OrdOrder();
        order.setOrderNo("ORD-TEST-001");
        order.setUserId("user-001");
        order.setTotalAmount(Money.of("100.00"));
        order.setPayableAmount(Money.of("100.00"));
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());

        // Save order
        orderRepository.save(order);
        assertNotNull(order.getId());

        // Pay order
        order.pay(Money.of("100.00"), "PAY-001");
        assertEquals(OrderStatus.PAID.getCode(), order.getStatus());
        assertEquals("PAY-001", order.getPaymentNo());

        // Ship order
        order.ship("WB-001", "SF Express");
        assertEquals(OrderStatus.SHIPPED.getCode(), order.getStatus());
        assertEquals("WB-001", order.getWaybillNo());

        // Complete order
        order.complete();
        assertEquals(OrderStatus.COMPLETED.getCode(), order.getStatus());
        assertTrue(order.isTerminal());
    }

    @Test
    @Transactional
    void shouldCancelOrderFromPendingPayment() {
        OrdOrder order = new OrdOrder();
        order.setOrderNo("ORD-TEST-002");
        order.setUserId("user-002");
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());

        order.cancel("User requested");
        assertEquals(OrderStatus.CANCELLED.getCode(), order.getStatus());
        assertEquals("User requested", order.getCancelReason());
        assertTrue(order.isTerminal());
    }

    @Test
    @Transactional
    void shouldNotCancelCompletedOrder() {
        OrdOrder order = new OrdOrder();
        order.setOrderNo("ORD-TEST-003");
        order.setUserId("user-003");
        order.setStatus(OrderStatus.COMPLETED.getCode());

        assertThrows(IllegalStateException.class, () -> order.cancel("Invalid"));
    }

    @Test
    @Transactional
    void shouldNotPayCancelledOrder() {
        OrdOrder order = new OrdOrder();
        order.setOrderNo("ORD-TEST-004");
        order.setUserId("user-004");
        order.setStatus(OrderStatus.CANCELLED.getCode());

        assertThrows(IllegalStateException.class, () -> 
            order.pay(Money.of("100.00"), "PAY-004"));
    }
}
