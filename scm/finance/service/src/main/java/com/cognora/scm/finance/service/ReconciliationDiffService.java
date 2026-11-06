package com.cognora.scm.finance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.scm.finance.domain.entity.ReconciliationDiff;

/**
 * 对账差异闭环：发现 → 确认/驳回 → 核销。
 */
public interface ReconciliationDiffService {

    ReconciliationDiff create(ReconciliationDiff diff);

    ReconciliationDiff confirm(String id, String resolution, String resolvedBy);

    ReconciliationDiff reject(String id, String reason, String resolvedBy);

    ReconciliationDiff writeOff(String id, String resolution, String resolvedBy);

    ReconciliationDiff getById(String id);

    Page<ReconciliationDiff> pageQuery(String reconciliationId, Integer status, int page, int size);
}
