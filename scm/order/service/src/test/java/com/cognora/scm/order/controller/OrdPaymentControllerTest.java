package com.cognora.scm.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.scm.order.domain.entity.PaymentStatus;
import com.cognora.scm.order.dto.PaymentCreateRequest;
import com.cognora.scm.order.dto.PaymentQueryRequest;
import com.cognora.scm.order.dto.PaymentResponse;
import com.cognora.scm.order.dto.PaymentStatusUpdateRequest;
import com.cognora.scm.order.service.command.OrdPaymentCommandService;
import com.cognora.scm.order.service.query.OrdPaymentQueryService;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdPaymentControllerTest {

    @Mock
    private OrdPaymentQueryService queryService;

    @Mock
    private OrdPaymentCommandService commandService;

    @InjectMocks
    private OrdPaymentController controller;

    @Test
    void getByIdDelegatesResponseMappingToQueryService() {
        UUID id = UUID.randomUUID();
        PaymentResponse payment = new PaymentResponse();
        when(queryService.getResponseById(id)).thenReturn(payment);

        ApiResponse<PaymentResponse> response = controller.getById(id);

        assertEquals(200, response.code());
        assertSame(payment, response.data());
    }

    @Test
    void listByOrderIdDelegatesResponseMappingToQueryService() {
        UUID orderId = UUID.randomUUID();
        List<PaymentResponse> payments = List.of(new PaymentResponse());
        when(queryService.listResponsesByOrderId(orderId)).thenReturn(payments);

        ApiResponse<List<PaymentResponse>> response = controller.listByOrderId(orderId);

        assertEquals(200, response.code());
        assertSame(payments, response.data());
    }

    @Test
    void queryDelegatesPageMappingToQueryService() {
        PaymentQueryRequest request = new PaymentQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        Page<PaymentResponse> page = new Page<>(1, 10, 4);
        when(queryService.pageResponses(request)).thenReturn(page);

        ApiResponse<Page<PaymentResponse>> response = controller.query(request);

        assertEquals(200, response.code());
        assertEquals(4L, response.data().getTotal());
    }

    @Test
    void createPaymentDelegatesRequestMappingToCommandService() {
        PaymentCreateRequest request = new PaymentCreateRequest();
        PaymentResponse created = new PaymentResponse();
        when(commandService.createPayment(request)).thenReturn(created);

        ApiResponse<PaymentResponse> response = controller.createPayment(request);

        assertEquals(200, response.code());
        assertSame(created, response.data());
    }

    @Test
    void createPaymentReturnsServerErrorWhenServiceReturnsNull() {
        PaymentCreateRequest request = new PaymentCreateRequest();
        when(commandService.createPayment(request)).thenReturn(null);

        ApiResponse<PaymentResponse> response = controller.createPayment(request);

        assertEquals(ResultCode.SERVER_ERROR.getCode(), response.code());
        assertEquals("创建支付记录失败", response.message());
    }

    @Test
    void updateStatusReturnsNotFoundWithoutQueryingWhenUpdateMisses() {
        UUID id = UUID.randomUUID();
        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest();
        request.setTargetStatus(PaymentStatus.SUCCESS);
        request.setReason("paid");
        when(commandService.updatePaymentStatus(id, PaymentStatus.SUCCESS, "paid")).thenReturn(false);

        ApiResponse<PaymentResponse> response = controller.updateStatus(id, request);

        assertEquals(ResultCode.NOT_FOUND.getCode(), response.code());
        verifyNoInteractions(queryService);
    }

    @Test
    void updateStatusReturnsLatestPaymentWhenUpdateSucceeds() {
        UUID id = UUID.randomUUID();
        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest();
        request.setTargetStatus(PaymentStatus.SUCCESS);
        request.setReason("paid");
        PaymentResponse latest = new PaymentResponse();
        when(commandService.updatePaymentStatus(id, PaymentStatus.SUCCESS, "paid")).thenReturn(true);
        when(queryService.getResponseById(id)).thenReturn(latest);

        ApiResponse<PaymentResponse> response = controller.updateStatus(id, request);

        assertEquals(200, response.code());
        assertSame(latest, response.data());
        verify(queryService).getResponseById(id);
    }
}