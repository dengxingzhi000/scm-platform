package com.cognora.scm.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cognora.platform.common.exception.ServiceException;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.util.UUIDv7Util;
import com.cognora.scm.finance.domain.dto.CreateBadDebtWriteOffRequest;
import com.cognora.scm.finance.domain.entity.BadDebtWriteOff;
import com.cognora.scm.finance.domain.entity.Invoice;
import com.cognora.scm.finance.domain.entity.SettlementOrder;
import com.cognora.scm.finance.domain.enums.BadDebtWriteOffStatus;
import com.cognora.scm.finance.domain.support.FinanceAmount;
import com.cognora.scm.finance.mapper.BadDebtWriteOffMapper;
import com.cognora.scm.finance.mapper.InvoiceMapper;
import com.cognora.scm.finance.mapper.SettlementOrderMapper;
import com.cognora.scm.finance.service.BadDebtWriteOffService;
import com.cognora.scm.finance.service.support.AccountingPeriodGuard;
import com.cognora.scm.finance.service.support.FinanceTenantGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * 坏账核销单据化实现。
 *
 * <p>三条硬约束：</p>
 * <ol>
 *   <li>核销必须经过审批，{@code PENDING → WRITTEN_OFF} 被状态机拒绝；</li>
 *   <li>核销金额绑定账期（{@code period_code}），申请与生效时账期都必须可写；</li>
 *   <li>同一结算单/发票不允许重复核销：先按「进行中 + 已核销」查重，再由数据库
 *       部分唯一索引兜底。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BadDebtWriteOffServiceImpl implements BadDebtWriteOffService {

    private static final Pattern PERIOD_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BadDebtWriteOffMapper badDebtWriteOffMapper;

    private final SettlementOrderMapper settlementOrderMapper;

    private final InvoiceMapper invoiceMapper;

    private final AccountingPeriodGuard accountingPeriodGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BadDebtWriteOff apply(CreateBadDebtWriteOffRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("核销申请不能为空");
        }
        String settlementId = trimToNull(request.getSettlementId());
        String invoiceId = trimToNull(request.getInvoiceId());
        if (!StringUtils.hasText(settlementId) && !StringUtils.hasText(invoiceId)) {
            throw new IllegalArgumentException("结算单 ID 与发票 ID 至少填写一个");
        }
        if (!StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("核销原因不能为空");
        }
        if (!StringUtils.hasText(request.getApplicantId()) || !StringUtils.hasText(request.getApplicantName())) {
            throw new IllegalArgumentException("申请人 ID 与姓名不能为空");
        }
        String periodCode = normalizePeriodCode(request.getPeriodCode());
        var amount = FinanceAmount.positive(request.getAmount(), "核销金额");

        String targetKey = StringUtils.hasText(settlementId) ? "结算单 " + settlementId : "发票 " + invoiceId;
        if (StringUtils.hasText(settlementId)) {
            assertSettlementExists(settlementId);
        }
        if (StringUtils.hasText(invoiceId)) {
            assertInvoiceExists(invoiceId);
        }
        assertNoActiveWriteOff(settlementId, invoiceId);
        accountingPeriodGuard.assertPeriodWritable(periodCode, targetKey);

        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        LocalDateTime now = LocalDateTime.now();
        BadDebtWriteOff writeOff = new BadDebtWriteOff()
                .setId(UUIDv7Util.generateString())
                .setTenantId(tenantId)
                .setWriteOffNo(generateWriteOffNo())
                .setSettlementId(settlementId)
                .setInvoiceId(invoiceId)
                .setPartnerId(trimToNull(request.getPartnerId()))
                .setPartnerName(trimToNull(request.getPartnerName()))
                .setAmount(amount)
                .setPeriodCode(periodCode)
                .setStatus(BadDebtWriteOffStatus.PENDING.getCode())
                .setReason(request.getReason().trim())
                .setApplicantId(request.getApplicantId().trim())
                .setApplicantName(request.getApplicantName().trim())
                .setAppliedAt(now)
                .setCreateTime(now)
                .setCreateBy(request.getApplicantId().trim())
                .setUpdateTime(now)
                .setUpdateBy(request.getApplicantId().trim())
                .setDeleted(false);

        if (badDebtWriteOffMapper.insert(writeOff) <= 0) {
            throw new IllegalStateException("坏账核销申请写入失败: " + targetKey);
        }
        log.info("坏账核销申请已提交: no={}, target={}, amount={}, period={}",
                writeOff.getWriteOffNo(), targetKey, amount, periodCode);
        return writeOff;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BadDebtWriteOff approve(String id, String approverId, String approverName, String remark) {
        if (!StringUtils.hasText(approverId) || !StringUtils.hasText(approverName)) {
            throw new IllegalArgumentException("审批人 ID 与姓名不能为空");
        }
        BadDebtWriteOff writeOff = loadForUpdate(id);
        assertTransition(writeOff, BadDebtWriteOffStatus.APPROVED);

        LocalDateTime now = LocalDateTime.now();
        writeOff.setStatus(BadDebtWriteOffStatus.APPROVED.getCode())
                .setApproverId(approverId.trim())
                .setApproverName(approverName.trim())
                .setApprovedAt(now)
                .setApproveRemark(trimToNull(remark))
                .setUpdateTime(now)
                .setUpdateBy(approverId.trim());
        badDebtWriteOffMapper.updateById(writeOff);
        log.info("坏账核销已批准: no={}, approver={}", writeOff.getWriteOffNo(), approverId);
        return writeOff;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BadDebtWriteOff reject(String id, String reason, String rejectedBy) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("驳回原因不能为空");
        }
        if (!StringUtils.hasText(rejectedBy)) {
            throw new IllegalArgumentException("驳回操作人不能为空");
        }
        BadDebtWriteOff writeOff = loadForUpdate(id);
        assertTransition(writeOff, BadDebtWriteOffStatus.REJECTED);

        LocalDateTime now = LocalDateTime.now();
        writeOff.setStatus(BadDebtWriteOffStatus.REJECTED.getCode())
                .setRejectedBy(rejectedBy.trim())
                .setRejectedAt(now)
                .setRejectReason(reason.trim())
                .setUpdateTime(now)
                .setUpdateBy(rejectedBy.trim());
        badDebtWriteOffMapper.updateById(writeOff);
        log.info("坏账核销已驳回: no={}, rejectedBy={}, reason={}",
                writeOff.getWriteOffNo(), rejectedBy, reason);
        return writeOff;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BadDebtWriteOff writeOff(String id, String writtenOffBy) {
        if (!StringUtils.hasText(writtenOffBy)) {
            throw new IllegalArgumentException("核销操作人不能为空");
        }
        BadDebtWriteOff writeOff = loadForUpdate(id);
        assertTransition(writeOff, BadDebtWriteOffStatus.WRITTEN_OFF);

        String targetKey = StringUtils.hasText(writeOff.getSettlementId())
                ? "结算单 " + writeOff.getSettlementId()
                : "发票 " + writeOff.getInvoiceId();
        // 审批到生效之间账期可能被关账，生效前再校验一次，避免核销落入已关账期间。
        accountingPeriodGuard.assertPeriodWritable(writeOff.getPeriodCode(), targetKey);

        LocalDateTime now = LocalDateTime.now();
        writeOff.setStatus(BadDebtWriteOffStatus.WRITTEN_OFF.getCode())
                .setWrittenOffBy(writtenOffBy.trim())
                .setWrittenOffAt(now)
                .setUpdateTime(now)
                .setUpdateBy(writtenOffBy.trim());
        badDebtWriteOffMapper.updateById(writeOff);
        log.info("坏账核销已生效: no={}, target={}, amount={}, period={}",
                writeOff.getWriteOffNo(), targetKey, writeOff.getAmount(), writeOff.getPeriodCode());
        return writeOff;
    }

    @Override
    public BadDebtWriteOff getById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("核销单 ID 不能为空");
        }
        BadDebtWriteOff writeOff = badDebtWriteOffMapper.selectById(id);
        if (writeOff == null || Boolean.TRUE.equals(writeOff.getDeleted())) {
            throw new IllegalArgumentException("核销单不存在: " + id);
        }
        FinanceTenantGuard.verifyOwnership(writeOff.getTenantId());
        return writeOff;
    }

    private BadDebtWriteOff loadForUpdate(String id) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("核销单 ID 不能为空");
        }
        BadDebtWriteOff writeOff = badDebtWriteOffMapper.selectById(id);
        if (writeOff == null || Boolean.TRUE.equals(writeOff.getDeleted())) {
            throw new IllegalArgumentException("核销单不存在: " + id);
        }
        FinanceTenantGuard.verifyOwnership(writeOff.getTenantId());
        return writeOff;
    }

    private void assertTransition(BadDebtWriteOff writeOff, BadDebtWriteOffStatus target) {
        BadDebtWriteOffStatus current = BadDebtWriteOffStatus.fromCode(writeOff.getStatus());
        if (current == null || !current.canTransitionTo(target)) {
            throw new ServiceException(ResultCode.CONFLICT.getCode(), HttpStatus.CONFLICT,
                    "非法核销单流转: " + current + " -> " + target);
        }
    }

    /**
     * 同一结算单/发票在「申请中 / 已批准 / 已核销」状态下只允许一张核销单。
     */
    private void assertNoActiveWriteOff(String settlementId, String invoiceId) {
        LambdaQueryWrapper<BadDebtWriteOff> wrapper = Wrappers.<BadDebtWriteOff>lambdaQuery()
                .eq(BadDebtWriteOff::getDeleted, false)
                .in(BadDebtWriteOff::getStatus,
                        BadDebtWriteOffStatus.PENDING.getCode(),
                        BadDebtWriteOffStatus.APPROVED.getCode(),
                        BadDebtWriteOffStatus.WRITTEN_OFF.getCode());
        if (StringUtils.hasText(settlementId)) {
            wrapper.eq(BadDebtWriteOff::getSettlementId, settlementId);
        } else {
            wrapper.eq(BadDebtWriteOff::getInvoiceId, invoiceId);
        }
        List<BadDebtWriteOff> existing = badDebtWriteOffMapper.selectList(wrapper);
        if (existing.isEmpty()) {
            return;
        }
        boolean writtenOff = existing.stream()
                .anyMatch(item -> BadDebtWriteOffStatus.fromCode(item.getStatus())
                        == BadDebtWriteOffStatus.WRITTEN_OFF);
        String key = StringUtils.hasText(settlementId) ? "结算单 " + settlementId : "发票 " + invoiceId;
        if (writtenOff) {
            throw new ServiceException(ResultCode.CONFLICT.getCode(), HttpStatus.CONFLICT,
                    key + " 已核销，禁止重复核销");
        }
        throw new ServiceException(ResultCode.CONFLICT.getCode(), HttpStatus.CONFLICT,
                key + " 已有进行中的坏账核销申请");
    }

    private void assertSettlementExists(String settlementId) {
        SettlementOrder order = settlementOrderMapper.selectById(settlementId);
        if (order == null || Boolean.TRUE.equals(order.getDeleted())) {
            throw new IllegalArgumentException("结算单不存在: " + settlementId);
        }
        FinanceTenantGuard.verifyOwnership(order.getTenantId());
    }

    private void assertInvoiceExists(String invoiceId) {
        Invoice invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("发票不存在: " + invoiceId);
        }
        FinanceTenantGuard.verifyOwnership(invoice.getTenantId());
    }

    private String normalizePeriodCode(String periodCode) {
        if (!StringUtils.hasText(periodCode)) {
            throw new IllegalArgumentException("核销账期不能为空");
        }
        String normalized = periodCode.trim();
        if (!PERIOD_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("核销账期格式必须为 YYYY-MM: " + periodCode);
        }
        return normalized;
    }

    private String generateWriteOffNo() {
        return "BD" + NO_FORMAT.format(LocalDateTime.now())
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
