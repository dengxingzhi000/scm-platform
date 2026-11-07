package com.cognora.scm.finance.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cognora.platform.common.exception.ServiceException;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.scm.finance.domain.dto.CreateBadDebtWriteOffRequest;
import com.cognora.scm.finance.domain.entity.BadDebtWriteOff;
import com.cognora.scm.finance.domain.entity.Invoice;
import com.cognora.scm.finance.domain.entity.SettlementOrder;
import com.cognora.scm.finance.domain.enums.BadDebtWriteOffStatus;
import com.cognora.scm.finance.mapper.BadDebtWriteOffMapper;
import com.cognora.scm.finance.mapper.InvoiceMapper;
import com.cognora.scm.finance.mapper.SettlementOrderMapper;
import com.cognora.scm.finance.service.support.AccountingPeriodGuard;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 坏账核销单据流：状态机非法流转、重复核销被拒、账期绑定、跨租户。
 */
@ExtendWith(MockitoExtension.class)
class BadDebtWriteOffServiceImplTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    private static final String FOREIGN_TENANT = "019a0aee-0000-7000-8000-000000000099";

    @Mock
    private BadDebtWriteOffMapper badDebtWriteOffMapper;

    @Mock
    private SettlementOrderMapper settlementOrderMapper;

    @Mock
    private InvoiceMapper invoiceMapper;

    @Mock
    private AccountingPeriodGuard accountingPeriodGuard;

    private BadDebtWriteOffServiceImpl service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BadDebtWriteOff.class);
        service = new BadDebtWriteOffServiceImpl(
                badDebtWriteOffMapper, settlementOrderMapper, invoiceMapper, accountingPeriodGuard);
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void applyCreatesPendingWriteOffBoundToPeriod() {
        stubSettlement(TENANT.toString());
        when(badDebtWriteOffMapper.selectList(any())).thenReturn(List.of());
        when(badDebtWriteOffMapper.insert(any(BadDebtWriteOff.class))).thenReturn(1);

        BadDebtWriteOff writeOff = service.apply(request("st-1", null, "100.00", "2026-10"));

        assertEquals(BadDebtWriteOffStatus.PENDING.getCode(), writeOff.getStatus());
        assertEquals("2026-10", writeOff.getPeriodCode());
        assertEquals(new BigDecimal("100.00"), writeOff.getAmount());
        assertEquals(TENANT.toString(), writeOff.getTenantId());
        assertTrue(writeOff.getWriteOffNo().startsWith("BD"));
        assertNotNull(writeOff.getAppliedAt());
        verify(badDebtWriteOffMapper).insert(writeOff);
    }

    @Test
    void applyRejectsWhenNoTargetProvided() {
        assertThrows(IllegalArgumentException.class, () -> service.apply(request(null, null, "100.00", "2026-10")));
        verify(badDebtWriteOffMapper, never()).insert(any(BadDebtWriteOff.class));
    }

    @Test
    void applyRejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> service.apply(request("st-1", null, "0", "2026-10")));
        verify(badDebtWriteOffMapper, never()).insert(any(BadDebtWriteOff.class));
    }

    @Test
    void applyRejectsInvalidPeriodFormat() {
        assertThrows(IllegalArgumentException.class, () -> service.apply(request("st-1", null, "100.00", "2026-13")));
    }

    @Test
    void applyRejectsDuplicateAfterWriteOff() {
        stubSettlement(TENANT.toString());
        when(badDebtWriteOffMapper.selectList(any())).thenReturn(List.of(
                existingWriteOff("wo-1", BadDebtWriteOffStatus.WRITTEN_OFF)));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.apply(request("st-1", null, "100.00", "2026-10")));

        assertTrue(exception.getMessage().contains("禁止重复核销"), exception.getMessage());
        verify(badDebtWriteOffMapper, never()).insert(any(BadDebtWriteOff.class));
    }

    @Test
    void applyRejectsInFlightDuplicate() {
        stubSettlement(TENANT.toString());
        when(badDebtWriteOffMapper.selectList(any())).thenReturn(List.of(
                existingWriteOff("wo-1", BadDebtWriteOffStatus.PENDING)));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.apply(request("st-1", null, "100.00", "2026-10")));

        assertTrue(exception.getMessage().contains("进行中"), exception.getMessage());
    }

    @Test
    void applyFailsWhenPeriodClosed() {
        stubSettlement(TENANT.toString());
        when(badDebtWriteOffMapper.selectList(any())).thenReturn(List.of());
        doThrow(closedPeriod()).when(accountingPeriodGuard)
                .assertPeriodWritable(anyString(), anyString());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.apply(request("st-1", null, "100.00", "2026-10")));

        assertEquals(ResultCode.CONFLICT.getCode(), exception.getCode());
        verify(badDebtWriteOffMapper, never()).insert(any(BadDebtWriteOff.class));
    }

    @Test
    void applyDeniesForeignTenantSettlement() {
        stubSettlement(FOREIGN_TENANT);

        assertThrows(AccessDeniedException.class,
                () -> service.apply(request("st-1", null, "100.00", "2026-10")));
        verify(badDebtWriteOffMapper, never()).insert(any(BadDebtWriteOff.class));
    }

    @Test
    void applyRejectsMissingSettlement() {
        when(settlementOrderMapper.selectById("st-1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.apply(request("st-1", null, "100.00", "2026-10")));
    }

    @Test
    void approveMovesPendingToApproved() {
        when(badDebtWriteOffMapper.selectById("wo-1")).thenReturn(
                existingWriteOff("wo-1", BadDebtWriteOffStatus.PENDING));

        BadDebtWriteOff writeOff = service.approve("wo-1", "u-2", "审批人", "同意");

        assertEquals(BadDebtWriteOffStatus.APPROVED.getCode(), writeOff.getStatus());
        assertEquals("u-2", writeOff.getApproverId());
        assertNotNull(writeOff.getApprovedAt());
        verify(badDebtWriteOffMapper).updateById(writeOff);
    }

    @Test
    void approveRejectsIllegalTransition() {
        when(badDebtWriteOffMapper.selectById("wo-1")).thenReturn(
                existingWriteOff("wo-1", BadDebtWriteOffStatus.WRITTEN_OFF));

        assertThrows(ServiceException.class, () -> service.approve("wo-1", "u-2", "审批人", "同意"));
        verify(badDebtWriteOffMapper, never()).updateById(any(BadDebtWriteOff.class));
    }

    @Test
    void writeOffRequiresApproval() {
        when(badDebtWriteOffMapper.selectById("wo-1")).thenReturn(
                existingWriteOff("wo-1", BadDebtWriteOffStatus.PENDING));

        assertThrows(ServiceException.class, () -> service.writeOff("wo-1", "u-3"));
        verify(badDebtWriteOffMapper, never()).updateById(any(BadDebtWriteOff.class));
    }

    @Test
    void writeOffMarksWrittenOff() {
        when(badDebtWriteOffMapper.selectById("wo-1")).thenReturn(
                existingWriteOff("wo-1", BadDebtWriteOffStatus.APPROVED));

        BadDebtWriteOff writeOff = service.writeOff("wo-1", "u-3");

        assertEquals(BadDebtWriteOffStatus.WRITTEN_OFF.getCode(), writeOff.getStatus());
        assertEquals("u-3", writeOff.getWrittenOffBy());
        assertNotNull(writeOff.getWrittenOffAt());
        verify(accountingPeriodGuard).assertPeriodWritable("2026-10", "结算单 st-1");
    }

    @Test
    void writeOffRejectsClosedPeriod() {
        when(badDebtWriteOffMapper.selectById("wo-1")).thenReturn(
                existingWriteOff("wo-1", BadDebtWriteOffStatus.APPROVED));
        doThrow(closedPeriod()).when(accountingPeriodGuard)
                .assertPeriodWritable(anyString(), anyString());

        assertThrows(ServiceException.class, () -> service.writeOff("wo-1", "u-3"));
        verify(badDebtWriteOffMapper, never()).updateById(any(BadDebtWriteOff.class));
    }

    @Test
    void rejectMovesPendingToRejected() {
        when(badDebtWriteOffMapper.selectById("wo-1")).thenReturn(
                existingWriteOff("wo-1", BadDebtWriteOffStatus.PENDING));

        BadDebtWriteOff writeOff = service.reject("wo-1", "证据不足", "u-4");

        assertEquals(BadDebtWriteOffStatus.REJECTED.getCode(), writeOff.getStatus());
        assertEquals("证据不足", writeOff.getRejectReason());
        assertEquals("u-4", writeOff.getRejectedBy());
        assertNotNull(writeOff.getRejectedAt());
    }

    @Test
    void rejectFromWrittenOffIsRejected() {
        when(badDebtWriteOffMapper.selectById("wo-1")).thenReturn(
                existingWriteOff("wo-1", BadDebtWriteOffStatus.WRITTEN_OFF));

        assertThrows(ServiceException.class, () -> service.reject("wo-1", "证据不足", "u-4"));
        verify(badDebtWriteOffMapper, never()).updateById(any(BadDebtWriteOff.class));
    }

    @Test
    void getByIdDeniesForeignTenant() {
        BadDebtWriteOff writeOff = existingWriteOff("wo-1", BadDebtWriteOffStatus.PENDING);
        writeOff.setTenantId(FOREIGN_TENANT);
        when(badDebtWriteOffMapper.selectById("wo-1")).thenReturn(writeOff);

        assertThrows(AccessDeniedException.class, () -> service.getById("wo-1"));
    }

    @Test
    void getByIdRejectsMissingRecord() {
        when(badDebtWriteOffMapper.selectById("wo-404")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.getById("wo-404"));
    }

    @Test
    void applyCanTargetInvoiceOnly() {
        when(invoiceMapper.selectById("inv-1")).thenReturn(new Invoice()
                .setId("inv-1").setTenantId(TENANT.toString()).setDeleted(false));
        when(badDebtWriteOffMapper.selectList(any())).thenReturn(List.of());
        when(badDebtWriteOffMapper.insert(any(BadDebtWriteOff.class))).thenReturn(1);

        BadDebtWriteOff writeOff = service.apply(request(null, "inv-1", "50.00", "2026-10"));

        assertEquals("inv-1", writeOff.getInvoiceId());
        assertEquals(null, writeOff.getSettlementId());
        verify(settlementOrderMapper, never()).selectById(anyString());
    }

    private CreateBadDebtWriteOffRequest request(String settlementId, String invoiceId,
                                                 String amount, String periodCode) {
        CreateBadDebtWriteOffRequest request = new CreateBadDebtWriteOffRequest();
        request.setSettlementId(settlementId);
        request.setInvoiceId(invoiceId);
        request.setAmount(new BigDecimal(amount));
        request.setPeriodCode(periodCode);
        request.setReason("客户破产，确认无法收回");
        request.setApplicantId("u-1");
        request.setApplicantName("申请人");
        return request;
    }

    private void stubSettlement(String tenantId) {
        when(settlementOrderMapper.selectById("st-1")).thenReturn(new SettlementOrder()
                .setId("st-1").setTenantId(tenantId).setDeleted(false));
    }

    private BadDebtWriteOff existingWriteOff(String id, BadDebtWriteOffStatus status) {
        return new BadDebtWriteOff()
                .setId(id)
                .setTenantId(TENANT.toString())
                .setWriteOffNo("BD20261001" + id)
                .setSettlementId("st-1")
                .setAmount(new BigDecimal("100.00"))
                .setPeriodCode("2026-10")
                .setStatus(status.getCode())
                .setDeleted(false);
    }

    private ServiceException closedPeriod() {
        return new ServiceException(ResultCode.CONFLICT.getCode(), HttpStatus.CONFLICT,
                "账期 2026-10 已关账，禁止写入单据");
    }
}
