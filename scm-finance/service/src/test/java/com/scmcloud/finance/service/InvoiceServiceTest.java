package com.scmcloud.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.finance.domain.dto.CreateInvoiceRequest;
import com.scmcloud.finance.domain.dto.UpdateInvoiceRequest;
import com.scmcloud.finance.domain.entity.Invoice;
import com.scmcloud.finance.domain.enums.InvoiceStatus;
import com.scmcloud.finance.mapper.InvoiceMapper;
import com.scmcloud.finance.service.impl.InvoiceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceMapper invoiceMapper;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    @Test
    void shouldCreateInvoice() {
        // Given
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setInvoiceNo("INV-2026-001");
        request.setInvoiceType(1);
        request.setInvoiceKind(1);
        request.setPartyId("party-1");

        // When
        Invoice result = invoiceService.create(request);

        // Then
        assertNotNull(result);
        assertEquals("INV-2026-001", result.getInvoiceNo());
        assertEquals(InvoiceStatus.DRAFT.getCode(), result.getStatus());
        assertFalse(result.getDeleted());
        verify(invoiceMapper).insert(any());
    }

    @Test
    void shouldUpdateInvoice() {
        // Given
        String id = "invoice-1";
        Invoice existing = new Invoice();
        existing.setId(id);
        existing.setInvoiceNo("INV-2026-001");
        existing.setStatus(InvoiceStatus.DRAFT.getCode());

        UpdateInvoiceRequest request = new UpdateInvoiceRequest();
        request.setInvoiceNo("INV-2026-001-UPDATED");
        request.setPartyName("New Party");

        when(invoiceMapper.selectById(id)).thenReturn(existing);
        when(invoiceMapper.updateById(any())).thenReturn(1);

        // When
        Invoice result = invoiceService.update(id, request);

        // Then
        assertEquals("INV-2026-001-UPDATED", result.getInvoiceNo());
        assertEquals("New Party", result.getPartyName());
        verify(invoiceMapper).updateById(any());
    }

    @Test
    void shouldDeleteInvoice() {
        // Given
        String id = "invoice-1";
        Invoice existing = new Invoice();
        existing.setId(id);
        existing.setDeleted(false);

        when(invoiceMapper.selectById(id)).thenReturn(existing);
        when(invoiceMapper.updateById(any())).thenReturn(1);

        // When
        invoiceService.delete(id);

        // Then
        assertTrue(existing.getDeleted());
        verify(invoiceMapper).updateById(existing);
    }

    @Test
    void shouldIssueInvoice() {
        // Given
        String id = "invoice-1";
        Invoice existing = new Invoice();
        existing.setId(id);
        existing.setStatus(InvoiceStatus.DRAFT.getCode());

        when(invoiceMapper.selectById(id)).thenReturn(existing);
        when(invoiceMapper.updateById(any())).thenReturn(1);

        // When
        Invoice result = invoiceService.issueInvoice(id, "John");

        // Then
        assertEquals(InvoiceStatus.ISSUED.getCode(), result.getStatus());
        assertEquals("John", result.getIssuerName());
        assertNotNull(result.getIssueDate());
        verify(invoiceMapper).updateById(any());
    }

    @Test
    void shouldNotIssueNonDraftInvoice() {
        // Given
        String id = "invoice-1";
        Invoice existing = new Invoice();
        existing.setId(id);
        existing.setStatus(InvoiceStatus.ISSUED.getCode());

        when(invoiceMapper.selectById(id)).thenReturn(existing);

        // When & Then
        assertThrows(IllegalStateException.class,
                () -> invoiceService.issueInvoice(id, "John"));
    }

    @Test
    void shouldVoidInvoice() {
        // Given
        String id = "invoice-1";
        Invoice existing = new Invoice();
        existing.setId(id);
        existing.setStatus(InvoiceStatus.ISSUED.getCode());

        when(invoiceMapper.selectById(id)).thenReturn(existing);
        when(invoiceMapper.updateById(any())).thenReturn(1);

        // When
        Invoice result = invoiceService.voidInvoice(id);

        // Then
        assertEquals(InvoiceStatus.VOIDED.getCode(), result.getStatus());
        verify(invoiceMapper).updateById(any());
    }

    @Test
    void shouldNotVoidAlreadyVoidedInvoice() {
        // Given
        String id = "invoice-1";
        Invoice existing = new Invoice();
        existing.setId(id);
        existing.setStatus(InvoiceStatus.VOIDED.getCode());

        when(invoiceMapper.selectById(id)).thenReturn(existing);

        // When & Then
        assertThrows(IllegalStateException.class,
                () -> invoiceService.voidInvoice(id));
    }

    @Test
    void shouldRedFlushInvoice() {
        // Given
        String id = "invoice-1";
        Invoice existing = new Invoice();
        existing.setId(id);
        existing.setStatus(InvoiceStatus.ISSUED.getCode());

        when(invoiceMapper.selectById(id)).thenReturn(existing);
        when(invoiceMapper.updateById(any())).thenReturn(1);

        // When
        Invoice result = invoiceService.redFlushInvoice(id);

        // Then
        assertEquals(InvoiceStatus.RED_FLUSHED.getCode(), result.getStatus());
        verify(invoiceMapper).updateById(any());
    }

    @Test
    void shouldNotRedFlushDraftInvoice() {
        // Given
        String id = "invoice-1";
        Invoice existing = new Invoice();
        existing.setId(id);
        existing.setStatus(InvoiceStatus.DRAFT.getCode());

        when(invoiceMapper.selectById(id)).thenReturn(existing);

        // When & Then
        assertThrows(IllegalStateException.class,
                () -> invoiceService.redFlushInvoice(id));
    }

    @Test
    void shouldPageByPartyId() {
        // Given
        String partyId = "party-1";
        when(invoiceMapper.selectPage(any(), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>());

        // When
        Page<Invoice> result = invoiceService.pageByPartyId(partyId, 1, 10);

        // Then
        assertNotNull(result);
        verify(invoiceMapper).selectPage(any(), any(LambdaQueryWrapper.class));
    }
}
