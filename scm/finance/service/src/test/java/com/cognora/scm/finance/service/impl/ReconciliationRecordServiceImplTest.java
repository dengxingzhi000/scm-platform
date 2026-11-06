package com.cognora.scm.finance.service.impl;

import com.cognora.platform.common.exception.ServiceException;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.scm.finance.domain.entity.ReconciliationRecord;
import com.cognora.scm.finance.mapper.ReconciliationRecordMapper;
import com.cognora.scm.finance.service.support.AccountingPeriodGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 对账记录状态机：0-待对账 → 1-已对账 / 3-有差异 → 2-已确认 / 4-已驳回。
 * 非法流转统一返回 409（ServiceException CONFLICT），跨租户返回 403。
 */
@ExtendWith(MockitoExtension.class)
class ReconciliationRecordServiceImplTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");
    private static final String RECORD_ID = "019ac000-0000-7000-8000-000000000001";

    @Mock
    private ReconciliationRecordMapper mapper;

    @Mock
    private AccountingPeriodGuard accountingPeriodGuard;

    private ReconciliationRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationRecordServiceImpl(accountingPeriodGuard);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void reconcileShouldMovePendingToReconciled() {
        when(mapper.selectById(RECORD_ID)).thenReturn(record(0, false));
        when(mapper.updateById(any(ReconciliationRecord.class))).thenReturn(1);

        ReconciliationRecord result = service.reconcile(RECORD_ID, "u1", "张三");

        assertEquals(1, result.getStatus());
        assertEquals("u1", result.getReconcilerId());
    }

    @Test
    void reconcileShouldMovePendingToDiffWhenAmountDiffers() {
        when(mapper.selectById(RECORD_ID)).thenReturn(record(0, true));
        when(mapper.updateById(any(ReconciliationRecord.class))).thenReturn(1);

        assertEquals(3, service.reconcile(RECORD_ID, "u1", "张三").getStatus());
    }

    @Test
    void reconcileShouldRejectAlreadyReconciledRecord() {
        when(mapper.selectById(RECORD_ID)).thenReturn(record(1, false));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.reconcile(RECORD_ID, "u1", "张三"));

        assertEquals(409, error.getCode());
        verify(mapper, never()).updateById(any(ReconciliationRecord.class));
    }

    @Test
    void confirmShouldAcceptReconciledAndDiffRecords() {
        when(mapper.selectById(RECORD_ID)).thenReturn(record(1, false));
        when(mapper.updateById(any(ReconciliationRecord.class))).thenReturn(1);
        assertEquals(2, service.confirm(RECORD_ID, "u2", "李四").getStatus());

        when(mapper.selectById(RECORD_ID)).thenReturn(record(3, true));
        assertEquals(2, service.confirm(RECORD_ID, "u2", "李四").getStatus());
    }

    @Test
    void confirmShouldRejectPendingRecord() {
        when(mapper.selectById(RECORD_ID)).thenReturn(record(0, false));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.confirm(RECORD_ID, "u2", "李四"));

        assertEquals(409, error.getCode());
        verify(mapper, never()).updateById(any(ReconciliationRecord.class));
    }

    @Test
    void markAsDiffShouldNormalizeTypeAndRejectUnknownType() {
        when(mapper.selectById(RECORD_ID)).thenReturn(record(1, false));
        when(mapper.updateById(any(ReconciliationRecord.class))).thenReturn(1);

        ReconciliationRecord result = service.markAsDiff(RECORD_ID, "amount", "金额不一致");
        assertEquals("AMOUNT", result.getDiffType());
        assertEquals(3, result.getStatus());
        assertTrue(result.getHasDiff());

        assertThrows(IllegalArgumentException.class,
                () -> service.markAsDiff(RECORD_ID, "UNKNOWN", "非法类型"));
    }

    @Test
    void markAsDiffShouldRejectConfirmedRecord() {
        when(mapper.selectById(RECORD_ID)).thenReturn(record(2, false));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.markAsDiff(RECORD_ID, "AMOUNT", "已确认后不允许改差异"));

        assertEquals(409, error.getCode());
    }

    @Test
    void rejectShouldAcceptReconciledAndDiffRecords() {
        when(mapper.selectById(RECORD_ID)).thenReturn(record(1, false));
        when(mapper.updateById(any(ReconciliationRecord.class))).thenReturn(1);
        ReconciliationRecord fromReconciled = service.reject(RECORD_ID, "对方不认可", "u3");
        assertEquals(4, fromReconciled.getStatus());
        assertEquals("对方不认可", fromReconciled.getRejectReason());

        when(mapper.selectById(RECORD_ID)).thenReturn(record(3, true));
        assertEquals(4, service.reject(RECORD_ID, "证据不足", "u3").getStatus());
    }

    @Test
    void rejectShouldRefuseIllegalTransitions() {
        for (int status : new int[]{0, 2, 4}) {
            when(mapper.selectById(RECORD_ID)).thenReturn(record(status, false));
            ServiceException error = assertThrows(ServiceException.class,
                    () -> service.reject(RECORD_ID, "驳回", "u3"), "status=" + status);
            assertEquals(409, error.getCode(), "status=" + status);
        }
        verify(mapper, never()).updateById(any(ReconciliationRecord.class));
    }

    @Test
    void rejectShouldRefuseCrossTenantRecord() {
        ReconciliationRecord foreign = record(1, false);
        foreign.setTenantId("019a0aee-0000-7000-8000-000000000099");
        when(mapper.selectById(RECORD_ID)).thenReturn(foreign);

        assertThrows(AccessDeniedException.class, () -> service.reject(RECORD_ID, "驳回", "u3"));
        verify(mapper, never()).updateById(any(ReconciliationRecord.class));
    }

    @Test
    void rejectShouldHonorClosedAccountingPeriodGuard() {
        when(mapper.selectById(RECORD_ID)).thenReturn(record(1, false));
        doThrow(new ServiceException(409, org.springframework.http.HttpStatus.CONFLICT, "账期已关账"))
                .when(accountingPeriodGuard).assertWritable(any(), any(), anyString());

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.reject(RECORD_ID, "驳回", "u3"));

        assertEquals(409, error.getCode());
        verify(mapper, never()).updateById(any(ReconciliationRecord.class));
    }

    @Test
    void rejectShouldRejectMissingRecord() {
        when(mapper.selectById(RECORD_ID)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.reject(RECORD_ID, "驳回", "u3"));
    }

    private ReconciliationRecord record(int status, boolean hasDiff) {
        ReconciliationRecord record = new ReconciliationRecord();
        record.setId(RECORD_ID);
        record.setTenantId(TENANT.toString());
        record.setReconciliationNo("RCN202610001");
        record.setReconciliationPeriod("2026-10");
        record.setStatus(status);
        record.setHasDiff(hasDiff);
        record.setDeleted(false);
        return record;
    }
}
