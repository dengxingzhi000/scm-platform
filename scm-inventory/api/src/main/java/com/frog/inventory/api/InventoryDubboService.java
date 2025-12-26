package com.frog.inventory.api;

/**
 * 库存服务 Dubbo 接口
 *
 * <p>提供库存查询、扣减、释放等核心功能。
 * <p>所有修改库存的方法都会参与 Seata 分布式事务。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface InventoryDubboService {

    /**
     * 扣减库存
     *
     * <p>此方法参与 Seata 分布式事务，无需添加 @GlobalTransactional 注解。
     * <p>通过 Dubbo RPC 调用时，会自动加入调用方的全局事务。
     *
     * @param skuId SKU ID
     * @param quantity 扣减数量
     * @param requestId 幂等性请求 ID（建议使用订单号）
     * @throws InsufficientStockException 库存不足异常
     * @throws IllegalArgumentException 参数非法异常
     */
    void deductStock(Long skuId, Integer quantity, String requestId);

    /**
     * 批量扣减库存
     *
     * @param deductRequest 批量扣减请求
     */
    void batchDeductStock(BatchDeductStockRequest deductRequest);

    /**
     * 释放库存（回滚扣减）
     *
     * @param skuId SKU ID
     * @param quantity 释放数量
     * @param requestId 幂等性请求 ID
     */
    void releaseStock(Long skuId, Integer quantity, String requestId);

    /**
     * 查询可用库存
     *
     * @param skuId SKU ID
     * @return 可用库存数量
     */
    Integer queryAvailableStock(Long skuId);

    /**
     * 批量扣减库存请求
     */
    class BatchDeductStockRequest implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private java.util.List<StockItem> items;
        private String requestId;

        public static class StockItem implements java.io.Serializable {
            private static final long serialVersionUID = 1L;
            private Long skuId;
            private Integer quantity;

            public Long getSkuId() {
                return skuId;
            }

            public void setSkuId(Long skuId) {
                this.skuId = skuId;
            }

            public Integer getQuantity() {
                return quantity;
            }

            public void setQuantity(Integer quantity) {
                this.quantity = quantity;
            }
        }

        public java.util.List<StockItem> getItems() {
            return items;
        }

        public void setItems(java.util.List<StockItem> items) {
            this.items = items;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }
    }

    /**
     * 库存不足异常
     */
    class InsufficientStockException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public InsufficientStockException(String message) {
            super(message);
        }

        public InsufficientStockException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}