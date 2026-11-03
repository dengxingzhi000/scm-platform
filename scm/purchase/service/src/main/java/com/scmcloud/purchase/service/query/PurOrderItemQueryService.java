package com.scmcloud.purchase.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.purchase.domain.entity.PurOrderItem;
import com.scmcloud.purchase.mapper.PurOrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurOrderItemQueryService {

    private final PurOrderItemMapper purOrderItemMapper;

    @Slave
    public PurOrderItem getById(String id) {
        return purOrderItemMapper.selectById(id);
    }

    @Slave
    public List<PurOrderItem> listAll() {
        return purOrderItemMapper.selectList(null);
    }

    @Slave
    public Page<PurOrderItem> pageQuery(Page<PurOrderItem> page, Wrapper<PurOrderItem> wrapper) {
        return purOrderItemMapper.selectPage(page, wrapper);
    }

    @Slave
    public List<PurOrderItem> listByOrderId(String orderId) {
        LambdaQueryWrapper<PurOrderItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurOrderItem::getOrderId, orderId);
        wrapper.orderByAsc(PurOrderItem::getCreateTime);
        return purOrderItemMapper.selectList(wrapper);
    }
}
