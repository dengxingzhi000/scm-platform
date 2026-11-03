package com.cognora.scm.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.scm.order.domain.entity.OrderStatus;
import com.cognora.scm.order.dto.OrderCancelRequest;
import com.cognora.scm.order.dto.OrderCreateRequest;
import com.cognora.scm.order.dto.OrderDashboardCompare;
import com.cognora.scm.order.dto.OrderDashboardRange;
import com.cognora.scm.order.dto.OrderDashboardSummaryResponse;
import com.cognora.scm.order.dto.OrderItemResponse;
import com.cognora.scm.order.dto.OrderQueryRequest;
import com.cognora.scm.order.dto.OrderResponse;
import com.cognora.scm.order.dto.OrderStatusUpdateRequest;
import com.cognora.scm.order.service.command.OrdOrderCommandService;
import com.cognora.scm.order.service.query.OrdOrderDashboardQueryService;
import com.cognora.scm.order.service.query.OrdOrderQueryService;
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
class OrdOrderControllerTest {

    @Mock
    private OrdOrderQueryService queryService;

    @Mock
    private OrdOrderCommandService commandService;

    @Mock
    private OrdOrderDashboardQueryService dashboardQueryService;

    @InjectMocks
    private OrdOrderController controller;

    @Test
    void getByIdDelegatesResponseMappingToQueryService() {
        UUID id = UUID.randomUUID();
        OrderResponse order = new OrderResponse();
        when(queryService.getResponseById(id.toString())).thenReturn(order);

        ApiResponse<OrderResponse> response = controller.getById(id);

        assertEquals(200, response.code());
        assertSame(order, response.data());
    }

    @Test
    void queryDelegatesPageMappingToQueryService() {
        OrderQueryRequest request = new OrderQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        Page<OrderResponse> page = new Page<>(1, 10, 3);
        when(queryService.queryPage(request)).thenReturn(page);

        ApiResponse<Page<OrderResponse>> response = controller.query(request);

        assertEquals(200, response.code());
        assertEquals(3L, response.data().getTotal());
        verify(queryService).queryPage(request);
    }

    @Test
    void getItemsDelegatesResponseMappingToQueryService() {
        UUID id = UUID.randomUUID();
        List<OrderItemResponse> items = List.of(new OrderItemResponse());
        when(queryService.listItemResponses(id)).thenReturn(items);

        ApiResponse<List<OrderItemResponse>> response = controller.getItems(id);

        assertEquals(200, response.code());
        assertSame(items, response.data());
    }

    @Test
    void createOrderDelegatesRequestAndResponseMappingToCommandService() {
        OrderCreateRequest request = new OrderCreateRequest();
        OrderResponse created = new OrderResponse();
        when(commandService.createOrder(request)).thenReturn(created);

        ApiResponse<OrderResponse> response = controller.createOrder(request);

        assertEquals(200, response.code());
        assertSame(created, response.data());
    }

    @Test
    void updateStatusReturnsNotFoundWhenCommandReturnsNull() {
        UUID id = UUID.randomUUID();
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
        request.setTargetStatus(OrderStatus.PAID);
        when(commandService.updateOrderStatus(id, OrderStatus.PAID)).thenReturn(null);

        ApiResponse<OrderResponse> response = controller.updateStatus(id, request);

        assertEquals(404, response.code());
        assertEquals("订单不存在或状态未变更: id=" + id, response.message());
    }

    @Test
    void updateStatusReturnsMappedResponseFromCommandService() {
        UUID id = UUID.randomUUID();
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
        request.setTargetStatus(OrderStatus.PAID);
        OrderResponse updated = new OrderResponse();
        when(commandService.updateOrderStatus(id, OrderStatus.PAID)).thenReturn(updated);

        ApiResponse<OrderResponse> response = controller.updateStatus(id, request);

        assertEquals(200, response.code());
        assertSame(updated, response.data());
    }

    @Test
    void cancelReturnsNotFoundWhenCommandReturnsNull() {
        UUID id = UUID.randomUUID();
        OrderCancelRequest request = new OrderCancelRequest();
        request.setReason("user cancel");
        when(commandService.cancelOrderResponse(id, "user cancel")).thenReturn(null);

        ApiResponse<OrderResponse> response = controller.cancel(id, request);

        assertEquals(404, response.code());
        assertEquals("订单不存在: id=" + id, response.message());
    }

    @Test
    void cancelReturnsMappedResponseFromCommandService() {
        UUID id = UUID.randomUUID();
        OrderCancelRequest request = new OrderCancelRequest();
        request.setReason("user cancel");
        OrderResponse cancelled = new OrderResponse();
        when(commandService.cancelOrderResponse(id, "user cancel")).thenReturn(cancelled);

        ApiResponse<OrderResponse> response = controller.cancel(id, request);

        assertEquals(200, response.code());
        assertSame(cancelled, response.data());
    }

    @Test
    void dashboardSummaryDelegatesToDashboardService() {
        OrderDashboardSummaryResponse summary = new OrderDashboardSummaryResponse();
        when(dashboardQueryService.summary(OrderDashboardRange.LAST_7_DAYS,
                OrderDashboardCompare.PREVIOUS_PERIOD)).thenReturn(summary);

        ApiResponse<OrderDashboardSummaryResponse> response =
                controller.dashboardSummary(OrderDashboardRange.LAST_7_DAYS,
                        OrderDashboardCompare.PREVIOUS_PERIOD);

        assertEquals(200, response.code());
        assertSame(summary, response.data());
    }
}