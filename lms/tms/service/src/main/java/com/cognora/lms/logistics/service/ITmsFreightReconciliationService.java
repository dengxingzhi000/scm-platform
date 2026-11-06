package com.cognora.lms.logistics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.lms.logistics.domain.entity.TmsFreightReconciliation;

import java.math.BigDecimal;

/**
 * 运费对账（批次 2）。
 */
public interface ITmsFreightReconciliationService extends IService<TmsFreightReconciliation> {

    Page<TmsFreightReconciliation> pageList(int page, int size, String batchNo, String diffType, String status);

    /**
     * 对单票运单做运费对账：按模板重算应收，与承运商账单比较并落差异分类。
     * 同一批次 + 同一运单幂等，重复调用返回既有记录。
     */
    TmsFreightReconciliation reconcile(String waybillId, String regionCode, BigDecimal billedAmount,
                                       String batchNo, String operator);
}
