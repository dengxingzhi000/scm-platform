package com.scmcloud.inventory.api;

import com.scmcloud.inventory.api.exception.InsufficientStockException;
import com.scmcloud.inventory.api.request.BatchDeductStockRequest;

/**
 * 搴撳瓨鏈嶅姟 Dubbo 鎺ュ彛
 *
 * <p>鎻愪緵搴撳瓨鏌ヨ銆佹墸鍑忋€侀噴鏀剧瓑鏍稿績鍔熻兘锟?
 * <p>鎵€鏈変慨鏀瑰簱瀛樼殑鏂规硶閮戒細鍙備笌 Seata 鍒嗗竷寮忎簨鍔★拷
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface InventoryDubboService {

    /**
     * 鎵e噺搴撳瓨
     *
     * <p>姝ゆ柟娉曞弬锟絊eata 鍒嗗竷寮忎簨鍔★紝鏃犻渶娣诲姞 @GlobalTransactional 娉ㄨВ锟?
     * <p>閫氳繃 Dubbo RPC 璋冪敤鏃讹紝浼氳嚜鍔ㄥ姞鍏ヨ皟鐢ㄦ柟鐨勫叏灞€浜嬪姟锟?
     *
     * @param skuId SKU ID
     * @param quantity 鎵e噺鏁伴噺
     * @param requestId 骞傜瓑鎬ц锟絀D锛堝缓璁娇鐢ㄨ鍗曞彿锟?
     * @throws InsufficientStockException 搴撳瓨涓嶈冻寮傚父
     * @throws IllegalArgumentException 鍙傛暟闈炴硶寮傚父
     */
    void deductStock(Long skuId, Integer quantity, String requestId);

    /**
     * 鎵归噺鎵e噺搴撳瓨
     *
     * @param deductRequest 鎵归噺鎵e噺璇锋眰
     */
    void batchDeductStock(BatchDeductStockRequest deductRequest);

    /**
     * 閲婃斁搴撳瓨锛堝洖婊氭墸鍑忥級
     *
     * @param skuId SKU ID
     * @param quantity 閲婃斁鏁伴噺
     * @param requestId 骞傜瓑鎬ц锟絀D
     */
    void releaseStock(Long skuId, Integer quantity, String requestId);

    /**
     * 鏌ヨ鍙敤搴撳瓨
     *
     * @param skuId SKU ID
     * @return 鍙敤搴撳瓨鏁伴噺
     */
    Integer queryAvailableStock(Long skuId);
}
