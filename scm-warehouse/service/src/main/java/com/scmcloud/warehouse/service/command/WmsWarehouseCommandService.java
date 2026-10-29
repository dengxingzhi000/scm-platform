package com.scmcloud.warehouse.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.warehouse.domain.entity.WmsWarehouse;
import com.scmcloud.warehouse.mapper.WmsWarehouseMapper;
import com.scmcloud.warehouse.service.query.WmsWarehouseQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsWarehouseCommandService {

    private final WmsWarehouseMapper warehouseMapper;
    private final WmsWarehouseQueryService warehouseQueryService;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public WmsWarehouse create(WmsWarehouse warehouse) {
        if (warehouseQueryService.existsByWarehouseCode(warehouse.getWarehouseCode())) {
            throw new IllegalStateException("仓库编码已存在: " + warehouse.getWarehouseCode());
        }
        warehouse.setId(UUIDv7Util.generateString());
        warehouse.setEnabled(true);
        warehouse.setUsedCapacity(0);
        warehouse.setDeleted(false);
        warehouse.setCreateTime(LocalDateTime.now());
        warehouse.setUpdateTime(LocalDateTime.now());
        warehouseMapper.insert(warehouse);
        log.info("仓库创建成功: id={}, code={}", (warehouse.getId() + "").replace("\r", "_").replace("\n", "_"), (warehouse.getWarehouseCode() + "").replace("\r", "_").replace("\n", "_"));
        return warehouse;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WmsWarehouse warehouse) {
        WmsWarehouse existing = warehouseMapper.selectById(warehouse.getId());
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return false;
        }
        warehouse.setUpdateTime(LocalDateTime.now());
        return warehouseMapper.updateById(warehouse) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsWarehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null || Boolean.TRUE.equals(warehouse.getDeleted())) {
            return false;
        }
        warehouse.setDeleted(true);
        warehouse.setUpdateTime(LocalDateTime.now());
        return warehouseMapper.updateById(warehouse) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean enable(String id) {
        WmsWarehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            log.warn("[enable] 仓库不存在: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
            return false;
        }
        warehouse.setEnabled(true);
        warehouse.setUpdateTime(LocalDateTime.now());
        boolean success = warehouseMapper.updateById(warehouse) > 0;
        if (success) {
            log.info("仓库已启用: id={}, name={}", (id + "").replace("\r", "_").replace("\n", "_"), (warehouse.getWarehouseName() + "").replace("\r", "_").replace("\n", "_"));
        }
        return success;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean disable(String id) {
        WmsWarehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            log.warn("[disable] 仓库不存在: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
            return false;
        }
        warehouse.setEnabled(false);
        warehouse.setUpdateTime(LocalDateTime.now());
        boolean success = warehouseMapper.updateById(warehouse) > 0;
        if (success) {
            log.info("仓库已停用: id={}, name={}", (id + "").replace("\r", "_").replace("\n", "_"), (warehouse.getWarehouseName() + "").replace("\r", "_").replace("\n", "_"));
        }
        return success;
    }
}
