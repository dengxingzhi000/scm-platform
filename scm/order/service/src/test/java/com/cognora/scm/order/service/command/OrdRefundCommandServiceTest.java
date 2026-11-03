package com.cognora.scm.order.service.command;

import com.cognora.scm.order.domain.entity.OrdRefund;
import com.cognora.scm.order.dto.RefundCreateRequest;
import com.cognora.scm.order.dto.RefundItemRequest;
import com.cognora.scm.order.dto.RefundResponse;
import com.cognora.scm.order.mapper.OrdRefundMapper;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdRefundCommandServiceTest {

    @Mock
    private OrdRefundMapper ordRefundMapper;

    @Mock
    private OrdRefundItemCommandService ordRefundItemCommandService;

    @InjectMocks
    private OrdRefundCommandService service;

    @Test
    void createRefundMapsRequestItemsAndResponse() {
        RefundCreateRequest request = new RefundCreateRequest();
        request.setRefundNo("REF-1");
        request.setOrderId(UUID.randomUUID());
        request.setOrderNo("ORD-1");
        request.setUserId("user-1");
        request.setRefundType(1);
        request.setReason("damaged");
        request.setRefundAmount(new BigDecimal("20.00"));
        RefundItemRequest item = new RefundItemRequest();
        item.setOrderItemId(UUID.randomUUID());
        item.setSkuId(UUID.randomUUID().toString());
        item.setSkuName("SKU-1");
        item.setQuantity(1);
        item.setRefundAmount(new BigDecimal("20.00"));
        request.setItems(List.of(item));
        when(ordRefundMapper.insert(any(OrdRefund.class))).thenReturn(1);

        RefundResponse response = service.createRefund(request);

        assertEquals("REF-1", response.getRefundNo());
        assertEquals("PENDING", response.getRefundStatus());
        assertEquals(new BigDecimal("20.00"), response.getRefundAmount());
        assertNull(response.getItems());
        verify(ordRefundItemCommandService).saveBatch(anyList());
    }
}
