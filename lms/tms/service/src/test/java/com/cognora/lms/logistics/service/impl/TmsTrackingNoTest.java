package com.cognora.lms.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cognora.lms.logistics.domain.WaybillStatus;
import com.cognora.lms.logistics.domain.entity.TmsTracking;
import com.cognora.lms.logistics.domain.entity.TmsWaybill;
import com.cognora.lms.logistics.mapper.TmsTrackingMapper;
import com.cognora.lms.logistics.mapper.TmsWaybillMapper;
import com.cognora.platform.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 批次 3 补遗：承运商回传真实跟踪号写入运单并随轨迹留存。
 */
class TmsTrackingNoTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String WAYBILL_ID = "waybill-1";
    private static final String WAYBILL_NO = "WB-1";

    private TmsTrackingMapper trackingMapper;
    private TmsWaybillMapper waybillMapper;
    private TmsTrackingServiceImpl service;

    @BeforeEach
    void setUp() {
        trackingMapper = mock(TmsTrackingMapper.class);
        waybillMapper = mock(TmsWaybillMapper.class);
        service = new TmsTrackingServiceImpl(waybillMapper);
        ReflectionTestUtils.setField(service, "baseMapper", trackingMapper);
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void firstCarrierTrackingNoShouldBeRecordedOnWaybillAndTracking() {
        when(trackingMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(waybillMapper.selectById(WAYBILL_ID)).thenReturn(waybill(WaybillStatus.CREATED, null));
        when(trackingMapper.insert(any(TmsTracking.class))).thenReturn(1);
        when(waybillMapper.updateById(any(TmsWaybill.class))).thenReturn(1);

        TmsTracking tracking = event("evt-1", "PICKED_UP", "SF1234567890");
        TmsTracking created = service.addTracking(tracking);

        ArgumentCaptor<TmsWaybill> captor = ArgumentCaptor.forClass(TmsWaybill.class);
        verify(waybillMapper, org.mockito.Mockito.times(2)).updateById(captor.capture());
        TmsWaybill trackingNoPatch = captor.getAllValues().get(1);
        assertEquals("SF1234567890", trackingNoPatch.getTrackingNo());
        assertEquals(WaybillStatus.PICKED_UP, captor.getAllValues().get(0).getStatus());
        assertEquals("SF1234567890", created.getTrackingNo());
        assertNotNull(created.getId());
    }

    @Test
    void existingWaybillTrackingNoShouldNotBeOverwritten() {
        when(trackingMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(waybillMapper.selectById(WAYBILL_ID))
                .thenReturn(waybill(WaybillStatus.IN_TRANSIT, "SF-ORIGINAL"));
        when(trackingMapper.insert(any(TmsTracking.class))).thenReturn(1);

        service.addTracking(event("evt-2", "IN_TRANSIT", "SF-CHANGED"));

        // 状态未推进且跟踪号已存在：不应产生任何运单更新。
        verify(waybillMapper, never()).updateById(any(TmsWaybill.class));
    }

    @Test
    void blankTrackingNoShouldNotTouchWaybill() {
        when(trackingMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(waybillMapper.selectById(WAYBILL_ID)).thenReturn(waybill(WaybillStatus.IN_TRANSIT, null));
        when(trackingMapper.insert(any(TmsTracking.class))).thenReturn(1);

        service.addTracking(event("evt-3", "IN_TRANSIT", "  "));

        verify(waybillMapper, never()).updateById(any(TmsWaybill.class));
    }

    private TmsTracking event(String eventId, String trackStatus, String trackingNo) {
        TmsTracking tracking = new TmsTracking();
        tracking.setEventId(eventId);
        tracking.setWaybillId(WAYBILL_ID);
        tracking.setWaybillNo(WAYBILL_NO);
        tracking.setTrackStatus(trackStatus);
        tracking.setTrackingNo(trackingNo);
        tracking.setTrackTime(LocalDateTime.of(2026, 11, 6, 10, 0));
        tracking.setLocation("上海市浦东新区");
        return tracking;
    }

    private TmsWaybill waybill(int status, String trackingNo) {
        TmsWaybill waybill = new TmsWaybill();
        waybill.setId(WAYBILL_ID);
        waybill.setWaybillNo(WAYBILL_NO);
        waybill.setStatus(status);
        waybill.setTrackingNo(trackingNo);
        return waybill;
    }
}
