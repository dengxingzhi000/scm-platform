package com.cognora.lms.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cognora.lms.logistics.domain.entity.TmsTracking;
import com.cognora.lms.logistics.domain.entity.TmsWaybill;
import com.cognora.lms.logistics.mapper.TmsTrackingMapper;
import com.cognora.lms.logistics.mapper.TmsWaybillMapper;
import com.cognora.platform.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 批次 1：物流轨迹回调幂等与租户边界用例。
 * 覆盖重复回调不重复落库/签收、并发重复回调合并、跨租户 403、缺失租户上下文拒绝。
 */
class TmsTrackingIdempotencyTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
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
        TenantContextHolder.setTenantId(TENANT_A);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void duplicateSignedCallbackShouldNotInsertSecondTrackingRecord() {
        TmsTracking existing = existingTracking("tracking-1", "evt-sign-1", "DELIVERED");
        when(trackingMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        TmsTracking incoming = signedEvent("evt-sign-1");

        TmsTracking result = service.addTracking(incoming);

        assertSame(existing, result);
        verify(trackingMapper, never()).insert(any(TmsTracking.class));
        // 重复回调不应再次推进运单状态/重写签收时间。
        verify(waybillMapper, never()).updateById(any(TmsWaybill.class));
    }

    @Test
    void concurrentDuplicateCallbackShouldMergeIntoExistingRecord() {
        TmsTracking existing = existingTracking("tracking-1", "evt-sign-1", "DELIVERED");
        when(trackingMapper.selectOne(any(Wrapper.class))).thenReturn(null, existing);
        when(trackingMapper.insert(any(TmsTracking.class))
                ).thenThrow(new DuplicateKeyException("uk_tms_tracking_event"));
        when(waybillMapper.selectById(WAYBILL_ID)).thenReturn(waybill());

        TmsTracking result = service.addTracking(signedEvent("evt-sign-1"));

        assertSame(existing, result);
        verify(trackingMapper, times(1)).insert(any(TmsTracking.class));
    }

    @Test
    void trackingForWaybillOutsideTenantShouldBeRejected() {
        when(waybillMapper.selectById(WAYBILL_ID)).thenReturn(null);
        when(waybillMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        TmsTracking incoming = signedEvent("evt-cross-tenant");

        AccessDeniedException denied = assertThrows(AccessDeniedException.class,
                () -> service.addTracking(incoming));

        assertEquals("Waybill is not accessible in the current tenant", denied.getMessage());
        verify(trackingMapper, never()).insert(any(TmsTracking.class));
    }

    @Test
    void trackingWithoutTenantContextShouldBeRejected() {
        TenantContextHolder.clear();
        TmsTracking incoming = signedEvent("evt-no-tenant");

        assertThrows(AccessDeniedException.class, () -> service.addTracking(incoming));

        verify(trackingMapper, never()).selectOne(any(Wrapper.class));
        verify(trackingMapper, never()).insert(any(TmsTracking.class));
    }

    @Test
    void missingEventIdShouldFallBackToContentFingerprint() {
        when(trackingMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(waybillMapper.selectById(WAYBILL_ID)).thenReturn(waybill());
        when(trackingMapper.insert(any(TmsTracking.class))).thenReturn(1);

        TmsTracking incoming = signedEvent(null);
        service.addTracking(incoming);

        assertNotNull(incoming.getEventId());
        assertEquals(64, incoming.getEventId().length());
        assertEquals(incoming.getEventId(), incoming.getPayloadHash());
        verify(trackingMapper).insert(incoming);
    }

    private TmsTracking signedEvent(String eventId) {
        TmsTracking tracking = new TmsTracking();
        tracking.setEventId(eventId);
        tracking.setWaybillId(WAYBILL_ID);
        tracking.setWaybillNo(WAYBILL_NO);
        tracking.setTrackStatus("DELIVERED");
        tracking.setTrackTime(LocalDateTime.of(2026, 11, 4, 10, 0));
        tracking.setLocation("上海市浦东新区");
        tracking.setDescription("本人已签收");
        return tracking;
    }

    private TmsTracking existingTracking(String id, String eventId, String trackStatus) {
        TmsTracking tracking = new TmsTracking();
        tracking.setId(id);
        tracking.setEventId(eventId);
        tracking.setWaybillId(WAYBILL_ID);
        tracking.setWaybillNo(WAYBILL_NO);
        tracking.setTrackStatus(trackStatus);
        return tracking;
    }

    private TmsWaybill waybill() {
        TmsWaybill waybill = new TmsWaybill();
        waybill.setId(WAYBILL_ID);
        waybill.setWaybillNo(WAYBILL_NO);
        return waybill;
    }
}
