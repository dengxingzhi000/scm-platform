package com.scmcloud.order.service.dubbo;

import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.TenantId;
import com.scmcloud.order.api.dto.OrderVO;
import com.scmcloud.order.api.request.CreateOrderRequest;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.event.OrderCreatedEvent;
import com.scmcloud.order.event.OrderEvent;
import com.scmcloud.order.event.OrderEventStore;
import com.scmcloud.order.event.OrderStatusChangedEvent;
import com.scmcloud.order.service.IOrdOrderService;
import com.scmcloud.system.api.StatusMachineDubboService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDubboServiceImplEventTest {

    @Mock private IOrdOrderService orderService;
    @Mock private OrderEventStore eventStore;
    @Mock private StatusMachineDubboService statusMachine;

    private OrderDubboServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderDubboServiceImpl(orderService, eventStore);
        ReflectionTestUtils.setField(service, "statusMachine", statusMachine);
    }

    private OrdOrder order(int statusCode) {
        OrdOrder order = new OrdOrder();
        order.setId(1L);
        order.setOrderNo("NO1001");
        order.setStatus(statusCode);
        order.setUserId("00000000-0000-0000-0000-000000000001");
        order.setTenantId(TenantId.generate());
        order.setTotalAmount(Money.of(new BigDecimal("99.90")));
        order.setPayableAmount(Money.of(new BigDecimal("99.90")));
        return order;
    }

    @Test
    void createOrderShouldAppendOrderCreatedEvent() {
        when(orderService.save(any(OrdOrder.class))).thenAnswer(inv -> {
            inv.getArgument(0, OrdOrder.class).setId(1L);
            return true;
        });
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(12345L);
        request.setSkuId(1L);
        request.setQuantity(1);
        request.setTotalAmount(new BigDecimal("99.90"));

        OrderVO vo = service.createOrder(request);

        assertEquals(1L, vo.getId());
        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventStore).append(captor.capture());
        OrderCreatedEvent event = assertInstanceOf(OrderCreatedEvent.class, captor.getValue());
        assertEquals(1L, event.getOrderId());
        assertEquals("12345", event.getUserId());
        assertEquals(new BigDecimal("99.90"), event.getTotalAmount());
    }

    @Test
    void cancelOrderShouldAppendCancelledEvent() {
        OrdOrder existing = order(OrderStatus.PAID.getCode());
        when(orderService.getOne(any())).thenReturn(existing);
        when(statusMachine.transition("ORDER", "PAID", "CANCEL"))
                .thenReturn(new StatusMachineDubboService.TransitionResultDTO(
                        true, "ORDER", "PAID", "CANCELLED", "CANCEL",
                        false, null, null));
        when(orderService.updateById(any(OrdOrder.class))).thenReturn(true);

        service.cancelOrder("NO1001");

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventStore).append(captor.capture());
        OrderStatusChangedEvent event =
                assertInstanceOf(OrderStatusChangedEvent.class, captor.getValue());
        assertEquals(OrderStatus.PAID, event.getFromStatus());
        assertEquals(OrderStatus.CANCELLED, event.getToStatus());
    }
}
