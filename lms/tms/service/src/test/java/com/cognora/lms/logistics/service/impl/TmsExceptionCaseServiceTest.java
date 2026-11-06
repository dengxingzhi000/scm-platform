package com.cognora.lms.logistics.service.impl;

import com.cognora.lms.logistics.domain.WaybillStatus;
import com.cognora.lms.logistics.domain.entity.TmsExceptionCase;
import com.cognora.lms.logistics.domain.entity.TmsWaybill;
import com.cognora.lms.logistics.domain.exceptioncase.ExceptionCaseStatus;
import com.cognora.lms.logistics.mapper.TmsExceptionCaseMapper;
import com.cognora.lms.logistics.service.ITmsWaybillService;
import com.cognora.platform.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

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
 * 批次 3：异常件登记/认领/流转用例。
 * 覆盖跨租户越权 403、终态运单拒绝、责任方认领与非法流转。
 */
class TmsExceptionCaseServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private TmsExceptionCaseMapper mapper;
    private ITmsWaybillService waybillService;
    private TmsExceptionCaseServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(TmsExceptionCaseMapper.class);
        waybillService = mock(ITmsWaybillService.class);
        service = new TmsExceptionCaseServiceImpl(waybillService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createCaseShouldPersistNewCaseForActiveWaybill() {
        when(waybillService.getById("w1")).thenReturn(waybill(WaybillStatus.IN_TRANSIT));

        TmsExceptionCase created = service.createCase("w1", "damaged", "外包装破损", "tester");

        ArgumentCaptor<TmsExceptionCase> captor = ArgumentCaptor.forClass(TmsExceptionCase.class);
        verify(mapper).insert(captor.capture());
        TmsExceptionCase saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals("WB-1", saved.getWaybillNo());
        assertEquals("DAMAGED", saved.getExceptionType());
        assertEquals(ExceptionCaseStatus.NEW, saved.getStatus());
        assertEquals("tester", saved.getCreateBy());
        assertEquals(saved.getId(), created.getId());
    }

    @Test
    void createCaseForDeliveredWaybillShouldBeRejected() {
        when(waybillService.getById("w1")).thenReturn(waybill(WaybillStatus.DELIVERED));

        assertThrows(IllegalStateException.class,
                () -> service.createCase("w1", "damaged", "外包装破损", "tester"));
        verify(mapper, never()).insert(any(TmsExceptionCase.class));
    }

    @Test
    void createCaseOutsideTenantShouldReturn403() {
        when(waybillService.getById("w1")).thenReturn(null);

        assertThrows(AccessDeniedException.class,
                () -> service.createCase("w1", "damaged", "外包装破损", "tester"));
        verify(mapper, never()).insert(any(TmsExceptionCase.class));
    }

    @Test
    void claimShouldMoveNewCaseToClaimedWithResponsibleParty() {
        TmsExceptionCase existing = exceptionCase(ExceptionCaseStatus.NEW);
        when(mapper.selectById("case-1")).thenReturn(existing);

        TmsExceptionCase claimed = service.claim("case-1", "carrier", "operator-a");

        ArgumentCaptor<TmsExceptionCase> captor = ArgumentCaptor.forClass(TmsExceptionCase.class);
        verify(mapper).updateById(captor.capture());
        TmsExceptionCase patch = captor.getValue();
        assertEquals(ExceptionCaseStatus.CLAIMED, patch.getStatus());
        assertEquals("CARRIER", patch.getResponsibleParty());
        assertEquals("operator-a", patch.getClaimedBy());
        assertNotNull(patch.getClaimedTime());
        assertEquals(ExceptionCaseStatus.CLAIMED, claimed.getStatus());
    }

    @Test
    void claimOutsideTenantShouldReturn403() {
        when(mapper.selectById("case-1")).thenReturn(null);

        assertThrows(AccessDeniedException.class,
                () -> service.claim("case-1", "carrier", "operator-a"));
        verify(mapper, never()).updateById(any(TmsExceptionCase.class));
    }

    @Test
    void claimOnProcessingCaseShouldBeRejected() {
        when(mapper.selectById("case-1")).thenReturn(exceptionCase(ExceptionCaseStatus.PROCESSING));

        assertThrows(IllegalStateException.class,
                () -> service.claim("case-1", "carrier", "operator-a"));
        verify(mapper, never()).updateById(any(TmsExceptionCase.class));
    }

    @Test
    void transitionShouldFollowStateMachineAndRecordResolution() {
        TmsExceptionCase existing = exceptionCase(ExceptionCaseStatus.PROCESSING);
        when(mapper.selectById("case-1")).thenReturn(existing);

        TmsExceptionCase resolved = service.transition("case-1", "resolved", "客户已签收", "operator-b");

        ArgumentCaptor<TmsExceptionCase> captor = ArgumentCaptor.forClass(TmsExceptionCase.class);
        verify(mapper).updateById(captor.capture());
        TmsExceptionCase patch = captor.getValue();
        assertEquals(ExceptionCaseStatus.RESOLVED, patch.getStatus());
        assertEquals("客户已签收", patch.getResolution());
        assertNotNull(patch.getResolvedTime());
        assertEquals(ExceptionCaseStatus.RESOLVED, resolved.getStatus());
    }

    @Test
    void illegalTransitionFromNewToResolvedShouldBeRejected() {
        when(mapper.selectById("case-1")).thenReturn(exceptionCase(ExceptionCaseStatus.NEW));

        assertThrows(IllegalStateException.class,
                () -> service.transition("case-1", "RESOLVED", "done", "operator-b"));
        verify(mapper, never()).updateById(any(TmsExceptionCase.class));
    }

    @Test
    void closedCaseShouldNotTransitionAgain() {
        when(mapper.selectById("case-1")).thenReturn(exceptionCase(ExceptionCaseStatus.CLOSED));

        assertThrows(IllegalStateException.class,
                () -> service.transition("case-1", "PROCESSING", null, "operator-b"));
    }

    @Test
    void missingTenantContextShouldBeRejected() {
        TenantContextHolder.clear();

        assertThrows(AccessDeniedException.class,
                () -> service.claim("case-1", "carrier", "operator-a"));
        verify(mapper, never()).selectById("case-1");
    }

    private TmsExceptionCase exceptionCase(String status) {
        TmsExceptionCase exceptionCase = new TmsExceptionCase();
        exceptionCase.setId("case-1");
        exceptionCase.setWaybillId("w1");
        exceptionCase.setWaybillNo("WB-1");
        exceptionCase.setStatus(status);
        return exceptionCase;
    }

    private TmsWaybill waybill(int status) {
        TmsWaybill waybill = new TmsWaybill();
        waybill.setId("w1");
        waybill.setWaybillNo("WB-1");
        waybill.setStatus(status);
        return waybill;
    }
}
