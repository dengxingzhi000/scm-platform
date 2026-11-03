package com.scmcloud.purchase.api;

import com.scmcloud.purchase.api.dto.PurchaseOrderVO;
import com.scmcloud.purchase.api.request.PurchaseRequestDTO;

/**
 * 閲囪喘鏈嶅姟 Dubbo 鎺ュ彛
 *
 * <p>鎻愪緵閲囪喘鐢宠銆侀噰璐崟鏌ヨ銆佹敹璐х'璁ょ瓑鏍稿績鍔熻兘锛屼緵鍏朵粬寰湇鍔¢€氳繃 RPC 璋冪敤锟?
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface PurchaseDubboService {

    /**
     * 鍒涘缓閲囪喘鐢宠
     *
     * @param dto 閲囪喘鐢宠淇℃伅
     * @return 閲囪喘鍗曚俊锟?
     */
    PurchaseOrderVO createPurchaseRequest(PurchaseRequestDTO dto);

    /**
     * 鏍规嵁 ID 鏌ヨ閲囪喘锟?
     *
     * @param id 閲囪喘锟絀D
     * @return 閲囪喘鍗曚俊鎭紝涓嶅瓨鍦ㄦ椂杩斿洖 null
     */
    PurchaseOrderVO getPurchaseOrderById(Long id);

    /**
     * 纭鏀惰揣
     *
     * @param receiptId 鏀惰揣锟絀D
     */
    void confirmReceipt(Long receiptId);
}
