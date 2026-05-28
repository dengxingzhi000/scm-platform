package scm.finance.service.impl;

import com.frog.common.util.UUIDv7Util;
import scm.finance.domain.entity.ReconciliationRecord;
import scm.finance.mapper.ReconciliationRecordMapper;
import scm.finance.service.IReconciliationRecordService;
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

        boolean success = save(record);
        if (!success) {
            throw new RuntimeException("创建对账记录失败");
        }

        log.info("对账记录创建成功: id={}, reconciliationNo={}", record.getId(), record.getReconciliationNo());
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord reconcile(String id, String reconcilerId, String reconcilerName) {
        log.info("执行对账: id={}, reconciler={}", id, reconcilerName);

        ReconciliationRecord record = getById(id);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw new IllegalArgumentException("对账记录不存在: " + id);
        }
        if (record.getStatus() != 0) {
            throw new IllegalStateException("只有待对账状态的记录才能对账, 当前状态: " + record.getStatus());
        }

        record.setStatus(1);
        record.setReconcilerId(reconcilerId);
        record.setReconcilerName(reconcilerName);
        record.setReconciledAt(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        if (Boolean.TRUE.equals(record.getHasDiff())) {
            record.setStatus(3);
            log.warn("对账存在差异: id={}, diffAmount={}", id, record.getDiffAmount());
        }

        updateById(record);
        log.info("对账完成: id={}, status={}", id, record.getStatus());
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord confirm(String id, String confirmerId, String confirmerName) {
        log.info("确认对账: id={}, confirmer={}", id, confirmerName);

        ReconciliationRecord record = getById(id);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw new IllegalArgumentException("对账记录不存在: " + id);
        }
        if (record.getStatus() != 1 && record.getStatus() != 3) {
            throw new IllegalStateException("只有已对账或有差异状态的记录才能确认, 当前状态: " + record.getStatus());
        }

        record.setStatus(2);
        record.setConfirmerId(confirmerId);
        record.setConfirmerName(confirmerName);
        record.setConfirmedAt(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        updateById(record);
        log.info("对账确认成功: id={}", id);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord markAsDiff(String id, String diffReason) {
        log.info("标记对账差异: id={}, reason={}", id, diffReason);

        ReconciliationRecord record = getById(id);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw new IllegalArgumentException("对账记录不存在: " + id);
        }

        record.setStatus(3);
        record.setHasDiff(true);
        record.setDiffReason(diffReason);
        record.setUpdateTime(LocalDateTime.now());

        updateById(record);
        log.info("对账差异标记成功: id={}", id);
        return record;
    }

    private String generateReconciliationNo() {
        return "RCN" + System.currentTimeMillis();
    }
}
