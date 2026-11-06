package com.cognora.scm.finance.service.impl;

import com.cognora.platform.common.exception.ServiceException;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.scm.finance.domain.entity.ReconciliationDiff;
import com.cognora.scm.finance.domain.entity.ReconciliationRecord;
import com.cognora.scm.finance.domain.enums.ReconciliationDiffStatus;
import com.cognora.scm.finance.mapper.ReconciliationDiffMapper;
import com.cognora.scm.finance.mapper.ReconciliationRecordMapper;
import com.cognora.scm.finance.service.support.AccountingPeriodGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationDiffServiceImplTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");
    private static final String FOREIGN_TENANT = "019a0aee-0000-7000-8000-000000000099";

    @Mock
    private ReconciliationDiffMapper diffMapper;

    @Mock
    private ReconciliationRecordMapper recordMapper;

    private ReconciliationDiffServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationDiffServiceImpl(diffMapper, recordMapper, mock(AccountingPeriodGuard.class));
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createShouldComputeAbsoluteDiffAndForceTenant() {
        stubParent(TENANT.toString());
        when(diffMapper.insert(any(ReconciliationDiff.class))).thenReturn(1);

        ReconciliationDiff request = new ReconciliationDiff();
        request.setReconciliationId("rec-1");
        request.setOurAmount(new BigDecimal("100.00"));
        request.setTheirAmount(new BigDecimal("95.00"));

        ReconciliationDiff created = service.create(request);

        assertEquals(TENANT.toString(), created.getTenantId());
        assertEquals("5.00", created.getDiffAmount().toPlainString());
        assertEquals("100.00", created.getOurAmount().toPlainString());
        assertEquals(ReconciliationDiffStatus.DISCOVERED.getCode(), created.getStatus());
        assertEquals("REC-1", created.getReconciliationNo());
        verify(diffMapper).insert(created);
    }

    @Test
    void createShouldRejectForeignTenantInRequestBody() {
        ReconciliationDiff request = new ReconciliationDiff();
        request.setReconciliationId("rec-1");
        request.setTenantId(FOREIGN_TENANT);

        assertThrows(AccessDeniedException.class, () -> service.create(request));
        verify(diffMapper, never()).insert(any(ReconciliationDiff.class));
    }

    @Test
    void createShouldRejectThreeDecimalAmount() {
        stubParent(TENANT.toString());
        ReconciliationDiff request = new ReconciliationDiff();
        request.setReconciliationId("rec-1");
        request.setOurAmount(new BigDecimal("100.005"));

        assertThrows(IllegalArgumentException.class, () -> service.create(request));
        verify(diffMapper, never()).insert(any(ReconciliationDiff.class));
    }

    @Test
    void createShouldRejectMissingParentReconciliation() {
        when(recordMapper.selectById("rec-1")).thenReturn(null);

        ReconciliationDiff request = new ReconciliationDiff();
        request.setReconciliationId("rec-1");

        assertThrows(IllegalArgumentException.class, () -> service.create(request));
        verify(diffMapper, never()).insert(any(ReconciliationDiff.class));
    }

    @Test
    void confirmShouldMoveDiscoveredToConfirmed() {
        when(diffMapper.selectById("diff-1")).thenReturn(diff(ReconciliationDiffStatus.DISCOVERED));
        when(diffMapper.updateById(any(ReconciliationDiff.class))).thenReturn(1);

        ReconciliationDiff result = service.confirm("diff-1", "客户承认多收", "user-1");

        assertEquals(ReconciliationDiffStatus.CONFIRMED.getCode(), result.getStatus());
        assertEquals("客户承认多收", result.getResolution());
        assertEquals("user-1", result.getResolvedBy());
        verify(diffMapper).updateById(result);
    }

    @Test
    void rejectShouldMoveDiscoveredToRejected() {
        when(diffMapper.selectById("diff-1")).thenReturn(diff(ReconciliationDiffStatus.DISCOVERED));
        when(diffMapper.updateById(any(ReconciliationDiff.class))).thenReturn(1);

        ReconciliationDiff result = service.reject("diff-1", "系统时间差，非实际差异", "user-1");

        assertEquals(ReconciliationDiffStatus.REJECTED.getCode(), result.getStatus());
        verify(diffMapper).updateById(result);
    }

    @Test
    void illegalTransitionFromWrittenOffShouldBeRejected() {
        when(diffMapper.selectById("diff-1")).thenReturn(diff(ReconciliationDiffStatus.WRITTEN_OFF));

        assertThrows(ServiceException.class, () -> service.confirm("diff-1", "再次确认", "user-1"));
        verify(diffMapper, never()).updateById(any(ReconciliationDiff.class));
    }

    @Test
    void rejectedIsTerminalAndCannotBeConfirmed() {
        when(diffMapper.selectById("diff-1")).thenReturn(diff(ReconciliationDiffStatus.REJECTED));

        assertThrows(ServiceException.class, () -> service.confirm("diff-1", "再次确认", "user-1"));
        verify(diffMapper, never()).updateById(any(ReconciliationDiff.class));
    }

    @Test
    void writeOffShouldRequireConfirmedFirst() {
        when(diffMapper.selectById("diff-1")).thenReturn(diff(ReconciliationDiffStatus.DISCOVERED));

        assertThrows(ServiceException.class, () -> service.writeOff("diff-1", "直接核销", "user-1"));
        verify(diffMapper, never()).updateById(any(ReconciliationDiff.class));
    }

    @Test
    void writeOffShouldMoveConfirmedToWrittenOff() {
        when(diffMapper.selectById("diff-1")).thenReturn(diff(ReconciliationDiffStatus.CONFIRMED));
        when(diffMapper.updateById(any(ReconciliationDiff.class))).thenReturn(1);

        ReconciliationDiff result = service.writeOff("diff-1", "金额低于阈值，核销", "user-1");

        assertEquals(ReconciliationDiffStatus.WRITTEN_OFF.getCode(), result.getStatus());
        verify(diffMapper).updateById(result);
    }

    @Test
    void crossTenantDiffShouldBeRejectedWith403() {
        ReconciliationDiff foreign = diff(ReconciliationDiffStatus.DISCOVERED);
        foreign.setTenantId(FOREIGN_TENANT);
        when(diffMapper.selectById("diff-1")).thenReturn(foreign);

        assertThrows(AccessDeniedException.class, () -> service.confirm("diff-1", "确认", "user-1"));
        verify(diffMapper, never()).updateById(any(ReconciliationDiff.class));
    }

    @Test
    void confirmShouldRequireResolution() {
        assertThrows(IllegalArgumentException.class, () -> service.confirm("diff-1", " ", "user-1"));
        verify(diffMapper, never()).selectById("diff-1");
    }

    private void stubParent(String tenantId) {
        ReconciliationRecord parent = new ReconciliationRecord();
        parent.setId("rec-1");
        parent.setTenantId(tenantId);
        parent.setReconciliationNo("REC-1");
        parent.setReconciliationPeriod("2026-10");
        parent.setDeleted(false);
        when(recordMapper.selectById("rec-1")).thenReturn(parent);
    }

    private ReconciliationDiff diff(ReconciliationDiffStatus status) {
        ReconciliationDiff diff = new ReconciliationDiff();
        diff.setId("diff-1");
        diff.setTenantId(TENANT.toString());
        diff.setStatus(status.getCode());
        diff.setDeleted(false);
        return diff;
    }
}
