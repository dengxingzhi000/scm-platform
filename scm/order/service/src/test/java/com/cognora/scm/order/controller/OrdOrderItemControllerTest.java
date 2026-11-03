package com.cognora.scm.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.scm.order.dto.OrderItemResponse;
import com.cognora.scm.order.dto.OrdOrderItemQueryRequest;
import com.cognora.scm.order.service.query.OrdOrderItemQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdOrderItemControllerTest {

    @Mock
    private OrdOrderItemQueryService queryService;

    @InjectMocks
    private OrdOrderItemController controller;

    @Test
    void getByIdDelegatesResponseMappingToQueryService() {
        UUID id = UUID.randomUUID();
        OrderItemResponse item = new OrderItemResponse();
        when(queryService.getResponseById(id.toString())).thenReturn(item);

        ApiResponse<OrderItemResponse> response = controller.getById(id);

        assertEquals(200, response.code());
        assertSame(item, response.data());
    }

    @Test
    void queryByOrderIdDelegatesPageMappingToQueryService() {
        OrdOrderItemQueryRequest request = new OrdOrderItemQueryRequest();
        request.setOrderId(UUID.randomUUID());
        request.setPageNum(1);
        request.setPageSize(10);
        Page<OrderItemResponse> page = new Page<>(1, 10, 2);
        when(queryService.pageResponses(request)).thenReturn(page);

        ApiResponse<Page<OrderItemResponse>> response = controller.queryByOrderId(request);

        assertEquals(200, response.code());
        assertEquals(2L, response.data().getTotal());
    }
}