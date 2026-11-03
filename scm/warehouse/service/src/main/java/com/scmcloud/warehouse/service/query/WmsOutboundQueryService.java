package com.scmcloud.warehouse.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.warehouse.domain.entity.WmsOutbound;
import com.scmcloud.warehouse.mapper.WmsOutboundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WmsOutboundQueryService {

    private final WmsOutboundMapper outboundMapper;

    @Slave
    public WmsOutbound getById(String id) {
        return outboundMapper.selectById(id);
    }

    @Slave
    public Page<WmsOutbound> pageList(int page, int size, String warehouseId, Integer outboundType, Integer status) {
        LambdaQueryWrapper<WmsOutbound> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WmsOutbound::getDeleted, false);
        if (StringUtils.hasText(warehouseId)) {
            wrapper.eq(WmsOutbound::getWarehouseId, warehouseId);
        }
        if (outboundType != null) {
            wrapper.eq(WmsOutbound::getOutboundType, outboundType);
        }
        if (status != null) {
            wrapper.eq(WmsOutbound::getStatus, status);
        }
        wrapper.orderByDesc(WmsOutbound::getCreateTime);
        return outboundMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
