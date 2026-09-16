package com.scmcloud.product.api;

import com.scmcloud.product.api.dto.ProductSearchResult;
import com.scmcloud.product.api.dto.ProductVO;
import com.scmcloud.product.api.dto.SkuVO;
import java.util.List;

/**
 * 鍟嗗搧鏈嶅姟 Dubbo 鎺ュ彛
 *
 * <p>鎻愪緵鍟嗗搧銆丼KU 鏌ヨ绛夋牳蹇冨姛鑳斤紝渚涘叾浠栧井鏈嶅姟閫氳繃 RPC 璋冪敤锟?
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface ProductDubboService {

    /**
     * 鏍规嵁 ID 鏌ヨ鍟嗗搧
     *
     * @param id 鍟嗗搧 ID
     * @return 鍟嗗搧淇℃伅锛屼笉瀛樺湪鏃惰繑锟絥ull
     */
    ProductVO getProductById(Long id);

    /**
     * 鏍规嵁 SKU ID 鏌ヨ SKU 淇℃伅
     *
     * @param skuId SKU ID
     * @return SKU 淇℃伅锛屼笉瀛樺湪鏃惰繑锟絥ull
     */
    SkuVO getSkuById(Long skuId);

    /**
     * 鎵归噺鏌ヨ鍟嗗搧
     *
     * @param ids 鍟嗗搧 ID 鍒楄〃
     * @return 鍟嗗搧鍒楄〃
     */
    List<ProductVO> batchGetProducts(List<Long> ids);

    /**
     * 鎼滅储鍟嗗搧
     *
     * @param keyword 鎼滅储鍏抽敭锟?
     * @param page 椤电爜锛堜粠 1 寮€濮嬶級
     * @param size 姣忛〉鏁伴噺
     * @return 鎼滅储缁撴灉
     */
    ProductSearchResult searchProducts(String keyword, int page, int size);
}
