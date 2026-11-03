package com.scmcloud.warehouse.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.warehouse.domain.entity.WmsOutboundItem;
import com.scmcloud.warehouse.mapper.WmsOutboundItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WmsOutboundItemQueryService {

    private final WmsOutboundItemMapper outboundItemMapper;

    @Slave
    public WmsOutboundItem getById(String id) {
        return outboundItemMapper.selectById(id);
    }

    @Slave
    public List<WmsOutboundItem> listByOutboundId(String outboundId) {
        LambdaQueryWrapper<WmsOutboundItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WmsOutboundItem::getOutboundId, outboundId);
        wrapper.eq(WmsOutboundItem::getDeleted, false);
        return outboundItemMapper.selectList(wrapper);
    }
}
