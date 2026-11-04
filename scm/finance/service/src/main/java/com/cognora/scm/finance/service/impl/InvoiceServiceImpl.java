package com.cognora.scm.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.util.UUIDv7Util;
import com.cognora.scm.finance.domain.dto.CreateInvoiceRequest;
import com.cognora.scm.finance.domain.dto.UpdateInvoiceRequest;
import com.cognora.scm.finance.domain.entity.Invoice;
import com.cognora.scm.finance.domain.enums.InvoiceStatus;
import com.cognora.scm.finance.domain.support.FinanceAmount;
import com.cognora.scm.finance.mapper.InvoiceMapper;
import com.cognora.scm.finance.service.IInvoiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
public class InvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice> implements IInvoiceService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice create(CreateInvoiceRequest request) {
        log.info("Create invoice: invoiceNo={}", (request.getInvoiceNo() + "").replace("\r", "_").replace("\n", "_"));

        Invoice invoice = new Invoice();
        invoice.setId(UUIDv7Util.generateString());
        invoice.setInvoiceNo(request.getInvoiceNo());
        invoice.setInvoiceType(request.getInvoiceType());
        invoice.setInvoiceKind(request.getInvoiceKind());
        invoice.setPartyId(request.getPartyId());
        invoice.setPartyType(request.getPartyType());
        invoice.setPartyName(request.getPartyName());
        invoice.setPartyTaxNo(request.getPartyTaxNo());
        invoice.setInvoiceCode(request.getInvoiceCode());
        invoice.setInvoiceNumber(request.getInvoiceNumber());
        // 租户必须取自登录态，忽略请求体里的 tenantId。
        invoice.setTenantId(TenantContextHolder.getRequiredTenantId().toString());
        applyAmounts(invoice, request.getAmount(), request.getTaxAmount(), request.getTaxRate());
        invoice.setRelatedOrders(request.getRelatedOrders());
        invoice.setSettlementId(request.getSettlementId());
        invoice.setRemark(request.getRemark());
        invoice.setStatus(InvoiceStatus.DRAFT.getCode());
        invoice.setDeleted(false);
        invoice.setCreateTime(LocalDateTime.now());
        invoice.setUpdateTime(LocalDateTime.now());

        save(invoice);
        log.info("Invoice created: id={}, invoiceNo={}", (invoice.getId() + "").replace("\r", "_").replace("\n", "_"), (invoice.getInvoiceNo() + "").replace("\r", "_").replace("\n", "_"));
        return invoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice update(String id, UpdateInvoiceRequest request) {
        log.info("Update invoice: id={}", (id + "").replace("\r", "_").replace("\n", "_"));

        Invoice invoice = getById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("Invoice not found: " + id);
        }
        // 已开具 / 已作废 / 已红冲的发票是会计凭证，不允许再改任何字段。
        if (invoice.getStatus() != InvoiceStatus.DRAFT.getCode()) {
            throw new IllegalStateException("Only draft invoices can be updated, current status: "
                    + InvoiceStatus.fromCode(invoice.getStatus()).getDescription());
        }

        if (StringUtils.hasText(request.getInvoiceNo())) {
            invoice.setInvoiceNo(request.getInvoiceNo());
        }
        if (request.getInvoiceType() != null) {
            invoice.setInvoiceType(request.getInvoiceType());
        }
        if (request.getInvoiceKind() != null) {
            invoice.setInvoiceKind(request.getInvoiceKind());
        }
        if (StringUtils.hasText(request.getPartyType())) {
            invoice.setPartyType(request.getPartyType());
        }
        if (StringUtils.hasText(request.getPartyId())) {
            invoice.setPartyId(request.getPartyId());
        }
        if (StringUtils.hasText(request.getPartyName())) {
            invoice.setPartyName(request.getPartyName());
        }
        if (StringUtils.hasText(request.getPartyTaxNo())) {
            invoice.setPartyTaxNo(request.getPartyTaxNo());
        }
        if (StringUtils.hasText(request.getInvoiceCode())) {
            invoice.setInvoiceCode(request.getInvoiceCode());
        }
        if (StringUtils.hasText(request.getInvoiceNumber())) {
            invoice.setInvoiceNumber(request.getInvoiceNumber());
        }
        if (request.getAmount() != null || request.getTaxAmount() != null || request.getTaxRate() != null) {
            applyAmounts(invoice,
                    request.getAmount() != null ? request.getAmount() : invoice.getAmount(),
                    request.getTaxAmount() != null ? request.getTaxAmount() : invoice.getTaxAmount(),
                    request.getTaxRate() != null ? request.getTaxRate() : invoice.getTaxRate());
        }
        if (StringUtils.hasText(request.getRelatedOrders())) {
            invoice.setRelatedOrders(request.getRelatedOrders());
        }
        if (StringUtils.hasText(request.getSettlementId())) {
            invoice.setSettlementId(request.getSettlementId());
        }
        if (StringUtils.hasText(request.getRemark())) {
            invoice.setRemark(request.getRemark());
        }

