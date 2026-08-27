package com.scmcloud.order.job;

import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.Quantity;
import com.scmcloud.inventory.api.InventoryDubboService;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.mapper.OrdOrderMapper;
import com.scmcloud.order.service.command.OrdOrderCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import java.util.UUID;

/**
 * Unit test for the timeout-cancel job handler.
 * Verifies scanning, delegation to the command service, and best-effort
 * inventory release — without a running XXL-Job console or database.
 */
@ExtendWith(MockitoExtension.class)
class OrderTimeoutCancelJobHandlerTest {

    @Mock
    private OrdOrderMapper orderMapper;

    @Mock
    private OrdOrderCommandService ordOrderCommandService;

    @Mock
    private InventoryDubboService inventoryService;

    private OrderTimeoutCancelJobHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderTimeoutCancelJobHandler(orderMapper, ordOrderCommandService);
        ReflectionTestUtils.setField(handler, "inventoryService", inventoryService);
    }

    @Test
    void shouldDelegateCancellationToCommandService() throws Exception {
        OrdOrder order = createTestOrder(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), "1001", 5);
        when(orderMapper.selectList(any())).thenReturn(List.of(order));

        handler.execute();

        verify(ordOrderCommandService).cancelTimeoutOrder(order);
        verify(inventoryService).releaseStock(eq(1001L), eq(5), eq("TIMEOUT_CANCEL:" + order.getOrderNo()));
    }

    @Test
    void shouldSkipInventoryReleaseWhenCancelFails() throws Exception {
        OrdOrder order = createTestOrder(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"), "1002", 3);
        when(orderMapper.selectList(any())).thenReturn(List.of(order));
        doThrow(new IllegalStateException("Cannot cancel order")).when(ordOrderCommandService)
                .cancelTimeoutOrder(any());

        handler.execute();

        verify(ordOrderCommandService).cancelTimeoutOrder(order);
        verify(inventoryService, never()).releaseStock(anyLong(), anyInt(), any());
    }

    @Test
    void shouldContinueProcessingRemainingOrdersWhenOneFails() throws Exception {
        OrdOrder first = createTestOrder(java.util.UUID.fromString("00000000-0000-0000-0000-000000000003"), "1003", 1);
        OrdOrder second = createTestOrder(java.util.UUID.fromString("00000000-0000-0000-0000-000000000004"), "1004", 2);
        when(orderMapper.selectList(any())).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("first fails")).when(ordOrderCommandService).cancelTimeoutOrder(first);

        handler.execute();

        verify(ordOrderCommandService).cancelTimeoutOrder(first);
        verify(ordOrderCommandService).cancelTimeoutOrder(second);
        verify(inventoryService, never()).releaseStock(eq(1003L), anyInt(), any());
        verify(inventoryService).releaseStock(eq(1004L), anyInt(), any());
    }

    @Test
    void shouldDoNothingWhenNoTimeoutOrders() throws Exception {
        when(orderMapper.selectList(any())).thenReturn(List.of());

        handler.execute();

        verify(ordOrderCommandService, never()).cancelTimeoutOrder(any());
        verify(inventoryService, never()).releaseStock(anyLong(), anyInt(), any());
    }

    @Test
    void shouldCountMalformedSkuAsFailure() throws Exception {
        OrdOrder order = createTestOrder(java.util.UUID.fromString("00000000-0000-0000-0000-000000000005"), "not-a-number", 2);
        when(orderMapper.selectList(any())).thenReturn(List.of(order));

        handler.execute();

        verify(ordOrderCommandService).cancelTimeoutOrder(order);
        verify(inventoryService, never()).releaseStock(anyLong(), anyInt(), any());
    }

    private OrdOrder createTestOrder(UUID id, String skuId, Integer quantity) {
        OrdOrder order = new OrdOrder();
        order.setId(id);
        order.setOrderNo("UT" + System.nanoTime() + id);
        order.setUserId("u-" + id);
        order.setSkuId(skuId);
        order.setQuantity(Quantity.of(quantity));
        order.setTotalAmount(Money.of(new BigDecimal("99.00").multiply(new BigDecimal(quantity))));
        order.setPayableAmount(Money.of(new BigDecimal("99.00").multiply(new BigDecimal(quantity))));
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setCreateTime(LocalDateTime.now().minusMinutes(35));
        return order;
    }
}
