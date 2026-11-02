package com.scmcloud.order.service.query;

import com.scmcloud.common.domain.Money;
import com.scmcloud.order.domain.entity.OrdRefund;
import com.scmcloud.order.domain.entity.OrdRefundItem;
import com.scmcloud.order.domain.entity.RefundStatus;
import com.scmcloud.order.dto.RefundResponse;
import com.scmcloud.order.mapper.OrdRefundMapper;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdRefundQueryServiceTest {

    @Mock
    private OrdRefundMapper ordRefundMapper;

    @Mock
    private OrdRefundItemQueryService refundItemQueryService;

    @InjectMocks
    private OrdRefundQueryService service;

    @Test
    void getResponseByIdAssemblesItems() {
        UUID id = UUID.randomUUID();
        OrdRefund refund = new OrdRefund();
        refund.setRefundNo("REF-1");
        refund.setRefundAmount(Money.of(new BigDecimal("20.00")));
        refund.setStatusEnum(RefundStatus.PENDING);
        OrdRefundItem item = new OrdRefundItem();
        item.setSkuName("SKU-1");
        when(ordRefundMapper.selectById(id)).thenReturn(refund);
        when(refundItemQueryService.listByRefundId(id)).thenReturn(List.of(item));

        RefundResponse response = service.getResponseById(id);

        assertEquals("REF-1", response.getRefundNo());
        assertEquals("PENDING", response.getRefundStatus());
        assertEquals(1, response.getItems().size());
        assertEquals("SKU-1", response.getItems().get(0).getSkuName());
    }

    @Test
    void getResponseByIdReturnsNullWithoutLoadingItemsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(ordRefundMapper.selectById(id)).thenReturn(null);

        assertNull(service.getResponseById(id));
        verify(refundItemQueryService, never()).listByRefundId(id);
    }

    @Test
    void listResponsesByOrderIdReturnsEmptyWhenOrderIdIsNull() {
        List<RefundResponse> responses = service.listResponsesByOrderId(null);

        assertEquals(0, responses.size());
        verify(ordRefundMapper, never()).selectList(org.mockito.ArgumentMatchers.any());
    }
}
