package com.cognora.lms.logistics.service.impl;

import com.cognora.lms.logistics.domain.entity.TmsCarrier;
import com.cognora.lms.logistics.domain.entity.TmsDeliveryArea;
import com.cognora.lms.logistics.domain.entity.TmsRoute;
import com.cognora.lms.logistics.domain.entity.TmsWaybill;
import com.cognora.lms.logistics.mapper.TmsCarrierMapper;
import com.cognora.lms.logistics.mapper.TmsDeliveryAreaMapper;
import com.cognora.lms.logistics.mapper.TmsRouteMapper;
import com.cognora.lms.logistics.mapper.TmsWaybillMapper;
import com.cognora.platform.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogisticsCrudServiceImplTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    private void attach(Object service, Object mapper) {
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void carrierCreateShouldInitializeFields() {
        TmsCarrierMapper mapper = mock(TmsCarrierMapper.class);
        TmsCarrierServiceImpl service = new TmsCarrierServiceImpl();
        attach(service, mapper);

        TmsCarrier carrier = new TmsCarrier();
        service.createCarrier(carrier);

        assertNotNull(carrier.getId());
        assertNotNull(carrier.getCreateTime());
        assertNotNull(carrier.getUpdateTime());
        assertFalse(carrier.getDeleted());
        verify(mapper).insert(carrier);
    }

    @Test
    void carrierUpdateShouldSetIdAndRefreshTime() {
        TmsCarrierMapper mapper = mock(TmsCarrierMapper.class);
        TmsCarrierServiceImpl service = new TmsCarrierServiceImpl();
        attach(service, mapper);

        TmsCarrier carrier = new TmsCarrier();
        service.updateCarrier("carrier-1", carrier);

        assertEquals("carrier-1", carrier.getId());
        assertNotNull(carrier.getUpdateTime());
        verify(mapper).updateById(carrier);
    }

    @Test
    void carrierDeleteShouldOnlyPatchIdAndDeleted() {
        TmsCarrierMapper mapper = mock(TmsCarrierMapper.class);
        TmsCarrierServiceImpl service = new TmsCarrierServiceImpl();
        attach(service, mapper);

        service.deleteCarrier("carrier-1");

        ArgumentCaptor<TmsCarrier> captor = ArgumentCaptor.forClass(TmsCarrier.class);
        verify(mapper).updateById(captor.capture());
        TmsCarrier patch = captor.getValue();
        assertEquals("carrier-1", patch.getId());
        assertEquals(Boolean.TRUE, patch.getDeleted());
        assertNull(patch.getEnabled());
        assertNotNull(patch.getUpdateTime());
    }

    @Test
    void toggleEnabledShouldOnlyPatchIdEnabledAndTime() {
        TmsCarrierMapper mapper = mock(TmsCarrierMapper.class);
        TmsCarrierServiceImpl service = new TmsCarrierServiceImpl();
        attach(service, mapper);

        service.toggleEnabled("carrier-1", false);

        ArgumentCaptor<TmsCarrier> captor = ArgumentCaptor.forClass(TmsCarrier.class);
        verify(mapper).updateById(captor.capture());
        TmsCarrier patch = captor.getValue();
        assertEquals("carrier-1", patch.getId());
        assertEquals(Boolean.FALSE, patch.getEnabled());
        assertNull(patch.getDeleted());
        assertNotNull(patch.getUpdateTime());
    }

    @Test
    void deliveryAreaCreateAndDeleteShouldKeepOrchestrationInService() {
        TmsDeliveryAreaMapper mapper = mock(TmsDeliveryAreaMapper.class);
        TmsDeliveryAreaServiceImpl service = new TmsDeliveryAreaServiceImpl();
        attach(service, mapper);

        TmsDeliveryArea area = new TmsDeliveryArea();
        service.createArea(area);

        assertNotNull(area.getId());
        assertNotNull(area.getCreateTime());
        assertNotNull(area.getUpdateTime());
        assertFalse(area.getDeleted());
        verify(mapper).insert(area);

        service.deleteArea("area-1");
        ArgumentCaptor<TmsDeliveryArea> captor = ArgumentCaptor.forClass(TmsDeliveryArea.class);
        verify(mapper).updateById(captor.capture());
        assertEquals("area-1", captor.getValue().getId());
        assertEquals(Boolean.TRUE, captor.getValue().getDeleted());
    }

    @Test
    void routeCreateShouldInitializeFields() {
        TmsRouteMapper mapper = mock(TmsRouteMapper.class);
        TmsRouteServiceImpl service = new TmsRouteServiceImpl();
        attach(service, mapper);

        TmsRoute route = new TmsRoute();
        service.createRoute(route);

        assertNotNull(route.getId());
        assertNotNull(route.getCreateTime());
        assertNotNull(route.getUpdateTime());
        verify(mapper).insert(route);
    }

    @Test
    void waybillDeleteShouldKeepOriginalPatchShape() {
        TmsWaybillMapper mapper = mock(TmsWaybillMapper.class);
        TmsWaybillServiceImpl service = new TmsWaybillServiceImpl(null);
        attach(service, mapper);
        TenantContextHolder.setTenantId(TENANT_ID);
        TmsWaybill existing = new TmsWaybill();
        existing.setId("waybill-1");
        when(mapper.selectById("waybill-1")).thenReturn(existing);

        service.deleteWaybill("waybill-1");

        ArgumentCaptor<TmsWaybill> captor = ArgumentCaptor.forClass(TmsWaybill.class);
        verify(mapper).updateById(captor.capture());
        TmsWaybill patch = captor.getValue();
        assertEquals("waybill-1", patch.getId());
        assertEquals(Boolean.TRUE, patch.getDeleted());
        assertNull(patch.getUpdateTime());
    }

    @Test
    void waybillDeleteShouldRejectWaybillOutsideTenant() {
        TmsWaybillMapper mapper = mock(TmsWaybillMapper.class);
        TmsWaybillServiceImpl service = new TmsWaybillServiceImpl(null);
        attach(service, mapper);
        TenantContextHolder.setTenantId(TENANT_ID);
        when(mapper.selectById("waybill-1")).thenReturn(null);

        org.springframework.security.access.AccessDeniedException denied =
                org.junit.jupiter.api.Assertions.assertThrows(
                        org.springframework.security.access.AccessDeniedException.class,
                        () -> service.deleteWaybill("waybill-1"));

        assertEquals("Waybill is not accessible in the current tenant", denied.getMessage());
        verify(mapper, never()).updateById(any(TmsWaybill.class));
    }

    @Test
    void waybillMutationShouldRejectMissingTenantContext() {
        TmsWaybillMapper mapper = mock(TmsWaybillMapper.class);
        TmsWaybillServiceImpl service = new TmsWaybillServiceImpl(null);
        attach(service, mapper);
        TenantContextHolder.clear();

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> service.createWaybill(new TmsWaybill()));

        verify(mapper, never()).insert(any(TmsWaybill.class));
    }

    @Test
    void waybillCreateShouldSetStatusAndTimestamps() {
        TmsWaybillMapper mapper = mock(TmsWaybillMapper.class);
        TmsWaybillServiceImpl service = new TmsWaybillServiceImpl(null);
        attach(service, mapper);
        TenantContextHolder.setTenantId(TENANT_ID);
        // save() reports failure when the insert affects no row, and
        // createWaybill() turns that into an exception.
        when(mapper.insert(any(TmsWaybill.class))).thenReturn(1);

        TmsWaybill waybill = new TmsWaybill();
        service.createWaybill(waybill);

        assertEquals(0, waybill.getStatus());
        assertNotNull(waybill.getId());
        assertNotNull(waybill.getCreateTime());
        assertNotNull(waybill.getUpdateTime());
        assertFalse(waybill.getDeleted());
        verify(mapper).insert(waybill);
    }
}