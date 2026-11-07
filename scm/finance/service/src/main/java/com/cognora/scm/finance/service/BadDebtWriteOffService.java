package com.cognora.scm.finance.service;

import com.cognora.scm.finance.domain.dto.CreateBadDebtWriteOffRequest;
import com.cognora.scm.finance.domain.entity.BadDebtWriteOff;

/**
 * 坏账核销单据流：申请 → 审批（通过/驳回）→ 核销生效。
 */
public interface BadDebtWriteOffService {

    BadDebtWriteOff apply(CreateBadDebtWriteOffRequest request);

    BadDebtWriteOff approve(String id, String approverId, String approverName, String remark);

    BadDebtWriteOff reject(String id, String reason, String rejectedBy);

    BadDebtWriteOff writeOff(String id, String writtenOffBy);

    BadDebtWriteOff getById(String id);
}
