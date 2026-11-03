package com.cognora.scm.order.service.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.domain.Money;
import com.cognora.scm.order.domain.entity.OrderStatus;
import com.cognora.scm.order.domain.entity.OrdOrder;
import com.cognora.scm.order.dto.OrderQueryRequest;
import com.cognora.scm.order.dto.OrderResponse;
import com.cognora.scm.order.mapper.OrdOrderItemMapper;
import com.cognora.scm.order.mapper.OrdOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdOrderQueryServiceTest {

    @Mock
    private OrdOrderMapper ordOrderMapper;

    @Mock
    private OrdOrderItemMapper ordOrderItemMapper;

    @InjectMocks
    private OrdOrderQueryService service;

    @Test
    void queryPageMapsRecordsAndPagination() {
        OrdOrder order = new OrdOrder();
        order.setId(UUID.randomUUID());
        order.setOrderNo("ORD-1");
        order.setStatus(OrderStatus.PAID.getCode());
        order.setTotalAmount(Money.of(new BigDecimal("100.00")));
        Page<OrdOrder> page = new Page<>(1, 10, 4);
        page.setRecords(List.of(order));
        when(ordOrderMapper.selectPage(any(), any())).thenReturn(page);

        OrderQueryRequest request = new OrderQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);

        Page<OrderResponse> result = service.queryPage(request);

        assertEquals(4L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("ORD-1", result.getRecords().get(0).getOrderNo());
        assertEquals(OrderStatus.PAID, result.getRecords().get(0).getStatus());
    }

    @Test
    void getResponseByIdReturnsNullWhenMissing() {
        UUID id = UUID.randomUUID();
        when(ordOrderMapper.selectById(id.toString())).thenReturn(null);

        assertNull(service.getResponseById(id.toString()));
    }
}
