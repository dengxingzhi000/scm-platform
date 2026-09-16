package com.scmcloud.finance.service;

import com.scmcloud.finance.domain.entity.ReconciliationRecord;
import com.baomidou.mybatisplus.spring.service.IService;

public interface IReconciliationRecordService extends IService<ReconciliationRecord> {

    ReconciliationRecord createReconciliation(ReconciliationRecord record);

    ReconciliationRecord reconcile(String id, String reconcilerId, String reconcilerName);

    ReconciliationRecord confirm(String id, String confirmerId, String confirmerName);

    ReconciliationRecord markAsDiff(String id, String diffReason);
}
