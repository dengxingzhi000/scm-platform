package com.cognora.lms.logistics.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.lms.logistics.domain.entity.TmsCarrierDispatch;

/**
 * 承运商下单幂等与降级记录（批次 3）。
 */
public interface ITmsCarrierDispatchService extends IService<TmsCarrierDispatch> {

    /**
     * 以幂等键开启一次派发。键已存在时直接返回既有记录，不重复下单。
     */
    TmsCarrierDispatch beginDispatch(String idempotencyKey, String waybillNo, String primaryCarrierId);

    TmsCarrierDispatch completeDispatch(String id, String actualCarrierId, int attemptCount,
                                        String result, String errorMessage);

    TmsCarrierDispatch findByIdempotencyKey(String idempotencyKey);
}
