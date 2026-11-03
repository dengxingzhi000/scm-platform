package com.cognora.scm.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.scm.order.dto.RefundApproveRequest;
import com.cognora.scm.order.dto.RefundCreateRequest;
import com.cognora.scm.order.dto.RefundItemResponse;
import com.cognora.scm.order.dto.RefundQueryRequest;
import com.cognora.scm.order.dto.RefundRejectRequest;
import com.cognora.scm.order.dto.RefundResponse;
import com.cognora.scm.order.service.command.OrdRefundCommandService;
import com.cognora.scm.order.service.query.OrdRefundItemQueryService;
import com.cognora.scm.order.service.query.OrdRefundQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdRefundControllerTest {

    @Mock
    private OrdRefundQueryService queryService;

    @Mock
    private OrdRefundItemQueryService refundItemQueryService;

    @Mock
    private OrdRefundCommandService commandService;

    @InjectMocks
    private OrdRefundController controller;

    @Test
    void getByIdDelegatesItemAssemblyToQueryService() {
        UUID id = UUID.randomUUID();
        RefundResponse refund = new RefundResponse();
        when(queryService.getResponseById(id)).thenReturn(refund);

        ApiResponse<RefundResponse> response = controller.getById(id);

        assertEquals(200, response.code());
        assertSame(refund, response.data());
    }

    @Test
    void listItemsDelegatesResponseMappingToItemQueryService() {
        UUID id = UUID.randomUUID();
        List<RefundItemResponse> items = List.of(new RefundItemResponse());
        when(refundItemQueryService.listResponsesByRefundId(id)).thenReturn(items);

        ApiResponse<List<RefundItemResponse>> response = controller.listItems(id);

        assertEquals(200, response.code());
        assertSame(items, response.data());
    }

    @Test
    void listByOrderIdDelegatesResponseMappingToQueryService() {
        UUID orderId = UUID.randomUUID();
        List<RefundResponse> refunds = List.of(new RefundResponse());
        when(queryService.listResponsesByOrderId(orderId)).thenReturn(refunds);

        ApiResponse<List<RefundResponse>> response = controller.listByOrderId(orderId);

        assertEquals(200, response.code());
        assertSame(refunds, response.data());
    }

    @Test
    void queryDelegatesPageMappingToQueryService() {
        RefundQueryRequest request = new RefundQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        Page<RefundResponse> page = new Page<>(1, 10, 2);
        when(queryService.pageResponses(request)).thenReturn(page);

        ApiResponse<Page<RefundResponse>> response = controller.query(request);

        assertEquals(200, response.code());
        assertEquals(2L, response.data().getTotal());
    }

    @Test
    void createRefundDelegatesRequestMappingToCommandService() {
        RefundCreateRequest request = new RefundCreateRequest();
        RefundResponse created = new RefundResponse();
        when(commandService.createRefund(request)).thenReturn(created);

        ApiResponse<RefundResponse> response = controller.createRefund(request);

        assertEquals(200, response.code());
        assertSame(created, response.data());
    }

    @Test
    void approveReturnsLatestRefundWithItems() {
        UUID id = UUID.randomUUID();
        RefundApproveRequest request = new RefundApproveRequest();
        request.setHandlerId("handler");
        request.setHandlerName("name");
        request.setHandlerRemark("ok");
        RefundResponse refund = new RefundResponse();
        when(queryService.getResponseById(id)).thenReturn(refund);

        ApiResponse<RefundResponse> response = controller.approve(id, request);

        assertEquals(200, response.code());
        assertSame(refund, response.data());
        verify(commandService).approveRefund(id, "handler", "name", "ok");
    }

    @Test
    void rejectReturnsLatestRefundWithItems() {
        UUID id = UUID.randomUUID();
        RefundRejectRequest request = new RefundRejectRequest();
        request.setHandlerId("handler");
        request.setHandlerName("name");
        request.setHandlerRemark("bad");
        RefundResponse refund = new RefundResponse();
        when(queryService.getResponseById(id)).thenReturn(refund);

        ApiResponse<RefundResponse> response = controller.reject(id, request);

        assertEquals(200, response.code());
        assertSame(refund, response.data());
        verify(commandService).rejectRefund(id, "handler", "name", "bad");
    }

    @Test
    void completeReturnsLatestRefundWithItems() {
        UUID id = UUID.randomUUID();
        RefundResponse refund = new RefundResponse();
        when(queryService.getResponseById(id)).thenReturn(refund);

        ApiResponse<RefundResponse> response = controller.complete(id);

        assertEquals(200, response.code());
        assertSame(refund, response.data());
        verify(commandService).completeRefund(id);
    }
}
