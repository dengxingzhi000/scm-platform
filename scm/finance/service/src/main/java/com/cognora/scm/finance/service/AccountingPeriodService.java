package com.cognora.scm.finance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.scm.finance.domain.entity.AccountingPeriod;

import java.util.List;

/**
 * 会计期间（账期）管理。
 */
public interface AccountingPeriodService {

    /**
     * 开启账期：按 {@code YYYY-MM} 推导起止日期，同租户同账期不允许重复开启。
     */
    AccountingPeriod openPeriod(String periodCode);

    /**
     * 关账：仅允许「未结账 → 已结账」，重复关账或非法状态返回 409。
     */
    AccountingPeriod closePeriod(String id, String closedBy);

    AccountingPeriod getById(String id);

    /**
     * 当前账期：返回当前自然月对应的账期记录（无论已结账还是未结账）。
     * 当月尚未开账时返回 {@code null}，由调用方决定提示文案。
     */
    AccountingPeriod getCurrentPeriod();

    /**
     * 已关账期列表：按账期编码倒序返回当前租户下所有已关账账期。
     */
    List<AccountingPeriod> listClosedPeriods();

    Page<AccountingPeriod> pageQuery(int page, int size, Integer status);
}
