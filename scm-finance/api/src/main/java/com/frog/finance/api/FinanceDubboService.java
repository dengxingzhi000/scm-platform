package com.frog.finance.api;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务服务 Dubbo 接口
 *
 * <p>提供结算、发票、运费计算等核心功能，供其他微服务通过 RPC 调用。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface FinanceDubboService {

    /**
     * 创建结算单
     *
     * @param request 结算请求
     * @return 结算单信息
     */
    SettlementVO createSettlement(SettlementRequest request);

    /**
     * 根据 ID 查询发票
     *
     * @param id 发票 ID
     * @return 发票信息，不存在时返回 null
     */
    InvoiceVO getInvoiceById(Long id);

    /**
     * 计算运费
     *
     * @param request 运费计算请求
     * @return 运费计算结果
     */
    FreightResult calculateFreight(FreightRequest request);

    /**
     * 结算请求
     */
    class SettlementRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long orderId;
        private Long supplierId;
        private BigDecimal amount;
        private String settlementType;
        private Long applicantId;
        private String remark;

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Long getSupplierId() {
            return supplierId;
        }

        public void setSupplierId(Long supplierId) {
            this.supplierId = supplierId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getSettlementType() {
            return settlementType;
        }

        public void setSettlementType(String settlementType) {
            this.settlementType = settlementType;
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
    }

    /**
     * 结算单信息
     */
    class SettlementVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private String settlementNo;
        private Long orderId;
        private Long supplierId;
        private BigDecimal amount;
        private String status;
        private LocalDateTime createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getSettlementNo() {
            return settlementNo;
        }

        public void setSettlementNo(String settlementNo) {
            this.settlementNo = settlementNo;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Long getSupplierId() {
            return supplierId;
        }

        public void setSupplierId(Long supplierId) {
            this.supplierId = supplierId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    /**
     * 发票信息
     */
    class InvoiceVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private String invoiceNo;
        private Long orderId;
        private Long supplierId;
        private BigDecimal amount;
        private BigDecimal taxAmount;
        private String invoiceType;
        private String status;
        private LocalDateTime issuedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getInvoiceNo() {
            return invoiceNo;
        }

        public void setInvoiceNo(String invoiceNo) {
            this.invoiceNo = invoiceNo;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Long getSupplierId() {
            return supplierId;
        }

        public void setSupplierId(Long supplierId) {
            this.supplierId = supplierId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getTaxAmount() {
            return taxAmount;
        }

        public void setTaxAmount(BigDecimal taxAmount) {
            this.taxAmount = taxAmount;
        }

        public String getInvoiceType() {
            return invoiceType;
        }

        public void setInvoiceType(String invoiceType) {
            this.invoiceType = invoiceType;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getIssuedAt() {
            return issuedAt;
        }

        public void setIssuedAt(LocalDateTime issuedAt) {
            this.issuedAt = issuedAt;
        }
    }

    /**
     * 运费计算请求
     */
    class FreightRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long warehouseId;
        private String destinationAddress;
        private BigDecimal weight;
        private BigDecimal volume;
        private String shippingMethod;

        public Long getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
        }

        public String getDestinationAddress() {
            return destinationAddress;
        }

        public void setDestinationAddress(String destinationAddress) {
            this.destinationAddress = destinationAddress;
        }

        public BigDecimal getWeight() {
            return weight;
        }

        public void setWeight(BigDecimal weight) {
            this.weight = weight;
        }

        public BigDecimal getVolume() {
            return volume;
        }

        public void setVolume(BigDecimal volume) {
            this.volume = volume;
        }

        public String getShippingMethod() {
            return shippingMethod;
        }

        public void setShippingMethod(String shippingMethod) {
            this.shippingMethod = shippingMethod;
        }
    }

    /**
     * 运费计算结果
     */
    class FreightResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private BigDecimal freightAmount;
        private BigDecimal insuranceAmount;
        private BigDecimal totalAmount;
        private String carrier;
        private Integer estimatedDays;

        public BigDecimal getFreightAmount() {
            return freightAmount;
        }

        public void setFreightAmount(BigDecimal freightAmount) {
            this.freightAmount = freightAmount;
        }

        public BigDecimal getInsuranceAmount() {
            return insuranceAmount;
        }

        public void setInsuranceAmount(BigDecimal insuranceAmount) {
            this.insuranceAmount = insuranceAmount;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public String getCarrier() {
            return carrier;
        }

        public void setCarrier(String carrier) {
            this.carrier = carrier;
        }

        public Integer getEstimatedDays() {
            return estimatedDays;
        }

        public void setEstimatedDays(Integer estimatedDays) {
            this.estimatedDays = estimatedDays;
        }
    }
}
