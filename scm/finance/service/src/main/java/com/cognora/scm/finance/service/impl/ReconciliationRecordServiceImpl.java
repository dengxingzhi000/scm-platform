package com.cognora.scm.finance.service.impl;

import com.cognora.platform.common.exception.ServiceException;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.util.UUIDv7Util;
import com.cognora.scm.finance.domain.entity.ReconciliationRecord;
import com.cognora.scm.finance.domain.enums.ReconciliationRecordStatus;
import com.cognora.scm.finance.domain.support.FinanceAmount;
import com.cognora.scm.finance.mapper.ReconciliationRecordMapper;
import com.cognora.scm.finance.service.IReconciliationRecordService;
import com.cognora.scm.finance.service.support.AccountingPeriodGuard;
import com.cognora.scm.finance.service.support.FinanceTenantGuard;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationRecordServiceImpl extends ServiceImpl<ReconciliationRecordMapper, ReconciliationRecord>
        implements IReconciliationRecordService {

    private final AccountingPeriodGuard accountingPeriodGuard;

    private static final Set<String> DIFF_TYPES = Set.of("AMOUNT", "MISSING_DOC", "DUPLICATE", "OTHER");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord createReconciliation(ReconciliationRecord record) {
        log.info("创建对账记录: partyName={}, period={}", (record.getPartyName() + "").replace("\r", "_").replace("\n", "_"), (record.getReconciliationPeriod() + "").replace("\r", "_").replace("\n", "_"));

        FinanceTenantGuard.verifyOwnership(record.getTenantId());
        record.setId(UUIDv7Util.generateString());
        record.setReconciliationNo(generateReconciliationNo());
        record.setStatus(0);
        record.setHasDiff(false);
        // 租户必须取自登录态，忽略请求体里的 tenantId。
        record.setTenantId(TenantContextHolder.getRequiredTenantId().toString());
        record.setDeleted(false);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        normalizeAmounts(record);
        accountingPeriodGuard.assertWritable(record.getReconciliationPeriod(), record.getStartDate(),
                "对账记录:" + record.getReconciliationNo());

        boolean success = save(record);
        if (!success) {
            throw new RuntimeException("创建对账记录失败");
        }

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord reconcile(String id, String reconcilerId, String reconcilerName) {
        log.info("Execute reconciliation: id={}, reconciler={}", (id + "").replace("\r", "_").replace("\n", "_"), (reconcilerName + "").replace("\r", "_").replace("\n", "_"));

        ReconciliationRecord record = getById(id);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw new IllegalArgumentException("Reconciliation record not found: " + id);
        }
        FinanceTenantGuard.verifyOwnership(record.getTenantId());
        accountingPeriodGuard.assertWritable(record.getReconciliationPeriod(), record.getStartDate(),
                "对账记录:" + record.getReconciliationNo());
        ReconciliationRecordStatus target = Boolean.TRUE.equals(record.getHasDiff())
                ? ReconciliationRecordStatus.DIFF
                : ReconciliationRecordStatus.RECONCILED;
        transition(record, target);

        record.setStatus(target.getCode());
        record.setReconcilerId(reconcilerId);
        record.setReconcilerName(reconcilerName);
        record.setReconciledAt(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        if (target == ReconciliationRecordStatus.DIFF) {
            log.warn("Reconciliation has differences: id={}, diffAmount={}",
                    (id + "").replace("\r", "_").replace("\n", "_"),
                    (record.getDiffAmount() + "").replace("\r", "_").replace("\n", "_"));
        }

        updateById(record);
        log.info("Reconciliation completed: id={}, status={}", (id + "").replace("\r", "_").replace("\n", "_"), (record.getStatus() + "").replace("\r", "_").replace("\n", "_"));
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord confirm(String id, String confirmerId, String confirmerName) {
        log.info("Confirm reconciliation: id={}, confirmer={}", (id + "").replace("\r", "_").replace("\n", "_"), (confirmerName + "").replace("\r", "_").replace("\n", "_"));

        ReconciliationRecord record = getById(id);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw new IllegalArgumentException("Reconciliation record not found: " + id);
        }
        FinanceTenantGuard.verifyOwnership(record.getTenantId());
        accountingPeriodGuard.assertWritable(record.getReconciliationPeriod(), record.getStartDate(),
                "对账记录:" + record.getReconciliationNo());
        transition(record, ReconciliationRecordStatus.CONFIRMED);

        record.setStatus(ReconciliationRecordStatus.CONFIRMED.getCode());
        record.setConfirmerId(confirmerId);
        record.setConfirmerName(confirmerName);
        record.setConfirmedAt(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        updateById(record);
        log.info("Reconciliation confirmed successfully: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord markAsDiff(String id, String diffType, String diffReason) {
        log.info("Mark reconciliation difference: id={}, type={}, reason={}",
                (id + "").replace("\r", "_").replace("\n", "_"),
                (diffType + "").replace("\r", "_").replace("\n", "_"),
                (diffReason + "").replace("\r", "_").replace("\n", "_"));

        ReconciliationRecord record = getById(id);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw new IllegalArgumentException("Reconciliation record not found: " + id);
        }
        FinanceTenantGuard.verifyOwnership(record.getTenantId());
        accountingPeriodGuard.assertWritable(record.getReconciliationPeriod(), record.getStartDate(),
                "对账记录:" + record.getReconciliationNo());

        String normalizedDiffType = normalizeDiffType(diffType);
        ReconciliationRecordStatus current = currentStatus(record);
        if (current == ReconciliationRecordStatus.CONFIRMED || current == ReconciliationRecordStatus.REJECTED) {
            throw conflict(record, ReconciliationRecordStatus.DIFF);
        }

        record.setHasDiff(true);
        record.setDiffType(normalizedDiffType);
        record.setDiffReason(diffReason);
        record.setStatus(ReconciliationRecordStatus.DIFF.getCode());
        record.setUpdateTime(LocalDateTime.now());

        updateById(record);

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord reject(String id, String reason, String rejectedBy) {
        log.info("Reject reconciliation: id={}, rejectedBy={}", (id + "").replace("\r", "_").replace("\n", "_"),
                (rejectedBy + "").replace("\r", "_").replace("\n", "_"));

        ReconciliationRecord record = getById(id);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw new IllegalArgumentException("Reconciliation record not found: " + id);
        }
        FinanceTenantGuard.verifyOwnership(record.getTenantId());
        accountingPeriodGuard.assertWritable(record.getReconciliationPeriod(), record.getStartDate(),
                "对账记录:" + record.getReconciliationNo());

        transition(record, ReconciliationRecordStatus.REJECTED);

        record.setStatus(ReconciliationRecordStatus.REJECTED.getCode());
        record.setRejectReason(reason);
        record.setRejectedBy(rejectedBy);
        record.setRejectedAt(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        updateById(record);
        log.info("Reconciliation rejected: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return record;
    }

    private ReconciliationRecordStatus currentStatus(ReconciliationRecord record) {
        ReconciliationRecordStatus current = ReconciliationRecordStatus.fromCode(record.getStatus());
        if (current == null) {
            throw conflict(record, null);
        }
        return current;
    }

    private void transition(ReconciliationRecord record, ReconciliationRecordStatus target) {
        ReconciliationRecordStatus current = currentStatus(record);
        if (!current.canTransitionTo(target)) {
            throw conflict(record, target);
        }
    }

    private ServiceException conflict(ReconciliationRecord record, ReconciliationRecordStatus target) {
        ReconciliationRecordStatus current = ReconciliationRecordStatus.fromCode(record.getStatus());
        String from = current == null ? String.valueOf(record.getStatus()) : current.getLabel();
        String to = target == null ? "未知状态" : target.getLabel();
        return new ServiceException(ResultCode.CONFLICT.getCode(), HttpStatus.CONFLICT,
                "非法的对账记录状态流转: " + from + " -> " + to);
    }

    private String normalizeDiffType(String diffType) {
        String value = diffType == null ? "OTHER" : diffType.trim().toUpperCase(Locale.ROOT);
        if (!DIFF_TYPES.contains(value)) {
            throw new IllegalArgumentException("差异类型仅支持 AMOUNT/MISSING_DOC/DUPLICATE/OTHER: " + diffType);
        }
        return value;
    }

    @Override
    public ReconciliationRecord updateReconciliation(String id, ReconciliationRecord record) {
        log.info("对账记录更新成功: id={}", (id + "").replace("\r", "_").replace("\n", "_"));

        ReconciliationRecord existing = getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            throw new IllegalArgumentException("Reconciliation record not found: " + id);
        }
        FinanceTenantGuard.verifyOwnership(existing.getTenantId());
        accountingPeriodGuard.assertWritable(existing.getReconciliationPeriod(), existing.getStartDate(),
                "对账记录:" + existing.getReconciliationNo());
        // 已对账后双方金额是核对结果，不允许通过更新接口改写。
        if (existing.getStatus() != 0 && hasAmountChanges(existing, record)) {
            throw new IllegalStateException("对账已执行，金额与差异不允许修改: " + id);
        }
        if (existing.getStatus() == 0) {
            applyRequestedAmounts(existing, record);
        }
        if (record.getDiffReason() != null) {
            existing.setDiffReason(record.getDiffReason());
        }
        if (record.getRemark() != null) {
            existing.setRemark(record.getRemark());
        }
        if (record.getAttachments() != null) {
            existing.setAttachments(record.getAttachments());
        }
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
        return existing;
    }

    @Override
    public void deleteReconciliation(String id) {
        ReconciliationRecord record = getById(id);
        if (record != null) {
            FinanceTenantGuard.verifyOwnership(record.getTenantId());
            accountingPeriodGuard.assertWritable(record.getReconciliationPeriod(), record.getStartDate(),
                    "对账记录:" + record.getReconciliationNo());
            record.setDeleted(true);
            record.setUpdateTime(LocalDateTime.now());
            updateById(record);
            log.info("对账记录删除成功: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        }
    }

    private String generateReconciliationNo() {
        return "RCN" + System.currentTimeMillis();
    }

    /**
     * 六项金额统一到两位小数，差异 = 我方总额 - 对方总额。
     */
    private void normalizeAmounts(ReconciliationRecord record) {
        record.setOurTotalAmount(FinanceAmount.optionalOrZero(record.getOurTotalAmount(), "我方总额"));
        record.setOurPaidAmount(FinanceAmount.optionalOrZero(record.getOurPaidAmount(), "我方已付"));
        record.setOurUnpaidAmount(FinanceAmount.optionalOrZero(record.getOurUnpaidAmount(), "我方未付"));
        record.setTheirTotalAmount(FinanceAmount.optionalOrZero(record.getTheirTotalAmount(), "对方总额"));
        record.setTheirPaidAmount(FinanceAmount.optionalOrZero(record.getTheirPaidAmount(), "对方已付"));
        record.setTheirUnpaidAmount(FinanceAmount.optionalOrZero(record.getTheirUnpaidAmount(), "对方未付"));

        BigDecimal diff = FinanceAmount.normalize(
                record.getOurTotalAmount().subtract(record.getTheirTotalAmount()), "差异金额");
        record.setDiffAmount(diff);
        record.setHasDiff(diff.signum() != 0);
    }

    private boolean hasAmountChanges(ReconciliationRecord existing, ReconciliationRecord requested) {
        return differs(existing.getOurTotalAmount(), requested.getOurTotalAmount())
                || differs(existing.getOurPaidAmount(), requested.getOurPaidAmount())
                || differs(existing.getOurUnpaidAmount(), requested.getOurUnpaidAmount())
                || differs(existing.getTheirTotalAmount(), requested.getTheirTotalAmount())
                || differs(existing.getTheirPaidAmount(), requested.getTheirPaidAmount())
                || differs(existing.getTheirUnpaidAmount(), requested.getTheirUnpaidAmount());
    }

    private boolean differs(BigDecimal current, BigDecimal requested) {
        if (requested == null) {
            return false;
        }
        if (current == null) {
            return true;
        }
        return current.compareTo(requested) != 0;
    }

    private void applyRequestedAmounts(ReconciliationRecord existing, ReconciliationRecord record) {
        if (record.getOurTotalAmount() != null) existing.setOurTotalAmount(record.getOurTotalAmount());
        if (record.getOurPaidAmount() != null) existing.setOurPaidAmount(record.getOurPaidAmount());
        if (record.getOurUnpaidAmount() != null) existing.setOurUnpaidAmount(record.getOurUnpaidAmount());
        if (record.getTheirTotalAmount() != null) existing.setTheirTotalAmount(record.getTheirTotalAmount());
        if (record.getTheirPaidAmount() != null) existing.setTheirPaidAmount(record.getTheirPaidAmount());
        if (record.getTheirUnpaidAmount() != null) existing.setTheirUnpaidAmount(record.getTheirUnpaidAmount());
        normalizeAmounts(existing);
    }
}
