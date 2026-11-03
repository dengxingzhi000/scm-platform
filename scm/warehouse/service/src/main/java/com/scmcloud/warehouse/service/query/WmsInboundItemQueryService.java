package com.scmcloud.warehouse.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.warehouse.domain.entity.WmsInboundItem;
import com.scmcloud.warehouse.mapper.WmsInboundItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WmsInboundItemQueryService {

    private final WmsInboundItemMapper inboundItemMapper;

    @Slave
    public WmsInboundItem getById(String id) {
        return inboundItemMapper.selectById(id);
    }

    @Slave
    public List<WmsInboundItem> listByInboundId(String inboundId) {
        LambdaQueryWrapper<WmsInboundItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WmsInboundItem::getInboundId, inboundId);
        wrapper.eq(WmsInboundItem::getDeleted, false);
        return inboundItemMapper.selectList(wrapper);
    }
}
