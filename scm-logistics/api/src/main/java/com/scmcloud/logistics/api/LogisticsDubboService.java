package com.scmcloud.logistics.api;

import com.scmcloud.logistics.api.dto.WaybillVO;
import com.scmcloud.logistics.api.request.WaybillRequest;

/**
 * 鐗╂祦鏈嶅姟 Dubbo 鎺ュ彛
 *
 * <p>鎻愪緵杩愬崟鍒涘缓銆佹煡璇€佺墿娴佺姸鎬佹洿鏂扮瓑鏍稿績鍔熻兘锛屼緵鍏朵粬寰湇鍔¢€氳繃 RPC 璋冪敤锟?
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface LogisticsDubboService {

    /**
     * 鍒涘缓杩愬崟
     *
     * @param request 杩愬崟璇锋眰
     * @return 杩愬崟淇℃伅
     */
    WaybillVO createWaybill(WaybillRequest request);

    /**
     * 鏍规嵁 ID 鏌ヨ杩愬崟
     *
     * @param id 杩愬崟 ID
     * @return 杩愬崟淇℃伅锛屼笉瀛樺湪鏃惰繑锟絥ull
     */
    WaybillVO getWaybillById(Long id);

    /**
     * 鏇存柊鐗╂祦璺熻釜鐘讹拷
     *
     * @param waybillId 杩愬崟 ID
     * @param status 鏂扮姸锟?
     */
    void updateTrackingStatus(Long waybillId, String status);
}
