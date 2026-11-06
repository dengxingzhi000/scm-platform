package com.cognora.lms.logistics.service.impl;

import com.cognora.lms.logistics.domain.IllegalStatusTransitionException;
import com.cognora.lms.logistics.domain.WaybillStatus;
import com.cognora.lms.logistics.domain.entity.TmsWaybill;
import com.cognora.lms.logistics.mapper.TmsWaybillMapper;
import com.cognora.platform.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 批次 2：运单状态守卫用例（非法跳转 400、终态不可逆、幂等重放、取消边界）。
 */
class TmsWaybillStatusGuardTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String WAYBILL_ID = "waybill-1";

    private TmsWaybillMapper mapper;
    private TmsWaybillServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(TmsWaybillMapper.class);
        service = new TmsWaybillServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void illegalSkipFromCreatedToDeliveredShouldBeRejected() {
        when(mapper.selectById(WAYBILL_ID)).thenReturn(waybill(WaybillStatus.CREATED));

        IllegalStatusTransitionException rejected = assertThrows(IllegalStatusTransitionException.class,
                () -> service.updateStatus(WAYBILL_ID, WaybillStatus.DELIVERED, "tester"));

        assertNotNull(rejected.getMessage());
        verify(mapper, never()).updateById(any(TmsWaybill.class));
    }

    @Test
    void deliveredWaybillShouldNotMoveBackwards() {
        TmsWaybill delivered = waybill(WaybillStatus.DELIVERED);
        delivered.setSignTime(java.time.LocalDateTime.of(2026, 11, 4, 10, 0));
        when(mapper.selectById(WAYBILL_ID)).thenReturn(delivered);

        IllegalStatusTransitionException rejected = assertThrows(IllegalStatusTransitionException.class,
                () -> service.updateStatus(WAYBILL_ID, WaybillStatus.IN_TRANSIT, "tester"));

        assertTrue(rejected.getMessage().contains("终态"));
        verify(mapper, never()).updateById(any(TmsWaybill.class));
    }

    @Test
    void repeatedDeliveryShouldBeIdempotentAndKeepSignTime() {
        TmsWaybill delivered = waybill(WaybillStatus.DELIVERED);
        delivered.setSignTime(java.time.LocalDateTime.of(2026, 11, 4, 10, 0));
        when(mapper.selectById(WAYBILL_ID)).thenReturn(delivered);

        boolean result = service.updateStatus(WAYBILL_ID, WaybillStatus.DELIVERED, "tester");

        assertTrue(result);
        verify(mapper, never()).updateById(any(TmsWaybill.class));
    }

    @Test
    void legalTransitionShouldPatchStatusAndSignTime() {
        when(mapper.selectById(WAYBILL_ID)).thenReturn(waybill(WaybillStatus.OUT_FOR_DELIVERY));
        when(mapper.updateById(any(TmsWaybill.class))).thenReturn(1);

        service.updateStatus(WAYBILL_ID, WaybillStatus.DELIVERED, "tester");

        ArgumentCaptor<TmsWaybill> captor = ArgumentCaptor.forClass(TmsWaybill.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(WaybillStatus.DELIVERED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getSignTime());
        assertNotNull(captor.getValue().getActualDelivery());
    }

    @Test
    void cancelDeliveredWaybillShouldBeRejected() {
        when(mapper.selectById(WAYBILL_ID)).thenReturn(waybill(WaybillStatus.DELIVERED));

        assertThrows(IllegalStatusTransitionException.class,
                () -> service.cancelWaybill(WAYBILL_ID, "customer request", "tester"));
        verify(mapper, never()).updateById(any(TmsWaybill.class));
    }

    @Test
    void cancelFromTransitShouldMoveToReturned() {
        when(mapper.selectById(WAYBILL_ID)).thenReturn(waybill(WaybillStatus.IN_TRANSIT));
        when(mapper.updateById(any(TmsWaybill.class))).thenReturn(1);

        service.cancelWaybill(WAYBILL_ID, "customer request", "tester");

        ArgumentCaptor<TmsWaybill> captor = ArgumentCaptor.forClass(TmsWaybill.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(WaybillStatus.RETURNED, captor.getValue().getStatus());
        assertEquals("CANCEL", captor.getValue().getExceptionType());
    }

    @Test
    void deleteDeliveredWaybillShouldBeRejected() {
        when(mapper.selectById(WAYBILL_ID)).thenReturn(waybill(WaybillStatus.DELIVERED));

        assertThrows(IllegalStatusTransitionException.class, () -> service.deleteWaybill(WAYBILL_ID));
        verify(mapper, never()).updateById(any(TmsWaybill.class));
    }

    @Test
    void updateWithoutTenantContextShouldBeRejected() {
        TenantContextHolder.clear();
        TmsWaybill waybill = waybill(WaybillStatus.CREATED);
        when(mapper.selectById(WAYBILL_ID)).thenReturn(waybill);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.updateStatus(WAYBILL_ID, WaybillStatus.PICKED_UP, "tester"));
        verify(mapper, never()).updateById(any(TmsWaybill.class));
    }

    private TmsWaybill waybill(int status) {
        TmsWaybill waybill = new TmsWaybill();
        waybill.setId(WAYBILL_ID);
        waybill.setWaybillNo("WB-1");
        waybill.setStatus(status);
        waybill.setDeleted(false);
        return waybill;
    }
}
