package com.scmcloud.ordercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.scmcloud.ordercenter.domain.entity.OcOrder;
import com.scmcloud.ordercenter.domain.entity.OcOrderItem;
import com.scmcloud.ordercenter.mapper.OcOrderItemMapper;
import com.scmcloud.ordercenter.mapper.OcOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCenterServiceImplTest {

    @Mock private OcOrderMapper orderMapper;
    @Mock private OcOrderItemMapper orderItemMapper;

    private OrderCenterServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderCenterServiceImpl(orderItemMapper);
        ReflectionTestUtils.setField(orderService, "baseMapper", orderMapper);
    }

    @Test
    void createOrderComputesPayableAmountFromItems() {
        OcOrder order = new OcOrder().setOrderNo("ORD-1").setUserId("user-1");
        OcOrderItem item = new OcOrderItem().setQuantity(2).setPrice(new BigDecimal("5.00"));
        when(orderMapper.insert(any(OcOrder.class))).thenReturn(1);

        OcOrder created = orderService.createOrder(order, List.of(item));

        assertEquals(new BigDecimal("10.00"), created.getTotalAmount());
        assertEquals(new BigDecimal("10.00"), created.getPayableAmount());
        assertEquals(1, created.getStatus());
    }

    @Test
    void createOrderRejectsEmptyItems() {
        OcOrder order = new OcOrder().setOrderNo("ORD-1").setUserId("user-1");

        assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(order, List.of()));
    }

    @Test
    void createOrderRejectsNegativeItemAmount() {
        OcOrder order = new OcOrder().setOrderNo("ORD-1").setUserId("user-1");
        OcOrderItem item = new OcOrderItem().setQuantity(1).setPayableAmount(new BigDecimal("-1.00"));

        assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(order, List.of(item)));
    }

    @Test
    void payOrderRejectsMissingPayableAmount() {
        OcOrder order = new OcOrder().setOrderNo("ORD-1").setStatus(1);
        when(orderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(order);

        assertThrows(IllegalStateException.class, () -> orderService.payOrder("ORD-1", "PAY-1"));
    }

    @Test
    void cancelOrderRejectsCompletedOrder() {
        OcOrder order = new OcOrder().setOrderNo("ORD-1").setStatus(7);
        when(orderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(order);

        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder("ORD-1", "x"));
    }

    @Test
    void pageQueryRejectsInvalidPage() {
        assertThrows(IllegalArgumentException.class,
                () -> orderService.pageQuery(0, 20, null, null, null));
    }

    @Test
    void pageQueryRejectsOversizedPageSize() {
        assertThrows(IllegalArgumentException.class,
                () -> orderService.pageQuery(1, 101, null, null, null));
    }
}
