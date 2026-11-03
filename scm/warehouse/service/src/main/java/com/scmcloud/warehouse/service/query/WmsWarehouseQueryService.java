package com.scmcloud.warehouse.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.warehouse.domain.entity.WmsWarehouse;
import com.scmcloud.warehouse.mapper.WmsWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WmsWarehouseQueryService {

    private final WmsWarehouseMapper warehouseMapper;

    @Slave
    public WmsWarehouse getById(String id) {
        return warehouseMapper.selectById(id);
    }

    @Slave
    public List<WmsWarehouse> listEnabled() {
        LambdaQueryWrapper<WmsWarehouse> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WmsWarehouse::getEnabled, true);
        wrapper.eq(WmsWarehouse::getDeleted, false);
        wrapper.orderByAsc(WmsWarehouse::getSortOrder);
        return warehouseMapper.selectList(wrapper);
    }

    @Slave
    public Page<WmsWarehouse> pageList(int page, int size, String warehouseName, Integer warehouseType, Boolean enabled) {
        LambdaQueryWrapper<WmsWarehouse> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WmsWarehouse::getDeleted, false);
        if (StringUtils.hasText(warehouseName)) {
            wrapper.like(WmsWarehouse::getWarehouseName, warehouseName);
        }
        if (warehouseType != null) {
            wrapper.eq(WmsWarehouse::getWarehouseType, warehouseType);
        }
        if (enabled != null) {
            wrapper.eq(WmsWarehouse::getEnabled, enabled);
        }
        wrapper.orderByAsc(WmsWarehouse::getSortOrder);
        return warehouseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public boolean existsByWarehouseCode(String warehouseCode) {
        LambdaQueryWrapper<WmsWarehouse> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WmsWarehouse::getWarehouseCode, warehouseCode);
        wrapper.eq(WmsWarehouse::getDeleted, false);
        return warehouseMapper.selectCount(wrapper) > 0;
    }
}
