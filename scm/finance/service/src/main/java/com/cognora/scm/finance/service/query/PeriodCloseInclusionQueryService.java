package com.cognora.scm.finance.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cognora.scm.finance.config.CloseCriteriaProperties;
import com.cognora.scm.finance.domain.dto.CloseIncludedDocument;
import com.cognora.scm.finance.domain.dto.CloseInclusionSummary;
import com.cognora.scm.finance.domain.entity.AccountingPeriod;
import com.cognora.scm.finance.domain.entity.Invoice;
import com.cognora.scm.finance.domain.entity.SettlementItem;
import com.cognora.scm.finance.domain.enums.CloseDateBasis;
import com.cognora.scm.finance.domain.enums.InvoiceStatus;
import com.cognora.scm.finance.mapper.AccountingPeriodMapper;
import com.cognora.scm.finance.mapper.InvoiceMapper;
import com.cognora.scm.finance.mapper.SettlementItemMapper;
import com.cognora.scm.finance.service.support.FinanceTenantGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 结账「本期纳入单据」预览。
 *
 * <p>默认口径（见 docs/guides/FINANCE_PERIOD_CLOSE_RULES.md）：</p>
 * <ul>
 *   <li>结算明细按业务日期 {@code document_date}、发票按 {@code invoice_date}
 *       归属本期（可配置为 {@code create_time}）；</li>
 *   <li>已发货未开票明细默认纳入（{@code finance.close.include-shipped-unbilled}）；</li>
 *   <li>已驳回/作废发票默认排除（状态 3），红冲发票（状态 4）始终排除。</li>
 * </ul>
 *
 * <p>各类别金额分开列示，不跨类别求和，避免发票与结算明细重复计量。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PeriodCloseInclusionQueryService {

    private final AccountingPeriodMapper accountingPeriodMapper;

    private final SettlementItemMapper settlementItemMapper;

    private final InvoiceMapper invoiceMapper;

    private final CloseCriteriaProperties properties;

    public CloseInclusionSummary preview(String periodId) {
        if (!StringUtils.hasText(periodId)) {
            throw new IllegalArgumentException("账期 ID 不能为空");
        }
        AccountingPeriod period = accountingPeriodMapper.selectById(periodId);
        if (period == null || Boolean.TRUE.equals(period.getDeleted())) {
            throw new IllegalArgumentException("账期不存在: " + periodId);
        }
        FinanceTenantGuard.verifyOwnership(period.getTenantId());

        LocalDate start = period.getStartDate();
        LocalDate end = period.getEndDate();
        List<SettlementItem> items = settlementItemMapper.selectList(itemWrapper(start, end)).stream()
                .filter(item -> inPeriod(itemBusinessDate(item), item.getCreateTime(), start, end))
                .toList();
        List<Invoice> periodInvoices = invoiceMapper.selectList(invoicePeriodWrapper(start, end)).stream()
                .filter(invoice -> isInvoiceIncluded(invoice, start, end))
                .toList();

        Set<String> billedSettlementIds = resolveBilledSettlementIds(items);
        Set<String> shippedUnbilledIds = items.stream()
                .filter(this::isShippedDocumentType)
                .filter(item -> !StringUtils.hasText(item.getSettlementId())
                        || !billedSettlementIds.contains(item.getSettlementId()))
                .map(SettlementItem::getId)
                .collect(Collectors.toSet());

        List<SettlementItem> includedItems = properties.isIncludeShippedUnbilled()
                ? items
                : items.stream().filter(item -> !shippedUnbilledIds.contains(item.getId())).toList();

        List<CloseIncludedDocument> documents = new ArrayList<>();
        BigDecimal settlementAmount = BigDecimal.ZERO;
        BigDecimal shippedUnbilledAmount = BigDecimal.ZERO;
        int shippedUnbilledCount = 0;
        for (SettlementItem item : includedItems) {
            BigDecimal amount = itemAmount(item);
            settlementAmount = settlementAmount.add(amount);
            boolean shippedUnbilled = shippedUnbilledIds.contains(item.getId());
            if (shippedUnbilled) {
                shippedUnbilledCount++;
                shippedUnbilledAmount = shippedUnbilledAmount.add(amount);
            }
            documents.add(new CloseIncludedDocument(
                    shippedUnbilled ? "SHIPPED_UNBILLED" : "SETTLEMENT_ITEM",
                    item.getDocumentType(),
                    item.getDocumentNo(),
                    itemBusinessDate(item),
                    amount));
        }

        BigDecimal invoiceAmount = BigDecimal.ZERO;
        for (Invoice invoice : periodInvoices) {
            BigDecimal amount = invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount();
            invoiceAmount = invoiceAmount.add(amount);
            documents.add(new CloseIncludedDocument(
                    "INVOICE",
                    String.valueOf(invoice.getInvoiceType()),
                    invoice.getInvoiceNo(),
                    invoiceBusinessDate(invoice),
                    amount));
        }

        CloseInclusionSummary summary = new CloseInclusionSummary();
        summary.setPeriodId(period.getId());
        summary.setPeriodCode(period.getPeriodCode());
        summary.setPeriodStart(start);
        summary.setPeriodEnd(end);
        summary.setDateBasis(properties.getDateBasis().name());
        summary.setIncludeShippedUnbilled(properties.isIncludeShippedUnbilled());
        summary.setExcludeRejected(properties.isExcludeRejected());
        summary.setSettlementItemCount(includedItems.size());
        summary.setSettlementItemAmount(settlementAmount);
        summary.setInvoiceCount(periodInvoices.size());
        summary.setInvoiceAmount(invoiceAmount);
        summary.setShippedUnbilledCount(shippedUnbilledCount);
        summary.setShippedUnbilledAmount(shippedUnbilledAmount);
        summary.setDocuments(documents);
        log.info("结账纳入预览: period={}, items={}, invoices={}, shippedUnbilled={}",
                period.getPeriodCode(), includedItems.size(), periodInvoices.size(), shippedUnbilledCount);
        return summary;
    }

    /**
     * 已开票的结算单：只要存在未作废/未红冲的发票关联该 settlement_id，即视为已开票
     * （不受发票日期是否在本期影响）。
     */
    private Set<String> resolveBilledSettlementIds(List<SettlementItem> items) {
        Set<String> settlementIds = items.stream()
                .map(SettlementItem::getSettlementId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (settlementIds.isEmpty()) {
            return Set.of();
        }
        List<Invoice> activeInvoices = invoiceMapper.selectList(Wrappers.<Invoice>lambdaQuery()
                .in(Invoice::getSettlementId, settlementIds)
                .eq(Invoice::getDeleted, false)
                .ne(Invoice::getStatus, InvoiceStatus.VOIDED.getCode())
                .ne(Invoice::getStatus, InvoiceStatus.RED_FLUSHED.getCode()));
        Set<String> billed = new HashSet<>();
        for (Invoice invoice : activeInvoices) {
            if (StringUtils.hasText(invoice.getSettlementId())) {
                billed.add(invoice.getSettlementId());
            }
        }
        return billed;
    }

    private LambdaQueryWrapper<SettlementItem> itemWrapper(LocalDate start, LocalDate end) {
        LambdaQueryWrapper<SettlementItem> wrapper = Wrappers.lambdaQuery();
        if (properties.getDateBasis() == CloseDateBasis.CREATE_TIME) {
            wrapper.ge(SettlementItem::getCreateTime, start.atStartOfDay())
                    .lt(SettlementItem::getCreateTime, end.plusDays(1).atStartOfDay());
        } else {
            wrapper.ge(SettlementItem::getDocumentDate, start)
                    .le(SettlementItem::getDocumentDate, end);
        }
        return wrapper;
    }

    private LambdaQueryWrapper<Invoice> invoicePeriodWrapper(LocalDate start, LocalDate end) {
        LambdaQueryWrapper<Invoice> wrapper = Wrappers.<Invoice>lambdaQuery()
                .eq(Invoice::getDeleted, false)
                .ne(Invoice::getStatus, InvoiceStatus.RED_FLUSHED.getCode());
        if (properties.getDateBasis() == CloseDateBasis.CREATE_TIME) {
            wrapper.ge(Invoice::getCreateTime, start.atStartOfDay())
                    .lt(Invoice::getCreateTime, end.plusDays(1).atStartOfDay());
        } else {
            wrapper.ge(Invoice::getInvoiceDate, start)
                    .le(Invoice::getInvoiceDate, end);
        }
        if (properties.isExcludeRejected()) {
            wrapper.ne(Invoice::getStatus, InvoiceStatus.VOIDED.getCode());
        }
        return wrapper;
    }

    /**
     * 发票纳入判定：红冲始终排除；exclude-rejected=true 时再排除作废/驳回（状态 3）。
     */
    private boolean isInvoiceIncluded(Invoice invoice, LocalDate start, LocalDate end) {
        Integer status = invoice.getStatus();
        if (status != null && (status == InvoiceStatus.RED_FLUSHED.getCode()
                || (properties.isExcludeRejected() && status == InvoiceStatus.VOIDED.getCode()))) {
            return false;
        }
        return inPeriod(invoiceBusinessDate(invoice), invoice.getCreateTime(), start, end);
    }

    /**
     * 归属期判定：BUSINESS_DATE 用业务日期，CREATE_TIME 用创建时间。
     */
    private boolean inPeriod(LocalDate businessDate, LocalDateTime createTime,
                             LocalDate start, LocalDate end) {
        if (properties.getDateBasis() == CloseDateBasis.CREATE_TIME) {
            if (createTime == null) {
                return false;
            }
            LocalDate createdDate = createTime.toLocalDate();
            return !createdDate.isBefore(start) && !createdDate.isAfter(end);
        }
        return businessDate != null && !businessDate.isBefore(start) && !businessDate.isAfter(end);
    }

    private boolean isShippedDocumentType(SettlementItem item) {
        if (!StringUtils.hasText(item.getDocumentType())) {
            return false;
        }
        String type = item.getDocumentType().trim();
        return properties.getShippedUnbilledDocumentTypes().stream()
                .filter(StringUtils::hasText)
                .anyMatch(candidate -> candidate.trim().equalsIgnoreCase(type));
    }

    private static LocalDate itemBusinessDate(SettlementItem item) {
        return item.getDocumentDate() != null
                ? item.getDocumentDate()
                : item.getCreateTime() == null ? null : item.getCreateTime().toLocalDate();
    }

    private static LocalDate invoiceBusinessDate(Invoice invoice) {
        return invoice.getInvoiceDate() != null
                ? invoice.getInvoiceDate()
                : invoice.getCreateTime() == null ? null : invoice.getCreateTime().toLocalDate();
    }

    private static BigDecimal itemAmount(SettlementItem item) {
        if (item.getActualAmount() != null) {
            return item.getActualAmount();
        }
        return item.getAmount() == null ? BigDecimal.ZERO : item.getAmount();
    }
}
