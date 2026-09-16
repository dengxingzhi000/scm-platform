package com.scmcloud.finance.api;

import com.scmcloud.finance.api.dto.FreightResult;
import com.scmcloud.finance.api.dto.InvoiceVO;
import com.scmcloud.finance.api.dto.SettlementVO;
import com.scmcloud.finance.api.request.FreightRequest;
import com.scmcloud.finance.api.request.SettlementRequest;

/**
 * 璐㈠姟鏈嶅姟 Dubbo 鎺ュ彛
 *
 * <p>鎻愪緵缁撶畻銆佸彂绁ㄣ€佽繍璐硅绠楃瓑鏍稿績鍔熻兘锛屼緵鍏朵粬寰湇鍔¢€氳繃 RPC 璋冪敤锟?
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface FinanceDubboService {

    /**
     * 鍒涘缓缁撶畻锟?
     *
     * @param request 缁撶畻璇锋眰
     * @return 缁撶畻鍗曚俊锟?
     */
    SettlementVO createSettlement(SettlementRequest request);

    /**
     * 鏍规嵁 ID 鏌ヨ鍙戠エ
     *
     * @param id 鍙戠エ ID
     * @return 鍙戠エ淇℃伅锛屼笉瀛樺湪鏃惰繑锟絥ull
     */
    InvoiceVO getInvoiceById(Long id);

    /**
     * 璁$畻杩愯垂
     *
     * @param request 杩愯垂璁$畻璇锋眰
     * @return 杩愯垂璁$畻缁撴灉
     */
    FreightResult calculateFreight(FreightRequest request);
}
