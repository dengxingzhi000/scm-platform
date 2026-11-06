package com.cognora.scm.finance.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.scm.finance.domain.dto.CreateInvoiceRequest;
import com.cognora.scm.finance.domain.dto.TaxRateResolution;
import com.cognora.scm.finance.domain.dto.UpdateInvoiceRequest;
import com.cognora.scm.finance.domain.entity.Invoice;
import com.cognora.scm.finance.domain.enums.InvoiceStatus;
import com.cognora.scm.finance.domain.enums.TaxRateSource;
import com.cognora.scm.finance.mapper.InvoiceMapper;
import com.cognora.scm.finance.service.impl.InvoiceServiceImpl;
import com.cognora.scm.finance.service.support.AccountingPeriodGuard;
import com.cognora.scm.finance.service.support.TaxRateResolver;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    @Mock
    private InvoiceMapper invoiceMapper;

    private InvoiceServiceImpl invoiceService;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Invoice.class);
        TaxRateResolver taxRateResolver = org.mockito.Mockito.mock(TaxRateResolver.class,
                invocation -> new TaxRateResolution(
                        new BigDecimal("0.1300"), TaxRateSource.DEFAULT, null, null));
        invoiceService = new InvoiceServiceImpl(
                org.mockito.Mockito.mock(AccountingPeriodGuard.class), taxRateResolver);
        // MyBatis-Plus ServiceImpl 的 baseMapper 位于父类 CrudRepository，
        // Mockito @InjectMocks 不会注入该字段，因此按仓库既有模式显式挂载。
        ReflectionTestUtils.setField(invoiceService, "baseMapper", invoiceMapper);
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldCreateInvoice() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setInvoiceNo("INV-2026-001");
        request.setInvoiceType(1);
        request.setInvoiceKind(1);
        request.setPartyId("party-1");
        request.setPartyName("Acme");
        request.setAmount(new BigDecimal("100.00"));

        when(invoiceMapper.insert(any(Invoice.class))).thenReturn(1);

        Invoice result = invoiceService.create(request);

        assertNotNull(result.getId());
        assertEquals("INV-2026-001", result.getInvoiceNo());
        assertEquals(1, result.getInvoiceType());
        assertEquals(1, result.getInvoiceKind());
        assertEquals("party-1", result.getPartyId());
        assertEquals("Acme", result.getPartyName());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals(new BigDecimal("100.00"), result.getTotalAmount());
        assertEquals(TENANT.toString(), result.getTenantId());
        assertEquals("CNY", result.getCurrency());
        // 未显式传税率时由 TaxRateResolver 解析，并在响应里标明来源。
        assertEquals("0.1300", result.getTaxRate().toPlainString());
        assertEquals("DEFAULT", result.getTaxRateSource());
        assertEquals(InvoiceStatus.DRAFT.getCode(), result.getStatus());
        assertFalse(result.getDeleted());
        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());
        verify(invoiceMapper).insert(result);
    }

    @Test
    void createShouldComputeTaxInclusiveTotal() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setInvoiceNo("INV-2026-002");
        request.setInvoiceType(1);
        request.setInvoiceKind(1);
        request.setPartyId("party-1");
        request.setAmount(new BigDecimal("100.00"));
        request.setTaxAmount(new BigDecimal("13.00"));
        request.setTaxRate(new BigDecimal("0.13"));
        when(invoiceMapper.insert(any(Invoice.class))).thenReturn(1);

        Invoice result = invoiceService.create(request);

        assertEquals("100.00", result.getAmount().toPlainString());
        assertEquals("13.00", result.getTaxAmount().toPlainString());
        assertEquals("113.00", result.getTotalAmount().toPlainString());
        assertEquals("0.1300", result.getTaxRate().toPlainString());
    }

    @Test
    void createShouldRejectAmountWithThreeDecimals() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setInvoiceNo("INV-2026-003");
        request.setInvoiceType(1);
        request.setInvoiceKind(1);
        request.setPartyId("party-1");
        request.setAmount(new BigDecimal("100.005"));

        assertThrows(IllegalArgumentException.class, () -> invoiceService.create(request));
        verify(invoiceMapper, never()).insert(any(Invoice.class));
    }

    @Test
    void createShouldRejectTaxRateOutOfRange() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setInvoiceNo("INV-2026-004");
        request.setInvoiceType(1);
        request.setInvoiceKind(1);
        request.setPartyId("party-1");
        request.setAmount(new BigDecimal("100.00"));
        request.setTaxRate(new BigDecimal("1.5"));

        assertThrows(IllegalArgumentException.class, () -> invoiceService.create(request));
        verify(invoiceMapper, never()).insert(any(Invoice.class));
    }

    @Test
    void updateShouldRejectIssuedInvoice() {
        Invoice existing = invoice("invoice-1", InvoiceStatus.ISSUED);
        when(invoiceMapper.selectById("invoice-1")).thenReturn(existing);

        UpdateInvoiceRequest request = new UpdateInvoiceRequest();
        request.setAmount(new BigDecimal("120.00"));

        assertThrows(IllegalStateException.class, () -> invoiceService.update("invoice-1", request));
        verify(invoiceMapper, never()).updateById(any(Invoice.class));
    }

    @Test
    void shouldUpdateInvoice() {
        Invoice existing = invoice("invoice-1", InvoiceStatus.DRAFT);
        existing.setInvoiceNo("INV-2026-001");
        existing.setPartyName("Old Party");
        existing.setAmount(new BigDecimal("100.00"));

        UpdateInvoiceRequest request = new UpdateInvoiceRequest();
        request.setInvoiceNo("INV-2026-001-UPDATED");
        request.setPartyName("New Party");
        request.setAmount(new BigDecimal("120.00"));

        when(invoiceMapper.selectById("invoice-1")).thenReturn(existing);
        when(invoiceMapper.updateById(any(Invoice.class))).thenReturn(1);

        Invoice result = invoiceService.update("invoice-1", request);

        assertEquals("INV-2026-001-UPDATED", result.getInvoiceNo());
        assertEquals("New Party", result.getPartyName());
        assertEquals(new BigDecimal("120.00"), result.getAmount());
        assertNull(result.getPartyTaxNo());
        assertNotNull(result.getUpdateTime());
        verify(invoiceMapper).updateById(existing);
    }

    @Test
    void updateRejectsMissingInvoice() {
        when(invoiceMapper.selectById("missing")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> invoiceService.update("missing", new UpdateInvoiceRequest()));
        verify(invoiceMapper, never()).updateById(any(Invoice.class));
    }

    @Test
    void updateRejectsDeletedInvoice() {
        Invoice existing = invoice("invoice-1", InvoiceStatus.DRAFT);
        existing.setDeleted(true);
        when(invoiceMapper.selectById("invoice-1")).thenReturn(existing);

        assertThrows(IllegalArgumentException.class,
                () -> invoiceService.update("invoice-1", new UpdateInvoiceRequest()));
        verify(invoiceMapper, never()).updateById(any(Invoice.class));
    }

    @Test
    void shouldDeleteInvoice() {
        Invoice existing = invoice("invoice-1", InvoiceStatus.DRAFT);

        when(invoiceMapper.selectById("invoice-1")).thenReturn(existing);
        when(invoiceMapper.updateById(any(Invoice.class))).thenReturn(1);

        invoiceService.delete("invoice-1");

        assertTrue(existing.getDeleted());
        assertNotNull(existing.getUpdateTime());
        verify(invoiceMapper).updateById(existing);
    }

    @Test
    void deleteRejectsMissingInvoice() {
        when(invoiceMapper.selectById("missing")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> invoiceService.delete("missing"));
        verify(invoiceMapper, never()).updateById(any(Invoice.class));
    }

    @Test
    void shouldIssueInvoice() {
        Invoice existing = invoice("invoice-1", InvoiceStatus.DRAFT);

        when(invoiceMapper.selectById("invoice-1")).thenReturn(existing);
        when(invoiceMapper.updateById(any(Invoice.class))).thenReturn(1);

        Invoice result = invoiceService.issueInvoice("invoice-1", "John");

        assertEquals(InvoiceStatus.ISSUED.getCode(), result.getStatus());
        assertEquals("John", result.getIssuerName());
        assertEquals(LocalDate.now(), result.getIssueDate());
        assertNotNull(result.getUpdateTime());
        verify(invoiceMapper).updateById(existing);
    }

    @Test
    void shouldNotIssueNonDraftInvoice() {
        Invoice existing = invoice("invoice-1", InvoiceStatus.ISSUED);
        when(invoiceMapper.selectById("invoice-1")).thenReturn(existing);

        assertThrows(IllegalStateException.class,
                () -> invoiceService.issueInvoice("invoice-1", "John"));
        verify(invoiceMapper, never()).updateById(any(Invoice.class));
    }

    @Test
    void shouldNotIssueMissingInvoice() {
        when(invoiceMapper.selectById("missing")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> invoiceService.issueInvoice("missing", "John"));
        verify(invoiceMapper, never()).updateById(any(Invoice.class));
    }

    @Test
    void shouldVoidInvoice() {
        Invoice existing = invoice("invoice-1", InvoiceStatus.ISSUED);

        when(invoiceMapper.selectById("invoice-1")).thenReturn(existing);
        when(invoiceMapper.updateById(any(Invoice.class))).thenReturn(1);

        Invoice result = invoiceService.voidInvoice("invoice-1");

        assertEquals(InvoiceStatus.VOIDED.getCode(), result.getStatus());
        assertNotNull(result.getUpdateTime());
        verify(invoiceMapper).updateById(existing);
    }

    @Test
    void shouldNotVoidAlreadyVoidedInvoice() {
        Invoice existing = invoice("invoice-1", InvoiceStatus.VOIDED);
        when(invoiceMapper.selectById("invoice-1")).thenReturn(existing);

        assertThrows(IllegalStateException.class,
                () -> invoiceService.voidInvoice("invoice-1"));
        verify(invoiceMapper, never()).updateById(any(Invoice.class));
    }

    @Test
    void shouldNotVoidRedFlushedInvoice() {
        Invoice existing = invoice("invoice-1", InvoiceStatus.RED_FLUSHED);
        when(invoiceMapper.selectById("invoice-1")).thenReturn(existing);

        assertThrows(IllegalStateException.class,
                () -> invoiceService.voidInvoice("invoice-1"));
        verify(invoiceMapper, never()).updateById(any(Invoice.class));
    }

    @Test
    void shouldRedFlushInvoice() {
        Invoice existing = invoice("invoice-1", InvoiceStatus.ISSUED);

        when(invoiceMapper.selectById("invoice-1")).thenReturn(existing);
        when(invoiceMapper.updateById(any(Invoice.class))).thenReturn(1);

        Invoice result = invoiceService.redFlushInvoice("invoice-1");

        assertEquals(InvoiceStatus.RED_FLUSHED.getCode(), result.getStatus());
        assertNotNull(result.getUpdateTime());
        verify(invoiceMapper).updateById(existing);
    }

    @Test
    void shouldRedFlushMailedInvoice() {
        Invoice existing = invoice("invoice-1", InvoiceStatus.MAILED);

        when(invoiceMapper.selectById("invoice-1")).thenReturn(existing);
        when(invoiceMapper.updateById(any(Invoice.class))).thenReturn(1);

        Invoice result = invoiceService.redFlushInvoice("invoice-1");

        assertEquals(InvoiceStatus.RED_FLUSHED.getCode(), result.getStatus());
        verify(invoiceMapper).updateById(existing);
    }

    @Test
    void shouldNotRedFlushDraftInvoice() {
        Invoice existing = invoice("invoice-1", InvoiceStatus.DRAFT);
        when(invoiceMapper.selectById("invoice-1")).thenReturn(existing);

        assertThrows(IllegalStateException.class,
                () -> invoiceService.redFlushInvoice("invoice-1"));
        verify(invoiceMapper, never()).updateById(any(Invoice.class));
    }

    @Test
    void shouldPageByPartyIdWithFilter() {
        Page<Invoice> page = new Page<>(1, 10, 2);
        page.setRecords(List.of(invoice("invoice-1", InvoiceStatus.DRAFT),
                invoice("invoice-2", InvoiceStatus.ISSUED)));
        when(invoiceMapper.selectPage(any(), any())).thenReturn(page);

        Page<Invoice> result = invoiceService.pageByPartyId("party-1", 1, 10);

        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());
        verify(invoiceMapper).selectPage(any(), argThat(wrapper ->
                wrapper.getSqlSegment().contains("party_id")
                        && wrapper.getSqlSegment().contains("invoice_date")));
    }

    @Test
    void shouldPageWithoutPartyIdFilter() {
        Page<Invoice> page = new Page<>(1, 10, 0);
        page.setRecords(List.of());
        when(invoiceMapper.selectPage(any(), any())).thenReturn(page);

        Page<Invoice> result = invoiceService.pageByPartyId(null, 1, 10);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        verify(invoiceMapper).selectPage(any(), argThat(wrapper ->
                !wrapper.getSqlSegment().contains("party_id")
                        && wrapper.getSqlSegment().contains("invoice_date")));
    }

    private Invoice invoice(String id, InvoiceStatus status) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setStatus(status.getCode());
        invoice.setDeleted(false);
        return invoice;
    }
}