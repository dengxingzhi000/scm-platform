package com.frog.finance.api;

import com.frog.finance.api.dto.FreightResult;
import com.frog.finance.api.dto.InvoiceVO;
import com.frog.finance.api.dto.SettlementVO;
import com.frog.finance.api.request.FreightRequest;
import com.frog.finance.api.request.SettlementRequest;

/**
 * 财务服务 Dubbo 接口
 *
 * <p>提供结算、发票、运费计算等核心功能，供其他微服务通过 RPC 调用。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface FinanceDubboService {

    /**
     * 创建结算单
     *
     * @param request 结算请求
     * @return 结算单信息
     */
    SettlementVO createSettlement(SettlementRequest request);

    /**
     * 根据 ID 查询发票
     *
     * @param id 发票 ID
     * @return 发票信息，不存在时返回 null
     */
    InvoiceVO getInvoiceById(Long id);

    /**
     * 计算运费
     *
     * @param request 运费计算请求
     * @return 运费计算结果
     */
    FreightResult calculateFreight(FreightRequest request);
}
