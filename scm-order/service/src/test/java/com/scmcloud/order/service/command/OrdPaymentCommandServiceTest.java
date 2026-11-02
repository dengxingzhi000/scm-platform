package com.scmcloud.order.service.command;

import com.scmcloud.order.domain.entity.OrdPayment;
import com.scmcloud.order.dto.PaymentCreateRequest;
import com.scmcloud.order.dto.PaymentResponse;
import com.scmcloud.order.mapper.OrdPaymentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdPaymentCommandServiceTest {

    @Mock
    private OrdPaymentMapper ordPaymentMapper;

    @InjectMocks
    private OrdPaymentCommandService service;

    @Test
    void createPaymentMapsRequestAndResponse() {
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setPaymentNo("PAY-1");
        request.setOrderId(UUID.randomUUID());
        request.setOrderNo("ORD-1");
        request.setUserId("user-1");
        request.setPaymentMethod(1);
        request.setPaymentAmount(new BigDecimal("12.50"));
        when(ordPaymentMapper.insert(any(OrdPayment.class))).thenReturn(1);

        PaymentResponse response = service.createPayment(request);

        assertEquals("PAY-1", response.getPaymentNo());
        assertEquals("PENDING", response.getPaymentStatus());
        assertEquals(new BigDecimal("12.50"), response.getPaymentAmount());
        assertNotNull(response.getInitiatedAt());
        verify(ordPaymentMapper).insert(any(OrdPayment.class));
    }
}
