package com.scmcloud.finance.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.finance.domain.entity.ReconciliationRecord;
import com.scmcloud.finance.mapper.ReconciliationRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationRecordCommandService {
    private final StatusValidator statusValidator;
    private final ReconciliationRecordMapper reconciliationRecordMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord createReconciliation(ReconciliationRecord record) {
        log.info("创建对账记录: partyName={}, period={}", record.getPartyName(), record.getReconciliationPeriod());

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

        int rows = reconciliationRecordMapper.insert(record);
        if (rows <= 0) {
            throw new RuntimeException("创建对账记录失败");
        }

        log.info("对账记录创建成功: id={}, reconciliationNo={}", record.getId(), record.getReconciliationNo());
        return record;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord reconcile(String id, String reconcilerId, String reconcilerName) {
        log.info("执行对账: id={}, reconciler={}", id, reconcilerName);

        ReconciliationRecord record = reconciliationRecordMapper.selectById(id);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw new IllegalArgumentException("对账记录不存在: " + id);
        }
        statusValidator.validateTransition("RECONCILIATION", "DRAFT", "COMPARING");

        record.setStatus(1);
        record.setReconcilerId(reconcilerId);
        record.setReconcilerName(reconcilerName);
        record.setReconciledAt(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        if (Boolean.TRUE.equals(record.getHasDiff())) {
            statusValidator.validateTransition("RECONCILIATION", resolveReconciliationStatusName(record.getStatus()), "MISMATCHED");
            record.setStatus(3);
            log.warn("对账存在差异: id={}, diffAmount={}", id, record.getDiffAmount());
        }

        reconciliationRecordMapper.updateById(record);
        log.info("对账完成: id={}, status={}", id, record.getStatus());
        return record;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord confirm(String id, String confirmerId, String confirmerName) {
        log.info("确认对账: id={}, confirmer={}", id, confirmerName);

        ReconciliationRecord record = reconciliationRecordMapper.selectById(id);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw new IllegalArgumentException("对账记录不存在: " + id);
        }
        statusValidator.validateTransition("RECONCILIATION", resolveReconciliationStatusName(record.getStatus()), "MATCHED");

        record.setStatus(2);
        record.setConfirmerId(confirmerId);
        record.setConfirmerName(confirmerName);
        record.setConfirmedAt(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        reconciliationRecordMapper.updateById(record);
        log.info("对账确认成功: id={}", id);
        return record;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord markAsDiff(String id, String diffReason) {
        log.info("标记对账差异: id={}, reason={}", id, diffReason);

        ReconciliationRecord record = reconciliationRecordMapper.selectById(id);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw new IllegalArgumentException("对账记录不存在: " + id);
        }

        statusValidator.validateTransition("RECONCILIATION", resolveReconciliationStatusName(record.getStatus()), "MISMATCHED");

        record.setStatus(3);
        record.setHasDiff(true);
        record.setDiffReason(diffReason);
        record.setUpdateTime(LocalDateTime.now());

        reconciliationRecordMapper.updateById(record);
        log.info("对账差异标记成功: id={}", id);
        return record;
    }

    private String generateReconciliationNo() {
        return "RCN" + System.currentTimeMillis();
    }

    private String resolveReconciliationStatusName(int status) {
        return switch (status) {
            case 0 -> "DRAFT";
            case 1 -> "COMPARING";
            case 2 -> "MATCHED";
            case 3 -> "MISMATCHED";
            case 4 -> "RESOLVED";
            default -> throw new IllegalStateException("未知的对账状态: " + status);
        };
    }
}