        invoice.setUpdateTime(LocalDateTime.now());
        updateById(invoice);
        log.info("Invoice updated: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return invoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        log.info("Delete invoice: id={}", (id + "").replace("\r", "_").replace("\n", "_"));

        Invoice invoice = getById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("Invoice not found: " + id);
        }

        invoice.setDeleted(true);
        invoice.setUpdateTime(LocalDateTime.now());
        updateById(invoice);
        log.info("Invoice deleted: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    public Page<Invoice> pageByPartyId(String partyId, int pageNum, int pageSize) {
        log.debug("Page invoices by party: partyId={}, page={}, size={}", (partyId + "").replace("\r", "_").replace("\n", "_"), (pageNum + "").replace("\r", "_").replace("\n", "_"), (pageSize + "").replace("\r", "_").replace("\n", "_"));

        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(partyId), Invoice::getPartyId, partyId)
                .orderByDesc(Invoice::getInvoiceDate);

        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice issueInvoice(String id, String issuerName) {
        log.info("Issue invoice: id={}, issuer={}", (id + "").replace("\r", "_").replace("\n", "_"), (issuerName + "").replace("\r", "_").replace("\n", "_"));

        Invoice invoice = getById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("Invoice not found: " + id);
        }
        if (invoice.getStatus() != InvoiceStatus.DRAFT.getCode()) {
            throw new IllegalStateException("Only draft invoices can be issued, current status: " + InvoiceStatus.fromCode(invoice.getStatus()).getDescription());
        }

        invoice.setStatus(InvoiceStatus.ISSUED.getCode());
        invoice.setIssuerName(issuerName);
        invoice.setIssueDate(LocalDate.now());
        invoice.setUpdateTime(LocalDateTime.now());

        updateById(invoice);
        log.info("Invoice issued: id={}, invoiceNo={}", (id + "").replace("\r", "_").replace("\n", "_"), (invoice.getInvoiceNo() + "").replace("\r", "_").replace("\n", "_"));
        return invoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice voidInvoice(String id) {
        log.info("Void invoice: id={}", (id + "").replace("\r", "_").replace("\n", "_"));

        Invoice invoice = getById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("Invoice not found: " + id);
        }
        if (invoice.getStatus() == InvoiceStatus.VOIDED.getCode()
                || invoice.getStatus() == InvoiceStatus.RED_FLUSHED.getCode()) {
            throw new IllegalStateException("Invoice already voided or red-flushed, cannot void again");
        }

        invoice.setStatus(InvoiceStatus.VOIDED.getCode());
        invoice.setUpdateTime(LocalDateTime.now());

        updateById(invoice);
        log.info("Invoice voided: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return invoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice redFlushInvoice(String id) {
        log.info("Red-flush invoice: id={}", (id + "").replace("\r", "_").replace("\n", "_"));

        Invoice invoice = getById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("Invoice not found: " + id);
        }
        if (invoice.getStatus() != InvoiceStatus.ISSUED.getCode()
                && invoice.getStatus() != InvoiceStatus.MAILED.getCode()) {
            throw new IllegalStateException("Only issued or mailed invoices can be red-flushed, current status: " + InvoiceStatus.fromCode(invoice.getStatus()).getDescription());
        }

        invoice.setStatus(InvoiceStatus.RED_FLUSHED.getCode());
        invoice.setUpdateTime(LocalDateTime.now());

        updateById(invoice);
        log.info("Invoice red-flushed: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return invoice;
    }

    /**
     * 金额统一到两位小数，并按「价税合计 = 金额 + 税额」计算 total_amount。
     *
     * <p>invoice.total_amount 是 NOT NULL 列；旧实现只写 amount/taxAmount，
     * 插入时会直接触发非空约束。</p>
     */
    private void applyAmounts(Invoice invoice, BigDecimal amount, BigDecimal taxAmount, BigDecimal taxRate) {
        BigDecimal normalizedAmount = FinanceAmount.positive(amount, "发票金额");
        BigDecimal normalizedTax = FinanceAmount.optionalOrZero(taxAmount, "税额");
        BigDecimal normalizedTotal = FinanceAmount.normalize(normalizedAmount.add(normalizedTax), "价税合计");
        invoice.setAmount(normalizedAmount);
        invoice.setTaxAmount(normalizedTax);
        invoice.setTotalAmount(normalizedTotal);
        invoice.setTaxRate(FinanceAmount.rate(taxRate, "税率"));
    }
}
