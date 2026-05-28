package scm.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.frog.common.util.UUIDv7Util;
import scm.finance.domain.entity.Invoice;
import scm.finance.mapper.InvoiceMapper;
import scm.finance.service.IInvoiceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class InvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice> implements IInvoiceService {

    @Override
    public List<Invoice> listByPartyId(String partyId) {
        log.debug("按往来方查询发票: partyId={}", partyId);
        LambdaQueryWrapper<Invoice> wrapper = Wrappers.lambdaQuery()
                .eq(StringUtils.hasText(partyId), Invoice::getPartyId, partyId)
                .eq(Invoice::getDeleted, false)
                .orderByDesc(Invoice::getInvoiceDate);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice issueInvoice(String id, String issuerName) {
        log.info("开具发票: id={}, issuer={}", id, issuerName);

        Invoice invoice = getById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("发票不存在: " + id);
        }
        if (invoice.getStatus() != 0) {
            throw new IllegalStateException("只有草稿状态的发票才能开具, 当前状态: " + invoice.getStatus());
        }

        invoice.setStatus(1);
        invoice.setIssuerName(issuerName);
        invoice.setIssueDate(LocalDate.now());
        invoice.setUpdateTime(LocalDateTime.now());
        invoice.setUpdateBy(issuerName);

        updateById(invoice);
        log.info("发票开具成功: id={}, invoiceNo={}", id, invoice.getInvoiceNo());
        return invoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice voidInvoice(String id) {
        log.info("作废发票: id={}", id);

        Invoice invoice = getById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("发票不存在: " + id);
        }
        if (invoice.getStatus() == 3 || invoice.getStatus() == 4) {
            throw new IllegalStateException("发票已作废或已红冲, 不能再次作废");
        }

        invoice.setStatus(3);
        invoice.setUpdateTime(LocalDateTime.now());

        updateById(invoice);
        log.info("发票作废成功: id={}", id);
        return invoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice redFlushInvoice(String id) {
        log.info("红冲发票: id={}", id);

        Invoice invoice = getById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("发票不存在: " + id);
        }
        if (invoice.getStatus() != 1 && invoice.getStatus() != 2) {
            throw new IllegalStateException("只有已开具或已邮寄的发票才能红冲, 当前状态: " + invoice.getStatus());
        }

        invoice.setStatus(4);
        invoice.setUpdateTime(LocalDateTime.now());

        updateById(invoice);
        log.info("发票红冲成功: id={}", id);
        return invoice;
    }
}
