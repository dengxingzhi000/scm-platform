package com.frog.purchase.api;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购服务 Dubbo 接口
 *
 * <p>提供采购申请、采购单查询、收货确认等核心功能，供其他微服务通过 RPC 调用。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface PurchaseDubboService {

    /**
     * 创建采购申请
     *
     * @param dto 采购申请信息
     * @return 采购单信息
     */
    PurchaseOrderVO createPurchaseRequest(PurchaseRequestDTO dto);

    /**
     * 根据 ID 查询采购单
     *
     * @param id 采购单 ID
     * @return 采购单信息，不存在时返回 null
     */
    PurchaseOrderVO getPurchaseOrderById(Long id);

    /**
     * 确认收货
     *
     * @param receiptId 收货单 ID
     */
    void confirmReceipt(Long receiptId);

    /**
     * 采购申请 DTO
     */
    class PurchaseRequestDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long supplierId;
        private Long warehouseId;
        private Long applicantId;
        private String remark;
        private List<PurchaseItemDTO> items;

        public Long getSupplierId() {
            return supplierId;
        }

        public void setSupplierId(Long supplierId) {
            this.supplierId = supplierId;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
        }

        public Long getApplicantId() {
            return applicantId;
        }

        public void setApplicantId(Long applicantId) {
            this.applicantId = applicantId;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public List<PurchaseItemDTO> getItems() {
            return items;
        }

        public void setItems(List<PurchaseItemDTO> items) {
            this.items = items;
        }
    }

    /**
     * 采购明细 DTO
     */
    class PurchaseItemDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long skuId;
        private Integer quantity;
        private BigDecimal unitPrice;

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

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }
    }

    /**
     * 采购单信息
     */
    class PurchaseOrderVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private String purchaseNo;
        private Long supplierId;
        private Long warehouseId;
        private Long applicantId;
        private BigDecimal totalAmount;
        private String status;
        private String remark;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getPurchaseNo() {
            return purchaseNo;
        }

        public void setPurchaseNo(String purchaseNo) {
            this.purchaseNo = purchaseNo;
        }

        public Long getSupplierId() {
            return supplierId;
        }

        public void setSupplierId(Long supplierId) {
            this.supplierId = supplierId;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
        }

        public Long getApplicantId() {
            return applicantId;
        }

        public void setApplicantId(Long applicantId) {
            this.applicantId = applicantId;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
