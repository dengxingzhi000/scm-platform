package com.cognora.scm.supplier.service.impl;

import com.cognora.platform.common.status.StatusValidator;
import com.cognora.scm.supplier.domain.entity.SupSettlement;
import com.cognora.scm.supplier.mapper.SupSettlementMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SupSettlementServiceImplTest {

    private SupSettlementServiceImpl newService(SupSettlementMapper mapper, StatusValidator validator) {
        SupSettlementServiceImpl service = new SupSettlementServiceImpl(validator);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        return service;
    }

    @Test
    void updateShouldRejectIllegalTransitionUsingCurrentAndTargetStatus() {
        SupSettlementMapper mapper = mock(SupSettlementMapper.class);
        StatusValidator validator = mock(StatusValidator.class);
        SupSettlementServiceImpl service = newService(mapper, validator);
        UUID id = UUID.randomUUID();
        SupSettlement existing = new SupSettlement();
        existing.setId(id);
        existing.setStatus(0);
        when(mapper.selectById(id.toString())).thenReturn(existing);
        doThrow(new IllegalStateException("非法状态流转: SUPPLIER_SETTLEMENT DRAFT -> PARTIAL_PAID"))
                .when(validator).validateTransition("SUPPLIER_SETTLEMENT", "DRAFT", "PARTIAL_PAID");

        SupSettlement request = new SupSettlement();
        request.setStatus(2);

        assertThrows(IllegalStateException.class, () -> service.updateSettlement(id.toString(), request));
        verify(validator).validateTransition("SUPPLIER_SETTLEMENT", "DRAFT", "PARTIAL_PAID");
        verify(mapper, never()).updateById(request);
    }

    @Test
    void updateShouldAllowLegalTransition() {
        SupSettlementMapper mapper = mock(SupSettlementMapper.class);
        StatusValidator validator = mock(StatusValidator.class);
        SupSettlementServiceImpl service = newService(mapper, validator);
        UUID id = UUID.randomUUID();
        SupSettlement existing = new SupSettlement();
        existing.setId(id);
        existing.setStatus(0);
        SupSettlement reloaded = new SupSettlement();
        reloaded.setId(id);
        reloaded.setStatus(1);
        when(mapper.selectById(id.toString())).thenReturn(existing, reloaded);
        doNothing().when(validator).validateTransition("SUPPLIER_SETTLEMENT", "DRAFT", "CONFIRMED");

        SupSettlement request = new SupSettlement();
        request.setStatus(1);
        SupSettlement result = service.updateSettlement(id.toString(), request);

        assertEquals(reloaded, result);
        assertEquals(id, request.getId());
        assertNotNull(request.getUpdateTime());
        verify(mapper).updateById(request);
    }

    @Test
    void updateWithoutStatusShouldSkipTransitionCheck() {
        SupSettlementMapper mapper = mock(SupSettlementMapper.class);
        StatusValidator validator = mock(StatusValidator.class);
        SupSettlementServiceImpl service = newService(mapper, validator);
        UUID id = UUID.randomUUID();
        SupSettlement existing = new SupSettlement();
        existing.setId(id);
        existing.setStatus(1);
        when(mapper.selectById(id.toString())).thenReturn(existing, existing);

        SupSettlement request = new SupSettlement();
        request.setRemark("only remark");
        service.updateSettlement(id.toString(), request);

        verifyNoInteractions(validator);
        verify(mapper).updateById(request);
    }

    @Test
    void updateWithSameStatusShouldSkipTransitionCheck() {
        SupSettlementMapper mapper = mock(SupSettlementMapper.class);
        StatusValidator validator = mock(StatusValidator.class);
        SupSettlementServiceImpl service = newService(mapper, validator);
        UUID id = UUID.randomUUID();
        SupSettlement existing = new SupSettlement();
        existing.setId(id);
        existing.setStatus(1);
        when(mapper.selectById(id.toString())).thenReturn(existing, existing);

        SupSettlement request = new SupSettlement();
        request.setStatus(1);
        service.updateSettlement(id.toString(), request);

        verifyNoInteractions(validator);
        verify(mapper).updateById(request);
    }

    @Test
    void updateShouldReturnNullWhenSettlementMissing() {
        SupSettlementMapper mapper = mock(SupSettlementMapper.class);
        StatusValidator validator = mock(StatusValidator.class);
        SupSettlementServiceImpl service = newService(mapper, validator);

        SupSettlement result = service.updateSettlement(UUID.randomUUID().toString(), new SupSettlement());

        assertNull(result);
        verifyNoInteractions(validator);
    }

    @Test
    void confirmShouldValidateTransitionFromActualStatus() {
        SupSettlementMapper mapper = mock(SupSettlementMapper.class);
        StatusValidator validator = mock(StatusValidator.class);
        SupSettlementServiceImpl service = newService(mapper, validator);
        UUID id = UUID.randomUUID();
        SupSettlement existing = new SupSettlement();
        existing.setId(id);
        existing.setStatus(2);
        when(mapper.selectById(id.toString())).thenReturn(existing);
        doThrow(new IllegalStateException("非法状态流转: SUPPLIER_SETTLEMENT PARTIAL_PAID -> CONFIRMED"))
                .when(validator).validateTransition("SUPPLIER_SETTLEMENT", "PARTIAL_PAID", "CONFIRMED");

        assertThrows(IllegalStateException.class,
                () -> service.confirm(id.toString(), "approver-1", "张三"));
        verify(validator).validateTransition("SUPPLIER_SETTLEMENT", "PARTIAL_PAID", "CONFIRMED");
        verify(mapper, never()).updateById(org.mockito.ArgumentMatchers.any(SupSettlement.class));
    }

    @Test
    void confirmShouldConfirmFromDraft() {
        SupSettlementMapper mapper = mock(SupSettlementMapper.class);
        StatusValidator validator = mock(StatusValidator.class);
        SupSettlementServiceImpl service = newService(mapper, validator);
        UUID id = UUID.randomUUID();
        SupSettlement existing = new SupSettlement();
        existing.setId(id);
        existing.setStatus(0);
        when(mapper.selectById(id.toString())).thenReturn(existing);
        doNothing().when(validator).validateTransition("SUPPLIER_SETTLEMENT", "DRAFT", "CONFIRMED");
        when(mapper.updateById(existing)).thenReturn(1);

        boolean confirmed = service.confirm(id.toString(), "approver-1", "张三");

        assertTrue(confirmed);
        assertEquals(1, existing.getStatus());
        assertEquals("approver-1", existing.getApproverId());
        assertEquals("张三", existing.getApproverName());
        assertNotNull(existing.getApprovedAt());
        assertNotNull(existing.getUpdateTime());
        verify(validator).validateTransition("SUPPLIER_SETTLEMENT", "DRAFT", "CONFIRMED");
    }

    @Test
    void confirmShouldSkipValidationWhenAlreadyConfirmed() {
        SupSettlementMapper mapper = mock(SupSettlementMapper.class);
        StatusValidator validator = mock(StatusValidator.class);
        SupSettlementServiceImpl service = newService(mapper, validator);
        UUID id = UUID.randomUUID();
        SupSettlement existing = new SupSettlement();
        existing.setId(id);
        existing.setStatus(1);
        when(mapper.selectById(id.toString())).thenReturn(existing);
        when(mapper.updateById(existing)).thenReturn(1);

        boolean confirmed = service.confirm(id.toString(), "approver-2", "李四");

        assertTrue(confirmed);
        verifyNoInteractions(validator);
    }

    @Test
    void markAsPaidShouldValidateActualStatusAndSetFullyPaid() {
        SupSettlementMapper mapper = mock(SupSettlementMapper.class);
        StatusValidator validator = mock(StatusValidator.class);
        SupSettlementServiceImpl service = newService(mapper, validator);
        UUID id = UUID.randomUUID();
        SupSettlement existing = new SupSettlement();
        existing.setId(id);
        existing.setStatus(2);
        existing.setActualAmount(new BigDecimal("100.00"));
        when(mapper.selectById(id.toString())).thenReturn(existing);
        doNothing().when(validator).validateTransition("SUPPLIER_SETTLEMENT", "PARTIAL_PAID", "FULLY_PAID");
        when(mapper.updateById(existing)).thenReturn(1);

        boolean paid = service.markAsPaid(id.toString(), "user-1");

        assertTrue(paid);
        assertEquals(3, existing.getStatus());
        assertEquals(new BigDecimal("100.00"), existing.getPaymentAmount());
        assertEquals("user-1", existing.getUpdateBy());
        assertNotNull(existing.getUpdateTime());
        verify(validator).validateTransition("SUPPLIER_SETTLEMENT", "PARTIAL_PAID", "FULLY_PAID");
    }

    @Test
    void markAsPaidShouldRejectDraftStatus() {
        SupSettlementMapper mapper = mock(SupSettlementMapper.class);
        StatusValidator validator = mock(StatusValidator.class);
        SupSettlementServiceImpl service = newService(mapper, validator);
        UUID id = UUID.randomUUID();
        SupSettlement existing = new SupSettlement();
        existing.setId(id);
        existing.setStatus(0);
        when(mapper.selectById(id.toString())).thenReturn(existing);
        doThrow(new IllegalStateException("非法状态流转: SUPPLIER_SETTLEMENT DRAFT -> FULLY_PAID"))
                .when(validator).validateTransition("SUPPLIER_SETTLEMENT", "DRAFT", "FULLY_PAID");

        assertThrows(IllegalStateException.class, () -> service.markAsPaid(id.toString(), "user-1"));
        verify(validator).validateTransition("SUPPLIER_SETTLEMENT", "DRAFT", "FULLY_PAID");
        verify(mapper, never()).updateById(org.mockito.ArgumentMatchers.any(SupSettlement.class));
    }

    @Test
    void createShouldInitializeDraftAndTimestamps() {
        SupSettlementMapper mapper = mock(SupSettlementMapper.class);
        SupSettlementServiceImpl service = newService(mapper, mock(StatusValidator.class));

        SupSettlement settlement = new SupSettlement();
        SupSettlement created = service.createSettlement(settlement);

        assertNotNull(created.getId());
        assertEquals(0, created.getStatus());
        assertFalse(created.getDeleted());
        assertNotNull(created.getCreateTime());
        assertNotNull(created.getUpdateTime());
        verify(mapper).insert(settlement);
    }

    @Test
    void deleteShouldReturnFalseWhenMissing() {
        SupSettlementMapper mapper = mock(SupSettlementMapper.class);
        SupSettlementServiceImpl service = newService(mapper, mock(StatusValidator.class));

        boolean deleted = service.deleteSettlement(UUID.randomUUID().toString());

        assertFalse(deleted);
        verify(mapper, never()).updateById(org.mockito.ArgumentMatchers.any(SupSettlement.class));
    }
}
