package com.cognora.scm.finance.service.query;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.scm.finance.config.CloseCriteriaProperties;
import com.cognora.scm.finance.domain.dto.CloseInclusionSummary;
import com.cognora.scm.finance.domain.entity.AccountingPeriod;
import com.cognora.scm.finance.domain.entity.Invoice;
import com.cognora.scm.finance.domain.entity.SettlementItem;
import com.cognora.scm.finance.domain.enums.CloseDateBasis;
import com.cognora.scm.finance.domain.enums.InvoiceStatus;
import com.cognora.scm.finance.mapper.AccountingPeriodMapper;
import com.cognora.scm.finance.mapper.InvoiceMapper;
import com.cognora.scm.finance.mapper.SettlementItemMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 结账口径边界：业务日期 vs 创建时间、含未开票已发货、排除已驳回。
 */
@ExtendWith(MockitoExtension.class)
class PeriodCloseInclusionQueryServiceTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    private static final String PERIOD_ID = "ap-1";

    @Mock
    private AccountingPeriodMapper accountingPeriodMapper;

    @Mock
    private SettlementItemMapper settlementItemMapper;

    @Mock
    private InvoiceMapper invoiceMapper;

    private CloseCriteriaProperties properties;

    private PeriodCloseInclusionQueryService service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Invoice.class);
        TableInfoHelper.initTableInfo(assistant, SettlementItem.class);
        TenantContextHolder.setTenantId(TENANT);
        properties = new CloseCriteriaProperties();
        service = new PeriodCloseInclusionQueryService(
                accountingPeriodMapper, settlementItemMapper, invoiceMapper, properties);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void businessDateBasisIncludesItemWhoseCreateTimeIsOutsidePeriod() {
        stubPeriod(TENANT.toString());
        when(settlementItemMapper.selectList(any()))
                .thenReturn(List.of(item("si-1", "SETTLEMENT", "D-1",
                        LocalDate.of(2026, 10, 15), LocalDateTime.of(2026, 11, 20, 10, 0), "100.00", "st-1")));
        when(invoiceMapper.selectList(any())).thenReturn(List.of());

        CloseInclusionSummary summary = service.preview(PERIOD_ID);

        assertEquals(1, summary.getSettlementItemCount());
        assertEquals("BUSINESS_DATE", summary.getDateBasis());
        assertEquals(LocalDate.of(2026, 10, 15), summary.getDocuments().get(0).getBusinessDate());
    }

    @Test
    void createTimeBasisExcludesItemWhoseCreateTimeIsOutsidePeriod() {
        properties.setDateBasis(CloseDateBasis.CREATE_TIME);
        stubPeriod(TENANT.toString());
        when(settlementItemMapper.selectList(any()))
                .thenReturn(List.of(item("si-1", "SETTLEMENT", "D-1",
                        LocalDate.of(2026, 10, 15), LocalDateTime.of(2026, 9, 15, 10, 0), "100.00", "st-1")));
        when(invoiceMapper.selectList(any())).thenReturn(List.of());

        CloseInclusionSummary summary = service.preview(PERIOD_ID);

        assertEquals(0, summary.getSettlementItemCount());
    }

    @Test
    void shippedUnbilledIsIncludedWhenEnabled() {
        stubPeriod(TENANT.toString());
        when(settlementItemMapper.selectList(any()))
                .thenReturn(List.of(item("si-1", "SHIPMENT", "SHP-1",
                        LocalDate.of(2026, 10, 15), LocalDateTime.of(2026, 10, 15, 10, 0), "1800.00", null)));
        when(invoiceMapper.selectList(any())).thenReturn(List.of());

        CloseInclusionSummary summary = service.preview(PERIOD_ID);

        assertEquals(1, summary.getSettlementItemCount());
        assertEquals(1, summary.getShippedUnbilledCount());
        assertEquals("SHIPPED_UNBILLED", summary.getDocuments().get(0).getCategory());
        assertEquals(new BigDecimal("1800.00"), summary.getShippedUnbilledAmount());
    }

    @Test
    void shippedUnbilledIsExcludedWhenDisabled() {
        properties.setIncludeShippedUnbilled(false);
        stubPeriod(TENANT.toString());
        when(settlementItemMapper.selectList(any()))
                .thenReturn(List.of(item("si-1", "SHIPMENT", "SHP-1",
                        LocalDate.of(2026, 10, 15), LocalDateTime.of(2026, 10, 15, 10, 0), "1800.00", null)));
        when(invoiceMapper.selectList(any())).thenReturn(List.of());

        CloseInclusionSummary summary = service.preview(PERIOD_ID);

        assertEquals(0, summary.getSettlementItemCount());
        assertEquals(0, summary.getShippedUnbilledCount());
    }

    @Test
    void billedShipmentIsNotCountedAsUnbilled() {
        stubPeriod(TENANT.toString());
        when(settlementItemMapper.selectList(any()))
                .thenReturn(List.of(item("si-1", "SHIPMENT", "SHP-1",
                        LocalDate.of(2026, 10, 15), LocalDateTime.of(2026, 10, 15, 10, 0), "1800.00", "st-1")));
        Invoice invoice = invoice("INV-1", LocalDate.of(2026, 10, 20), InvoiceStatus.ISSUED.getCode(),
                "28900.00", "st-1");
        when(invoiceMapper.selectList(any())).thenReturn(List.of(invoice));

        CloseInclusionSummary summary = service.preview(PERIOD_ID);

        assertEquals(1, summary.getSettlementItemCount());
        assertEquals(0, summary.getShippedUnbilledCount());
        assertEquals("SETTLEMENT_ITEM", summary.getDocuments().get(0).getCategory());
    }

    @Test
    void rejectedAndRedFlushedInvoicesAreExcluded() {
        stubPeriod(TENANT.toString());
        when(settlementItemMapper.selectList(any())).thenReturn(List.of());
        when(invoiceMapper.selectList(any())).thenReturn(List.of(
                invoice("INV-OK", LocalDate.of(2026, 10, 10), InvoiceStatus.ISSUED.getCode(), "100.00", null),
                invoice("INV-REJECT", LocalDate.of(2026, 10, 11), InvoiceStatus.VOIDED.getCode(), "200.00", null),
                invoice("INV-RED", LocalDate.of(2026, 10, 12), InvoiceStatus.RED_FLUSHED.getCode(), "300.00", null)));

        CloseInclusionSummary summary = service.preview(PERIOD_ID);

        assertEquals(1, summary.getInvoiceCount());
        assertEquals(new BigDecimal("100.00"), summary.getInvoiceAmount());
    }

    @Test
    void rejectedInvoiceIsKeptWhenExcludeRejectedIsDisabled() {
        properties.setExcludeRejected(false);
        stubPeriod(TENANT.toString());
        when(settlementItemMapper.selectList(any())).thenReturn(List.of());
        when(invoiceMapper.selectList(any())).thenReturn(List.of(
                invoice("INV-OK", LocalDate.of(2026, 10, 10), InvoiceStatus.ISSUED.getCode(), "100.00", null),
                invoice("INV-REJECT", LocalDate.of(2026, 10, 11), InvoiceStatus.VOIDED.getCode(), "200.00", null)));
        CloseInclusionSummary summary = service.preview(PERIOD_ID);

        // 红冲始终排除；关闭 exclude-rejected 后状态 3 保留。
        assertEquals(2, summary.getInvoiceCount());
        assertEquals(new BigDecimal("300.00"), summary.getInvoiceAmount());
    }

    @Test
    void foreignTenantPeriodShouldBeDenied() {
        stubPeriod("019a0aee-0000-7000-8000-000000000099");

        assertThrows(AccessDeniedException.class, () -> service.preview(PERIOD_ID));
    }

    @Test
    void unknownPeriodShouldFail() {
        when(accountingPeriodMapper.selectById(PERIOD_ID)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.preview(PERIOD_ID));
    }

    private void stubPeriod(String tenantId) {
        AccountingPeriod period = new AccountingPeriod();
        period.setId(PERIOD_ID);
        period.setTenantId(tenantId);
        period.setPeriodCode("2026-10");
        period.setStartDate(LocalDate.of(2026, 10, 1));
        period.setEndDate(LocalDate.of(2026, 10, 31));
        period.setDeleted(false);
        when(accountingPeriodMapper.selectById(PERIOD_ID)).thenReturn(period);
    }

    private SettlementItem item(String id, String documentType, String documentNo,
                                LocalDate documentDate, LocalDateTime createTime,
                                String amount, String settlementId) {
        SettlementItem item = new SettlementItem();
        item.setId(id);
        item.setTenantId(TENANT.toString());
        item.setDocumentType(documentType);
        item.setDocumentNo(documentNo);
        item.setDocumentDate(documentDate);
        item.setCreateTime(createTime);
        item.setActualAmount(new BigDecimal(amount));
        item.setSettlementId(settlementId);
        return item;
    }

    private Invoice invoice(String invoiceNo, LocalDate invoiceDate, int status,
                            String amount, String settlementId) {
        Invoice invoice = new Invoice();
        invoice.setId("inv-" + invoiceNo);
        invoice.setTenantId(TENANT.toString());
        invoice.setInvoiceNo(invoiceNo);
        invoice.setInvoiceDate(invoiceDate);
        invoice.setStatus(status);
        invoice.setTotalAmount(new BigDecimal(amount));
        invoice.setSettlementId(settlementId);
        invoice.setDeleted(false);
        return invoice;
    }
}
