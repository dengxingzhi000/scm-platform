package com.scmcloud.order.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.order.dto.StatusHistoryResponse;
import com.scmcloud.order.service.query.OrdStatusHistoryQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdStatusHistoryControllerTest {

    @Mock
    private OrdStatusHistoryQueryService queryService;

    @InjectMocks
    private OrdStatusHistoryController controller;

    @Test
    void getByIdDelegatesResponseMappingToQueryService() {
        UUID id = UUID.randomUUID();
        StatusHistoryResponse history = new StatusHistoryResponse();
        when(queryService.getResponseById(id.toString())).thenReturn(history);

        ApiResponse<StatusHistoryResponse> response = controller.getById(id);

        assertEquals(200, response.code());
        assertSame(history, response.data());
    }

    @Test
    void listByOrderIdDelegatesResponseMappingToQueryService() {
        UUID orderId = UUID.randomUUID();
        List<StatusHistoryResponse> history = List.of(new StatusHistoryResponse());
        when(queryService.listResponsesByOrderId(orderId)).thenReturn(history);

        ApiResponse<List<StatusHistoryResponse>> response = controller.listByOrderId(orderId);

        assertEquals(200, response.code());
        assertSame(history, response.data());
    }
}
