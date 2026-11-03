package com.scmcloud.warehouse.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.warehouse.domain.entity.WmsWavePicking;
import com.scmcloud.warehouse.mapper.WmsWavePickingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WmsWavePickingQueryService {

    private final WmsWavePickingMapper wavePickingMapper;

    @Slave
    public WmsWavePicking getById(String id) {
        return wavePickingMapper.selectById(id);
    }

    @Slave
    public Page<WmsWavePicking> pageList(int page, int size, String warehouseId, Integer status) {
        LambdaQueryWrapper<WmsWavePicking> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(warehouseId)) {
            wrapper.eq(WmsWavePicking::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(WmsWavePicking::getStatus, status);
        }
        wrapper.orderByDesc(WmsWavePicking::getCreateTime);
        return wavePickingMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
