package com.cognora.scm.finance.service.impl;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.util.UUIDv7Util;
import com.cognora.scm.finance.domain.entity.ReconciliationRecord;
import com.cognora.scm.finance.domain.support.FinanceAmount;
import com.cognora.scm.finance.mapper.ReconciliationRecordMapper;
import com.cognora.scm.finance.service.IReconciliationRecordService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class ReconciliationRecordServiceImpl extends ServiceImpl<ReconciliationRecordMapper, ReconciliationRecord>
        implements IReconciliationRecordService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord createReconciliation(ReconciliationRecord record) {
        log.info("创建对账记录: partyName={}, period={}", (record.getPartyName() + "").replace("\r", "_").replace("\n", "_"), (record.getReconciliationPeriod() + "").replace("\r", "_").replace("\n", "_"));

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
        if (record.getStatus() != 0) {
            throw new IllegalStateException("Only pending records can be reconciled, current status: " + record.getStatus());
        }

        record.setStatus(1);
        record.setReconcilerId(reconcilerId);
        record.setReconcilerName(reconcilerName);
        record.setReconciledAt(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        if (Boolean.TRUE.equals(record.getHasDiff())) {
            record.setStatus(3);
            log.warn("Reconciliation has differences: id={}, diffAmount={}", (id + "").replace("\r", "_").replace("\n", "_"), (record.getDiffAmount() + "").replace("\r", "_").replace("\n", "_"));
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
        if (record.getStatus() != 1 && record.getStatus() != 3) {
            throw new IllegalStateException("Only reconciled or diff records can be confirmed, current status: " + record.getStatus());
        }

        record.setStatus(2);
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
    public ReconciliationRecord markAsDiff(String id, String diffReason) {
        log.info("Mark reconciliation difference: id={}, reason={}", (id + "").replace("\r", "_").replace("\n", "_"), (diffReason + "").replace("\r", "_").replace("\n", "_"));

        ReconciliationRecord record = getById(id);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw new IllegalArgumentException("Reconciliation record not found: " + id);
        }
        record.setDiffReason(diffReason);
        record.setUpdateTime(LocalDateTime.now());

        updateById(record);

        return record;
    }

    @Override
    public ReconciliationRecord updateReconciliation(String id, ReconciliationRecord record) {
        log.info("对账记录更新成功: id={}", (id + "").replace("\r", "_").replace("\n", "_"));

        ReconciliationRecord existing = getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            throw new IllegalArgumentException("Reconciliation record not found: " + id);
        }
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
