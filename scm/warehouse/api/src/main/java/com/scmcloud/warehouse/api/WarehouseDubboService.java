package com.scmcloud.warehouse.api;

import com.scmcloud.warehouse.api.dto.LocationVO;
import com.scmcloud.warehouse.api.dto.WarehouseVO;

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
}
