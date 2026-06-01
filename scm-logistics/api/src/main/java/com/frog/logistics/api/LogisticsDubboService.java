package com.frog.logistics.api;

import com.frog.logistics.api.dto.WaybillVO;
import com.frog.logistics.api.request.WaybillRequest;

/**
 * 物流服务 Dubbo 接口
 *
 * <p>提供运单创建、查询、物流状态更新等核心功能，供其他微服务通过 RPC 调用。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface LogisticsDubboService {

    /**
     * 创建运单
     *
     * @param request 运单请求
     * @return 运单信息
     */
    WaybillVO createWaybill(WaybillRequest request);

    /**
     * 根据 ID 查询运单
     *
     * @param id 运单 ID
     * @return 运单信息，不存在时返回 null
     */
    WaybillVO getWaybillById(Long id);

    /**
     * 更新物流跟踪状态
     *
     * @param waybillId 运单 ID
     * @param status 新状态
     */
    void updateTrackingStatus(Long waybillId, String status);
}
