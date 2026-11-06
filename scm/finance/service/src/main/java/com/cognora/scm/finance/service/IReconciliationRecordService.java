package com.cognora.scm.finance.service;

import com.cognora.scm.finance.domain.entity.ReconciliationRecord;
import com.baomidou.mybatisplus.spring.service.IService;

public interface IReconciliationRecordService extends IService<ReconciliationRecord> {

    ReconciliationRecord createReconciliation(ReconciliationRecord record);

    ReconciliationRecord reconcile(String id, String reconcilerId, String reconcilerName);

    ReconciliationRecord confirm(String id, String confirmerId, String confirmerName);

    ReconciliationRecord markAsDiff(String id, String diffType, String diffReason);

    /**
     * 驳回对账结果：只有已对账（1）与有差异（3）的记录可以驳回，终态记录一律拒绝。
     */
    ReconciliationRecord reject(String id, String reason, String rejectedBy);

    ReconciliationRecord updateReconciliation(String id, ReconciliationRecord record);

    void deleteReconciliation(String id);
}