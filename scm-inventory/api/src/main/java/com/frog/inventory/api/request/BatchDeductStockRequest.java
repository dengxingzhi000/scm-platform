package com.frog.inventory.api.request;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 批量扣减库存请求
 */
@Data
@Accessors(chain = true)
public class BatchDeductStockRequest {

    private List<StockItem> items;
    private String requestId;

    @Data
    @Accessors(chain = true)
    public static class StockItem {
        private Long skuId;
        private Integer quantity;
    }
}
