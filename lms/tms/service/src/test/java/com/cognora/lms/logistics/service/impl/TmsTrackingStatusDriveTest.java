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
 * 批次 2：轨迹事件驱动运单状态用例（签收推进、重复回调不刷新签收时间、乱序事件不回退）。
 */
class TmsTrackingStatusDriveTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final String WAYBILL_ID = "waybill-1";
    private static final LocalDateTime SIGNED_AT = LocalDateTime.of(2026, 11, 4, 10, 0);

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
    void deliveredEventShouldAdvanceWaybillAndStampSignTime() {
        when(trackingMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(trackingMapper.insert(any(TmsTracking.class))).thenReturn(1);
        when(waybillMapper.selectById(WAYBILL_ID)).thenReturn(waybill(WaybillStatus.IN_TRANSIT));
        when(waybillMapper.updateById(any(TmsWaybill.class))).thenReturn(1);

        service.addTracking(event("evt-sign-1", "DELIVERED"));

        ArgumentCaptor<TmsWaybill> captor = ArgumentCaptor.forClass(TmsWaybill.class);
        verify(waybillMapper).updateById(captor.capture());
        assertEquals(WaybillStatus.DELIVERED, captor.getValue().getStatus());
        assertEquals(SIGNED_AT, captor.getValue().getSignTime());
        assertEquals(SIGNED_AT, captor.getValue().getActualDelivery());
    }

    @Test
    void duplicateDeliveredCallbackShouldNotTouchWaybill() {
        TmsTracking existing = new TmsTracking();
        existing.setId("tracking-1");
        existing.setEventId("evt-sign-1");
        existing.setWaybillId(WAYBILL_ID);
        existing.setWaybillNo("WB-1");
        existing.setTrackStatus("DELIVERED");
        when(trackingMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        service.addTracking(event("evt-sign-1", "DELIVERED"));

        verify(trackingMapper, never()).insert(any(TmsTracking.class));
        verify(waybillMapper, never()).updateById(any(TmsWaybill.class));
    }

    @Test
    void outOfOrderBackwardEventShouldNotRollStatusBack() {
        when(trackingMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(trackingMapper.insert(any(TmsTracking.class))).thenReturn(1);
        when(waybillMapper.selectById(WAYBILL_ID)).thenReturn(waybill(WaybillStatus.OUT_FOR_DELIVERY));

        service.addTracking(event("evt-transit-2", "IN_TRANSIT"));

        verify(waybillMapper, never()).updateById(any(TmsWaybill.class));
    }

    @Test
    void signedWaybillShouldIgnoreLaterEvents() {
        when(trackingMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(trackingMapper.insert(any(TmsTracking.class))).thenReturn(1);
        TmsWaybill signed = waybill(WaybillStatus.DELIVERED);
        signed.setSignTime(SIGNED_AT);
        when(waybillMapper.selectById(WAYBILL_ID)).thenReturn(signed);

        service.addTracking(event("evt-exception-1", "EXCEPTION"));

        verify(waybillMapper, never()).updateById(any(TmsWaybill.class));
        assertEquals(SIGNED_AT, signed.getSignTime());
    }

    @Test
    void unknownTrackingStatusShouldNotChangeWaybill() {
        when(trackingMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(trackingMapper.insert(any(TmsTracking.class))).thenReturn(1);
        when(waybillMapper.selectById(WAYBILL_ID)).thenReturn(waybill(WaybillStatus.CREATED));

        service.addTracking(event("evt-weird-1", "WEATHER_DELAY"));

        verify(waybillMapper, never()).updateById(any(TmsWaybill.class));
    }

    private TmsTracking event(String eventId, String trackStatus) {
        TmsTracking tracking = new TmsTracking();
        tracking.setEventId(eventId);
        tracking.setWaybillId(WAYBILL_ID);
        tracking.setWaybillNo("WB-1");
        tracking.setTrackStatus(trackStatus);
        tracking.setTrackTime(SIGNED_AT);
        tracking.setLocation("上海市浦东新区");
        tracking.setDescription("承运商回调");
        return tracking;
    }

    private TmsWaybill waybill(int status) {
        TmsWaybill waybill = new TmsWaybill();
        waybill.setId(WAYBILL_ID);
        waybill.setWaybillNo("WB-1");
        waybill.setStatus(status);
        return waybill;
    }
}
