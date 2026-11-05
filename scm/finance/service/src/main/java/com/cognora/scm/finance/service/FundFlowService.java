package com.cognora.scm.finance.service;

import com.cognora.scm.finance.domain.entity.FundFlow;

public interface FundFlowService {

    /**
     * 登记一条资金流水。
     *
     * <p>幂等键在租户内唯一：同一键、同一金额/方向重复提交时返回既有流水；
     * 同一键但金额或方向不同说明调用方复用了幂等键，直接拒绝。</p>
     */
    FundFlow record(FundFlow flow);
}
