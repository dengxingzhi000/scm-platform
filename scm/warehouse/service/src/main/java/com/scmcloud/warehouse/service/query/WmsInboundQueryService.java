package com.scmcloud.warehouse.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.warehouse.domain.entity.WmsInbound;
import com.scmcloud.warehouse.mapper.WmsInboundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WmsInboundQueryService {

    private final WmsInboundMapper inboundMapper;

    @Slave
    public WmsInbound getById(String id) {
        return inboundMapper.selectById(id);
    }

    @Slave
    public Page<WmsInbound> pageList(int page, int size, String warehouseId, Integer inboundType, Integer status) {
        LambdaQueryWrapper<WmsInbound> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WmsInbound::getDeleted, false);
        if (StringUtils.hasText(warehouseId)) {
            wrapper.eq(WmsInbound::getWarehouseId, warehouseId);
        }
        if (inboundType != null) {
            wrapper.eq(WmsInbound::getInboundType, inboundType);
        }
        if (status != null) {
            wrapper.eq(WmsInbound::getStatus, status);
        }
        wrapper.orderByDesc(WmsInbound::getCreateTime);
        return inboundMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
