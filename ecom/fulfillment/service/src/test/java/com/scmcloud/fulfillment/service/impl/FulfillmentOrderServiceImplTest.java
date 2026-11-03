package com.scmcloud.fulfillment.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.scmcloud.fulfillment.domain.entity.FulfillmentOrder;
import com.scmcloud.fulfillment.mapper.FulfillmentOrderMapper;
import com.scmcloud.fulfillment.service.ITrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FulfillmentOrderServiceImplTest {

    @Mock private FulfillmentOrderMapper fulfillmentOrderMapper;
    @Mock private ITrackingService trackingService;

    private FulfillmentOrderServiceImpl fulfillmentService;

    @BeforeEach
    void setUp() {
        fulfillmentService = new FulfillmentOrderServiceImpl(trackingService);
        ReflectionTestUtils.setField(fulfillmentService, "baseMapper", fulfillmentOrderMapper);
    }

    @Test
    void createFulfillmentRejectsBlankType() {
        assertThrows(IllegalArgumentException.class,
                () -> fulfillmentService.createFulfillment("ORD-1", "user-1", " "));
    }

    @Test
    void pickRejectsOrderThatIsAlreadyPacked() {
        when(fulfillmentOrderMapper.selectOne(any(Wrapper.class), eq(true)))
                .thenReturn(new FulfillmentOrder().setFulfillmentNo("FUL-1").setStatus(3));

        assertThrows(IllegalStateException.class, () -> fulfillmentService.pickItems("FUL-1"));
    }

    @Test
    void shipRejectsMissingTrackingNumber() {
        assertThrows(IllegalArgumentException.class,
                () -> fulfillmentService.shipItems("FUL-1", " ", "SF"));
    }

    @Test
    void confirmRejectsOrderThatWasNotShipped() {
        when(fulfillmentOrderMapper.selectOne(any(Wrapper.class), eq(true)))
                .thenReturn(new FulfillmentOrder().setFulfillmentNo("FUL-1").setStatus(3));

        assertThrows(IllegalStateException.class, () -> fulfillmentService.confirmDelivery("FUL-1"));
    }

    @Test
    void pageQueryRejectsInvalidPage() {
        assertThrows(IllegalArgumentException.class,
                () -> fulfillmentService.pageQuery(0, 20, null, null, null));
    }

    @Test
    void pageQueryRejectsOversizedPageSize() {
        assertThrows(IllegalArgumentException.class,
                () -> fulfillmentService.pageQuery(1, 101, null, null, null));
    }
}
