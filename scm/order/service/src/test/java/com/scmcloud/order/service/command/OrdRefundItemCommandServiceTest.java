package com.scmcloud.order.service.command;

import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.TenantId;
import com.scmcloud.order.domain.entity.OrdRefund;
import com.scmcloud.order.domain.entity.OrdRefundItem;
import com.scmcloud.order.domain.entity.RefundStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link OrdRefundItemCommandService#saveBatch(List)}.
 *
 * <p>不依赖 Spring / 数据库，验证：</p>
 * <ul>
 *   <li>空集合直接返回 0，不调用 mapper</li>
 *   <li>非空集合逐条 insert 并返回 size</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OrdRefundItemCommandServiceTest {

    @Mock
    private com.scmcloud.order.mapper.OrdRefundItemMapper ordRefundItemMapper;

    private OrdRefundItemCommandService service;

    @BeforeEach
    void setUp() {
        service = new OrdRefundItemCommandService(ordRefundItemMapper);
    }

    @Test
    void saveBatchShouldReturnZeroForEmptyList() {
        int saved = service.saveBatch(List.of());
        assertEquals(0, saved);
    }

    @Test
    void saveBatchShouldInsertEachItem() {
        OrdRefundItem item1 = item(UUID.randomUUID());
        OrdRefundItem item2 = item(UUID.randomUUID());

        int saved = service.saveBatch(List.of(item1, item2));

        assertEquals(2, saved);
        ArgumentCaptor<OrdRefundItem> captor = ArgumentCaptor.forClass(OrdRefundItem.class);
        verify(ordRefundItemMapper, times(2)).insert(captor.capture());
        assertEquals(item1.getSkuId(), captor.getAllValues().get(0).getSkuId());
        assertEquals(item2.getSkuId(), captor.getAllValues().get(1).getSkuId());
    }

    private OrdRefundItem item(UUID skuId) {
        OrdRefundItem item = new OrdRefundItem();
        item.setTenantId(TenantId.generate());
        item.setRefundId(UUID.randomUUID());
        item.setRefundNo("RF-001");
        item.setOrderId(UUID.randomUUID());
        item.setOrderNo("ORD-001");
        item.setOrderItemId(UUID.randomUUID());
        item.setSkuId(skuId);
        item.setSkuCode("SKU-" + skuId.toString().substring(0, 4));
        item.setSkuName("Test SKU");
        item.setQuantity(1);
        item.setRefundAmount(Money.of(new BigDecimal("99.90")));
        return item;
    }

    @Test
    void ordRefundStatusEnumShouldRoundTrip() {
        for (RefundStatus s : RefundStatus.values()) {
            assertEquals(s, RefundStatus.fromCode(s.getCode()));
        }
    }

    @Test
    void ordRefundSetStatusEnumShouldWriteCode() {
        OrdRefund refund = new OrdRefund();
        refund.setStatusEnum(RefundStatus.APPROVED);
        assertEquals(RefundStatus.APPROVED.getCode(), refund.getStatus());
        assertEquals(RefundStatus.APPROVED, refund.getStatusEnum());
    }
}
