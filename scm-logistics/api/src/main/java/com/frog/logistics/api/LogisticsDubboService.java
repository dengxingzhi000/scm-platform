package com.frog.logistics.api;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物流服务 Dubbo 接口
 *
 * <p>提供运单创建、查询、物流状态更新等核心功能，供其他微服务通过 RPC 调用。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface LogisticsDubboService {

    /**
     * 创建运单
     *
     * @param request 运单请求
     * @return 运单信息
     */
    WaybillVO createWaybill(WaybillRequest request);

    /**
     * 根据 ID 查询运单
     *
     * @param id 运单 ID
     * @return 运单信息，不存在时返回 null
     */
    WaybillVO getWaybillById(Long id);

    /**
     * 更新物流跟踪状态
     *
     * @param waybillId 运单 ID
     * @param status 新状态
     */
    void updateTrackingStatus(Long waybillId, String status);

    /**
     * 运单请求
     */
    class WaybillRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long orderId;
        private Long warehouseId;
        private String receiverName;
        private String receiverPhone;
        private String receiverAddress;
        private String carrier;

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
        }

        public String getReceiverName() {
            return receiverName;
        }

        public void setReceiverName(String receiverName) {
            this.receiverName = receiverName;
        }

        public String getReceiverPhone() {
            return receiverPhone;
        }

        public void setReceiverPhone(String receiverPhone) {
            this.receiverPhone = receiverPhone;
        }

        public String getReceiverAddress() {
            return receiverAddress;
        }

        public void setReceiverAddress(String receiverAddress) {
            this.receiverAddress = receiverAddress;
        }

        public String getCarrier() {
            return carrier;
        }

        public void setCarrier(String carrier) {
            this.carrier = carrier;
        }
    }

    /**
     * 运单信息
     */
    class WaybillVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private String waybillNo;
        private Long orderId;
        private Long warehouseId;
        private String receiverName;
        private String receiverPhone;
        private String receiverAddress;
        private String carrier;
        private String trackingNo;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getWaybillNo() {
            return waybillNo;
        }

        public void setWaybillNo(String waybillNo) {
            this.waybillNo = waybillNo;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
        }

        public String getReceiverName() {
            return receiverName;
        }

        public void setReceiverName(String receiverName) {
            this.receiverName = receiverName;
        }

        public String getReceiverPhone() {
            return receiverPhone;
        }

        public void setReceiverPhone(String receiverPhone) {
            this.receiverPhone = receiverPhone;
        }

        public String getReceiverAddress() {
            return receiverAddress;
        }

        public void setReceiverAddress(String receiverAddress) {
            this.receiverAddress = receiverAddress;
        }

        public String getCarrier() {
            return carrier;
        }

        public void setCarrier(String carrier) {
            this.carrier = carrier;
        }

        public String getTrackingNo() {
            return trackingNo;
        }

        public void setTrackingNo(String trackingNo) {
            this.trackingNo = trackingNo;
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

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
