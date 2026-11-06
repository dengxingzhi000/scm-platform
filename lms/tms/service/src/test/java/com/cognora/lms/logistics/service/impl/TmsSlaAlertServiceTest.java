package com.cognora.lms.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cognora.lms.logistics.domain.sla.SlaCalculator;
import com.cognora.lms.logistics.domain.WaybillStatus;
import com.cognora.lms.logistics.domain.entity.TmsSlaAlert;
import com.cognora.lms.logistics.domain.entity.TmsWaybill;
import com.cognora.lms.logistics.mapper.TmsSlaAlertMapper;
import com.cognora.lms.logistics.service.ITmsWaybillService;
import com.cognora.platform.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 批次 3：SLA 超时扫描与预警确认用例。
 */
class TmsSlaAlertServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 6, 12, 0);

    private TmsSlaAlertMapper mapper;
    private ITmsWaybillService waybillService;
    private TmsSlaAlertServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(TmsSlaAlertMapper.class);
        waybillService = mock(ITmsWaybillService.class);
        service = new TmsSlaAlertServiceImpl(waybillService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void overdueWaybillShouldCreateWarnAlert() {
        when(waybillService.list(any(Wrapper.class))).thenReturn(List.of(waybill(NOW.minusHours(1))));
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(mapper.insert(any(TmsSlaAlert.class))).thenReturn(1);

        int created = service.scanOverdue(NOW);

        ArgumentCaptor<TmsSlaAlert> captor = ArgumentCaptor.forClass(TmsSlaAlert.class);
        verify(mapper).insert(captor.capture());
        TmsSlaAlert alert = captor.getValue();
        assertEquals(1, created);
        assertNotNull(alert.getId());
        assertEquals("WB-1", alert.getWaybillNo());
        assertEquals("carrier-1", alert.getCarrierId());
        assertEquals(NOW.minusHours(1), alert.getPromisedTime());
        assertEquals(0, alert.getOverdueHours().compareTo(new java.math.BigDecimal("1.00")));
        assertEquals(SlaCalculator.LEVEL_WARN, alert.getLevel());
        assertEquals(TmsSlaAlert.STATUS_OPEN, alert.getStatus());
        assertEquals(TENANT_ID.toString(), alert.getTenantId());
    }

    @Test
    void overdueBeyond24HoursShouldCreateCriticalAlert() {
        when(waybillService.list(any(Wrapper.class))).thenReturn(List.of(waybill(NOW.minusHours(25))));
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(mapper.insert(any(TmsSlaAlert.class))).thenReturn(1);

        service.scanOverdue(NOW);

        ArgumentCaptor<TmsSlaAlert> captor = ArgumentCaptor.forClass(TmsSlaAlert.class);
        verify(mapper).insert(captor.capture());
        assertEquals(SlaCalculator.LEVEL_CRITICAL, captor.getValue().getLevel());
    }

    @Test
    void existingAlertShouldMakeScanIdempotent() {
        when(waybillService.list(any(Wrapper.class))).thenReturn(List.of(waybill(NOW.minusHours(1))));
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(new TmsSlaAlert()));

        int created = service.scanOverdue(NOW);

        assertEquals(0, created);
        verify(mapper, never()).insert(any(TmsSlaAlert.class));
    }

    @Test
    void promisedTimeEqualToScanTimeShouldNotAlert() {
        when(waybillService.list(any(Wrapper.class))).thenReturn(List.of(waybill(NOW)));
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        int created = service.scanOverdue(NOW);

        assertEquals(0, created);
        verify(mapper, never()).insert(any(TmsSlaAlert.class));
    }

    @Test
    void acknowledgeOpenAlertShouldPatchAckFields() {
        TmsSlaAlert alert = alert(TmsSlaAlert.STATUS_OPEN);
        when(mapper.selectById("alert-1")).thenReturn(alert);

        service.acknowledge("alert-1", "operator-a");

        ArgumentCaptor<TmsSlaAlert> captor = ArgumentCaptor.forClass(TmsSlaAlert.class);
        verify(mapper).updateById(captor.capture());
        TmsSlaAlert patch = captor.getValue();
        assertEquals(TmsSlaAlert.STATUS_ACK, patch.getStatus());
        assertEquals("operator-a", patch.getAckBy());
        assertNotNull(patch.getAckTime());
    }

    @Test
    void acknowledgeAlreadyAckedAlertShouldBeIdempotent() {
        when(mapper.selectById("alert-1")).thenReturn(alert(TmsSlaAlert.STATUS_ACK));

        service.acknowledge("alert-1", "operator-a");

        verify(mapper, never()).updateById(any(TmsSlaAlert.class));
    }

    @Test
    void acknowledgeClosedAlertShouldBeRejected() {
        when(mapper.selectById("alert-1")).thenReturn(alert(TmsSlaAlert.STATUS_CLOSED));

        assertThrows(IllegalArgumentException.class, () -> service.acknowledge("alert-1", "operator-a"));
        verify(mapper, never()).updateById(any(TmsSlaAlert.class));
    }

    @Test
    void acknowledgeOutsideTenantShouldReturn403() {
        when(mapper.selectById("alert-1")).thenReturn(null);

        assertThrows(AccessDeniedException.class, () -> service.acknowledge("alert-1", "operator-a"));
    }

    private TmsWaybill waybill(LocalDateTime estimatedDelivery) {
        TmsWaybill waybill = new TmsWaybill();
        waybill.setId("w1");
        waybill.setWaybillNo("WB-1");
        waybill.setCarrierId("carrier-1");
        waybill.setStatus(WaybillStatus.IN_TRANSIT);
        waybill.setEstimatedDelivery(estimatedDelivery);
        waybill.setDeleted(false);
        waybill.setTenantId(TENANT_ID.toString());
        return waybill;
    }

    private TmsSlaAlert alert(String status) {
        TmsSlaAlert alert = new TmsSlaAlert();
        alert.setId("alert-1");
        alert.setWaybillNo("WB-1");
        alert.setStatus(status);
        return alert;
    }
}
