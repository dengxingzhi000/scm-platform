package com.cognora.scm.order.service.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.domain.Money;
import com.cognora.scm.order.domain.entity.OrdOrderItem;
import com.cognora.scm.order.dto.OrderItemResponse;
import com.cognora.scm.order.dto.OrdOrderItemQueryRequest;
import com.cognora.scm.order.mapper.OrdOrderItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdOrderItemQueryServiceTest {

    @Mock
    private OrdOrderItemMapper ordOrderItemMapper;

    @InjectMocks
    private OrdOrderItemQueryService service;

    @Test
    void pageResponsesMapsRecordsAndPagination() {
        OrdOrderItem item = new OrdOrderItem();
        item.setSkuName("SKU-1");
        item.setSkuCode("SKU-CODE-1");
        item.setSubtotal(Money.of(new BigDecimal("30.00")));
        Page<OrdOrderItem> page = new Page<>(1, 10, 3);
        page.setRecords(List.of(item));
        when(ordOrderItemMapper.selectPage(any(), any())).thenReturn(page);

        OrdOrderItemQueryRequest request = new OrdOrderItemQueryRequest();
        request.setOrderId(UUID.randomUUID());
        request.setPageNum(1);
        request.setPageSize(10);

        Page<OrderItemResponse> result = service.pageResponses(request);

        assertEquals(3L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("SKU-1", result.getRecords().get(0).getSkuName());
        assertEquals(new BigDecimal("30.00"), result.getRecords().get(0).getSubtotal());
    }
}
