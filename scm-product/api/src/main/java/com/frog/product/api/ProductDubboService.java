package com.frog.product.api;

import java.io.Serializable;
import java.util.List;

/**
 * 商品服务 Dubbo 接口
 *
 * <p>提供商品、SKU 查询等核心功能，供其他微服务通过 RPC 调用。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface ProductDubboService {

    /**
     * 根据 ID 查询商品
     *
     * @param id 商品 ID
     * @return 商品信息，不存在时返回 null
     */
    ProductVO getProductById(Long id);

    /**
     * 根据 SKU ID 查询 SKU 信息
     *
     * @param skuId SKU ID
     * @return SKU 信息，不存在时返回 null
     */
    SkuVO getSkuById(Long skuId);

    /**
     * 批量查询商品
     *
     * @param ids 商品 ID 列表
     * @return 商品列表
     */
    List<ProductVO> batchGetProducts(List<Long> ids);

    /**
     * 搜索商品
     *
     * @param keyword 搜索关键词
     * @param page 页码（从 1 开始）
     * @param size 每页数量
     * @return 搜索结果
     */
    ProductSearchResult searchProducts(String keyword, int page, int size);

    /**
     * 商品信息
     */
    class ProductVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private String name;
        private String code;
        private String category;
        private String brand;
        private String unit;
        private Integer status;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getBrand() {
            return brand;
        }

        public void setBrand(String brand) {
            this.brand = brand;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    /**
     * SKU 信息
     */
    class SkuVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private Long productId;
        private String skuCode;
        private String spec;
        private java.math.BigDecimal price;
        private Integer status;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getSkuCode() {
            return skuCode;
        }

        public void setSkuCode(String skuCode) {
            this.skuCode = skuCode;
        }

        public String getSpec() {
            return spec;
        }

        public void setSpec(String spec) {
            this.spec = spec;
        }

        public java.math.BigDecimal getPrice() {
            return price;
        }

        public void setPrice(java.math.BigDecimal price) {
            this.price = price;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    /**
     * 商品搜索结果
     */
    class ProductSearchResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<ProductVO> items;
        private long total;
        private int page;
        private int size;

        public List<ProductVO> getItems() {
            return items;
        }

        public void setItems(List<ProductVO> items) {
            this.items = items;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }
    }
}
