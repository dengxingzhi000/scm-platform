package com.cognora.scm.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.exception.ServiceException;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.util.UUIDv7Util;
import com.cognora.scm.finance.domain.entity.ReconciliationDiff;
import com.cognora.scm.finance.domain.entity.ReconciliationRecord;
import com.cognora.scm.finance.domain.enums.ReconciliationDiffStatus;
import com.cognora.scm.finance.domain.support.FinanceAmount;
import com.cognora.scm.finance.mapper.ReconciliationDiffMapper;
import com.cognora.scm.finance.mapper.ReconciliationRecordMapper;
import com.cognora.scm.finance.service.ReconciliationDiffService;
import com.cognora.scm.finance.service.support.AccountingPeriodGuard;
import com.cognora.scm.finance.service.support.FinanceTenantGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationDiffServiceImpl implements ReconciliationDiffService {

    private static final Set<String> DIFF_TYPES = Set.of("AMOUNT", "MISSING_DOC", "DUPLICATE", "OTHER");

    private final ReconciliationDiffMapper reconciliationDiffMapper;
    private final ReconciliationRecordMapper reconciliationRecordMapper;
    private final AccountingPeriodGuard accountingPeriodGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationDiff create(ReconciliationDiff diff) {
        // 请求体显式携带其他租户的 tenantId 时直接 403，而不是静默覆盖。
        FinanceTenantGuard.verifyOwnership(diff.getTenantId());

        if (!StringUtils.hasText(diff.getReconciliationId())) {
            throw new IllegalArgumentException("对账记录 ID 不能为空");
        }
        ReconciliationRecord parent = reconciliationRecordMapper.selectById(diff.getReconciliationId());
        if (parent == null || Boolean.TRUE.equals(parent.getDeleted())) {
            throw new IllegalArgumentException("对账记录不存在: " + diff.getReconciliationId());
        }
        FinanceTenantGuard.verifyOwnership(parent.getTenantId());
        accountingPeriodGuard.assertWritable(parent.getReconciliationPeriod(), parent.getStartDate(),
                "对账差异:" + parent.getReconciliationNo());

        String diffType = StringUtils.hasText(diff.getDiffType()) ? diff.getDiffType().trim().toUpperCase() : "AMOUNT";
        if (!DIFF_TYPES.contains(diffType)) {
            throw new IllegalArgumentException("差异类型必须为 AMOUNT/MISSING_DOC/DUPLICATE/OTHER: " + diff.getDiffType());
        }

        BigDecimal ourAmount = FinanceAmount.nonNegative(
                diff.getOurAmount() == null ? BigDecimal.ZERO : diff.getOurAmount(), "我方金额");
        BigDecimal theirAmount = FinanceAmount.nonNegative(
                diff.getTheirAmount() == null ? BigDecimal.ZERO : diff.getTheirAmount(), "对方金额");
        BigDecimal computedDiff = FinanceAmount.normalize(ourAmount.subtract(theirAmount).abs(), "差异金额");
        if (diff.getDiffAmount() != null
                && FinanceAmount.normalize(diff.getDiffAmount(), "差异金额").compareTo(computedDiff) != 0) {
            throw new IllegalArgumentException("差异金额与双方金额不一致: "
                    + diff.getDiffAmount().toPlainString() + " vs " + computedDiff.toPlainString());
        }

        diff.setId(UUIDv7Util.generateString());
        diff.setTenantId(TenantContextHolder.getRequiredTenantId().toString());
        diff.setReconciliationNo(parent.getReconciliationNo());
        diff.setDiffType(diffType);
        diff.setOurAmount(ourAmount);
        diff.setTheirAmount(theirAmount);
        diff.setDiffAmount(computedDiff);
        diff.setCurrency(FinanceAmount.currency(diff.getCurrency()));
        diff.setStatus(ReconciliationDiffStatus.DISCOVERED.getCode());
        diff.setDeleted(false);
        diff.setCreateTime(LocalDateTime.now());
        diff.setUpdateTime(LocalDateTime.now());

        if (reconciliationDiffMapper.insert(diff) <= 0) {
            throw new IllegalStateException("创建对账差异失败");
        }
        log.info("对账差异已登记: id={}, reconciliationNo={}, diffType={}",
                diff.getId(), diff.getReconciliationNo(), diffType);
        return diff;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationDiff confirm(String id, String resolution, String resolvedBy) {
        if (!StringUtils.hasText(resolution)) {
            throw new IllegalArgumentException("确认差异必须填写处理说明");
        }
        return transition(id, ReconciliationDiffStatus.CONFIRMED, resolution, resolvedBy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationDiff reject(String id, String reason, String resolvedBy) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("驳回差异必须填写原因");
        }
        return transition(id, ReconciliationDiffStatus.REJECTED, reason, resolvedBy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationDiff writeOff(String id, String resolution, String resolvedBy) {
        if (!StringUtils.hasText(resolution)) {
            throw new IllegalArgumentException("核销差异必须填写核销说明");
        }
        return transition(id, ReconciliationDiffStatus.WRITTEN_OFF, resolution, resolvedBy);
    }

    @Override
    public ReconciliationDiff getById(String id) {
        ReconciliationDiff diff = requireExisting(id);
        FinanceTenantGuard.verifyOwnership(diff.getTenantId());
        return diff;
    }

    @Override
    public Page<ReconciliationDiff> pageQuery(String reconciliationId, Integer status, int page, int size) {
        LambdaQueryWrapper<ReconciliationDiff> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ReconciliationDiff::getTenantId, TenantContextHolder.getRequiredTenantId().toString())
                .eq(ReconciliationDiff::getDeleted, false)
                .eq(StringUtils.hasText(reconciliationId), ReconciliationDiff::getReconciliationId, reconciliationId)
                .eq(status != null, ReconciliationDiff::getStatus, status)
                .orderByDesc(ReconciliationDiff::getCreateTime);
        return reconciliationDiffMapper.selectPage(new Page<>(page, size), wrapper);
    }

    private ReconciliationDiff transition(String id, ReconciliationDiffStatus target,
                                          String resolution, String resolvedBy) {
        ReconciliationDiff diff = requireExisting(id);
        FinanceTenantGuard.verifyOwnership(diff.getTenantId());

        ReconciliationDiffStatus current = ReconciliationDiffStatus.fromCode(diff.getStatus());
        if (!current.canTransitionTo(target)) {
            throw new ServiceException(ResultCode.CONFLICT.getCode(), HttpStatus.CONFLICT,
                    "非法差异流转: " + current + " -> " + target);
        }

        diff.setStatus(target.getCode());
        diff.setResolution(resolution);
        diff.setResolvedBy(resolvedBy);
        diff.setResolvedAt(LocalDateTime.now());
        diff.setUpdateTime(LocalDateTime.now());
        reconciliationDiffMapper.updateById(diff);
        log.info("对账差异状态流转: id={}, {} -> {}", id, current, target);
        return diff;
    }

    private ReconciliationDiff requireExisting(String id) {
        ReconciliationDiff diff = reconciliationDiffMapper.selectById(id);
        if (diff == null || Boolean.TRUE.equals(diff.getDeleted())) {
            throw new IllegalArgumentException("对账差异不存在: " + id);
        }
        return diff;
    }
}
