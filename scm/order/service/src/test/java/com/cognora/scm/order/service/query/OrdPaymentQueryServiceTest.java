package com.cognora.scm.order.service.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.domain.Money;
import com.cognora.scm.order.domain.entity.OrdPayment;
import com.cognora.scm.order.domain.entity.PaymentStatus;
import com.cognora.scm.order.dto.PaymentQueryRequest;
import com.cognora.scm.order.dto.PaymentResponse;
import com.cognora.scm.order.mapper.OrdPaymentMapper;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdPaymentQueryServiceTest {

    @Mock
    private OrdPaymentMapper ordPaymentMapper;

    @InjectMocks
    private OrdPaymentQueryService service;

    @Test
    void pageResponsesMapsRecordsAndPagination() {
        OrdPayment payment = new OrdPayment();
        payment.setPaymentNo("PAY-1");
        payment.setPaymentAmount(Money.of(new BigDecimal("12.50")));
        payment.setStatusEnum(PaymentStatus.PENDING);
        Page<OrdPayment> page = new Page<>(2, 5, 7);
        page.setRecords(List.of(payment));
        when(ordPaymentMapper.selectPage(any(), any())).thenReturn(page);

        PaymentQueryRequest request = new PaymentQueryRequest();
        request.setPageNum(2);
        request.setPageSize(5);

        Page<PaymentResponse> result = service.pageResponses(request);

        assertEquals(2L, result.getCurrent());
        assertEquals(5L, result.getSize());
        assertEquals(7L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("PAY-1", result.getRecords().get(0).getPaymentNo());
        assertEquals("PENDING", result.getRecords().get(0).getPaymentStatus());
        assertEquals(new BigDecimal("12.50"), result.getRecords().get(0).getPaymentAmount());
    }

    @Test
    void getResponseByIdReturnsNullWhenMissing() {
        UUID id = UUID.randomUUID();
        when(ordPaymentMapper.selectById(id)).thenReturn(null);

        assertNull(service.getResponseById(id));
    }

    @Test
    void listResponsesByOrderIdReturnsEmptyWhenOrderIdIsNull() {
        List<PaymentResponse> responses = service.listResponsesByOrderId(null);

        assertEquals(0, responses.size());
        verify(ordPaymentMapper, never()).selectList(any());
    }
}
