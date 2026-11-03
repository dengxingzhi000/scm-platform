package com.scmcloud.warehouse.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.warehouse.domain.entity.WmsLocation;
import com.scmcloud.warehouse.mapper.WmsLocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WmsLocationQueryService {

    private final WmsLocationMapper locationMapper;

    @Slave
    public WmsLocation getById(String id) {
        return locationMapper.selectById(id);
    }

    @Slave
    public List<WmsLocation> listByWarehouseId(String warehouseId) {
        LambdaQueryWrapper<WmsLocation> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WmsLocation::getWarehouseId, warehouseId);
        wrapper.eq(WmsLocation::getDeleted, false);
        wrapper.orderByAsc(WmsLocation::getZone)
                .orderByAsc(WmsLocation::getShelf)
                .orderByAsc(WmsLocation::getLayer)
                .orderByAsc(WmsLocation::getPosition);
        return locationMapper.selectList(wrapper);
    }

    @Slave
    public Page<WmsLocation> pageList(int page, int size, String warehouseId, Integer locationType, Integer status) {
        LambdaQueryWrapper<WmsLocation> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WmsLocation::getDeleted, false);
        if (StringUtils.hasText(warehouseId)) {
            wrapper.eq(WmsLocation::getWarehouseId, warehouseId);
        }
        if (locationType != null) {
            wrapper.eq(WmsLocation::getLocationType, locationType);
        }
        if (status != null) {
            wrapper.eq(WmsLocation::getStatus, status);
        }
        wrapper.orderByAsc(WmsLocation::getZone)
                .orderByAsc(WmsLocation::getShelf)
                .orderByAsc(WmsLocation::getLayer)
                .orderByAsc(WmsLocation::getPosition);
        return locationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public boolean existsByWarehouseIdAndCode(String warehouseId, String locationCode) {
        LambdaQueryWrapper<WmsLocation> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WmsLocation::getWarehouseId, warehouseId);
        wrapper.eq(WmsLocation::getLocationCode, locationCode);
        wrapper.eq(WmsLocation::getDeleted, false);
        return locationMapper.selectCount(wrapper) > 0;
    }
}
