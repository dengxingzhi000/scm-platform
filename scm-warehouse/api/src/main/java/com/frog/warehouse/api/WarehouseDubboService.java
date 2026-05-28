package com.frog.warehouse.api;

import java.io.Serializable;
import java.util.List;

/**
 * 仓库服务 Dubbo 接口
 *
 * <p>提供仓库查询、库位管理等核心功能，供其他微服务通过 RPC 调用。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface WarehouseDubboService {

    /**
     * 根据 ID 查询仓库
     *
     * @param id 仓库 ID
     * @return 仓库信息，不存在时返回 null
     */
    WarehouseVO getWarehouseById(Long id);

    /**
     * 查询所有仓库列表
     *
     * @return 仓库列表
     */
    List<WarehouseVO> listWarehouses();

    /**
     * 查询仓库下可用库位
     *
     * @param warehouseId 仓库 ID
     * @return 可用库位列表
     */
    List<LocationVO> getAvailableLocations(Long warehouseId);

    /**
     * 仓库信息
     */
    class WarehouseVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private String name;
        private String code;
        private String address;
        private String contactPerson;
        private String contactPhone;
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

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getContactPerson() {
            return contactPerson;
        }

        public void setContactPerson(String contactPerson) {
            this.contactPerson = contactPerson;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public void setContactPhone(String contactPhone) {
            this.contactPhone = contactPhone;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    /**
     * 库位信息
     */
    class LocationVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private Long warehouseId;
        private String locationCode;
        private String zone;
        private Integer capacity;
        private Integer usedCapacity;
        private Integer status;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
        }

        public String getLocationCode() {
            return locationCode;
        }

        public void setLocationCode(String locationCode) {
            this.locationCode = locationCode;
        }

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }

        public Integer getCapacity() {
            return capacity;
        }

        public void setCapacity(Integer capacity) {
            this.capacity = capacity;
        }

        public Integer getUsedCapacity() {
            return usedCapacity;
        }

        public void setUsedCapacity(Integer usedCapacity) {
            this.usedCapacity = usedCapacity;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }
}
