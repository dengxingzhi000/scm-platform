package scm.finance.service;

import scm.finance.domain.entity.ReconciliationRecord;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IReconciliationRecordService extends IService<ReconciliationRecord> {

    ReconciliationRecord createReconciliation(ReconciliationRecord record);

    ReconciliationRecord reconcile(String id, String reconcilerId, String reconcilerName);

    ReconciliationRecord confirm(String id, String confirmerId, String confirmerName);

    ReconciliationRecord markAsDiff(String id, String diffReason);
}
