package com.scmcloud.purchase.service.command;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.purchase.domain.entity.PurOrderItem;
import com.scmcloud.purchase.mapper.PurOrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurOrderItemCommandService {

    private final PurOrderItemMapper purOrderItemMapper;

    @Master(reason = "保存采购订单明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurOrderItem entity) {
        return purOrderItemMapper.insert(entity) > 0;
    }

    @Master(reason = "更新采购订单明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurOrderItem entity) {
        return purOrderItemMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除采购订单明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purOrderItemMapper.deleteById(id) > 0;
    }

    @Master(reason = "根据订单ID删除明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByOrderId(String orderId) {
        LambdaUpdateWrapper<PurOrderItem> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(PurOrderItem::getOrderId, orderId);
        return purOrderItemMapper.delete(wrapper) > 0;
    }
}
