package com.scmcloud.finance.service.impl;

import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.finance.domain.entity.ReconciliationRecord;
import com.scmcloud.finance.mapper.ReconciliationRecordMapper;
import com.scmcloud.finance.service.IReconciliationRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
        log.info("鍒涘缓瀵硅处璁板綍: partyName={}, period={}", record.getPartyName(), record.getReconciliationPeriod());

        record.setId(UUIDv7Util.generateString());
        record.setReconciliationNo(generateReconciliationNo());
        record.setStatus(0);
        record.setHasDiff(false);
        record.setDeleted(false);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        if (record.getOurTotalAmount() != null && record.getTheirTotalAmount() != null) {
            BigDecimal diff = record.getOurTotalAmount().subtract(record.getTheirTotalAmount());
            record.setDiffAmount(diff);
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                record.setHasDiff(true);
            }
        }

        boolean success = save(record);
        if (!success) {
            throw new RuntimeException("鍒涘缓瀵硅处璁板綍澶辫触");
        }

        log.info("瀵硅处璁板綍鍒涘缓鎴愬姛: id={}, reconciliationNo={}", record.getId(), record.getReconciliationNo());
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord reconcile(String id, String reconcilerId, String reconcilerName) {
        log.info("Execute reconciliation: id={}, reconciler={}", id, reconcilerName);

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
            log.warn("Reconciliation has differences: id={}, diffAmount={}", id, record.getDiffAmount());
        }

        updateById(record);
        log.info("Reconciliation completed: id={}, status={}", id, record.getStatus());
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord confirm(String id, String confirmerId, String confirmerName) {
        log.info("Confirm reconciliation: id={}, confirmer={}", id, confirmerName);

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
        log.info("Reconciliation confirmed successfully: id={}", id);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord markAsDiff(String id, String diffReason) {
        log.info("Mark reconciliation difference: id={}, reason={}", id, diffReason);

        ReconciliationRecord record = getById(id);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw new IllegalArgumentException("Reconciliation record not found: " + id);
        record.setDiffReason(diffReason);
        record.setUpdateTime(LocalDateTime.now());

        updateById(record);
        log.info("瀵硅处宸紓鏍囪鎴愬姛: id={}", id);
        return record;
    }

    private String generateReconciliationNo() {
        return "RCN" + System.currentTimeMillis();
    }
}
