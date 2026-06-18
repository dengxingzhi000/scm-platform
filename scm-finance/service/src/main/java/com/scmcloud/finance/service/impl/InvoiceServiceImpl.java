package com.scmcloud.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.finance.domain.dto.CreateInvoiceRequest;
import com.scmcloud.finance.domain.dto.UpdateInvoiceRequest;
import com.scmcloud.finance.domain.entity.Invoice;
import com.scmcloud.finance.domain.enums.InvoiceStatus;
import com.scmcloud.finance.mapper.InvoiceMapper;
import com.scmcloud.finance.service.IInvoiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
public class InvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice> implements IInvoiceService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice create(CreateInvoiceRequest request) {
        log.info("Create invoice: invoiceNo={}", request.getInvoiceNo());

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
        invoice.setAmount(request.getAmount());
        invoice.setTaxAmount(request.getTaxAmount());
        invoice.setTaxRate(request.getTaxRate());
        invoice.setRelatedOrders(request.getRelatedOrders());
        invoice.setSettlementId(request.getSettlementId());
        invoice.setRemark(request.getRemark());
        invoice.setStatus(InvoiceStatus.DRAFT.getCode());
        invoice.setDeleted(false);
        invoice.setCreateTime(LocalDateTime.now());
        invoice.setUpdateTime(LocalDateTime.now());

        save(invoice);
        log.info("Invoice created: id={}, invoiceNo={}", invoice.getId(), invoice.getInvoiceNo());
        return invoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice update(String id, UpdateInvoiceRequest request) {
        log.info("Update invoice: id={}", id);

        Invoice invoice = getById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("Invoice not found: " + id);
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
        if (request.getAmount() != null) {
            invoice.setAmount(request.getAmount());
        }
        if (request.getTaxAmount() != null) {
            invoice.setTaxAmount(request.getTaxAmount());
        }
        if (request.getTaxRate() != null) {
            invoice.setTaxRate(request.getTaxRate());
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
        log.info("Invoice updated: id={}", id);
        return invoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        log.info("Delete invoice: id={}", id);

        Invoice invoice = getById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("Invoice not found: " + id);
        }

        invoice.setDeleted(true);
        invoice.setUpdateTime(LocalDateTime.now());
        updateById(invoice);
        log.info("Invoice deleted: id={}", id);
    }

    @Override
    public Page<Invoice> pageByPartyId(String partyId, int pageNum, int pageSize) {
        log.debug("Page invoices by party: partyId={}, page={}, size={}", partyId, pageNum, pageSize);

        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(partyId), Invoice::getPartyId, partyId)
                .orderByDesc(Invoice::getInvoiceDate);

        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice issueInvoice(String id, String issuerName) {
        log.info("Issue invoice: id={}, issuer={}", id, issuerName);

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
        log.info("Invoice issued: id={}, invoiceNo={}", id, invoice.getInvoiceNo());
        return invoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice voidInvoice(String id) {
        log.info("Void invoice: id={}", id);

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
        log.info("Invoice voided: id={}", id);
        return invoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice redFlushInvoice(String id) {
        log.info("Red-flush invoice: id={}", id);

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
        log.info("Invoice red-flushed: id={}", id);
        return invoice;
    }
}
