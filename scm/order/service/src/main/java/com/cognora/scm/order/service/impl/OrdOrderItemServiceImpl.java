package com.cognora.scm.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.cognora.scm.order.domain.entity.OrdOrderItem;
import com.cognora.scm.order.mapper.OrdOrderItemMapper;
import com.cognora.scm.order.service.IOrdOrderItemService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OrdOrderItemServiceImpl extends ServiceImpl<OrdOrderItemMapper, OrdOrderItem> implements IOrdOrderItemService {

    @Override
    public List<OrdOrderItem> listByOrderId(UUID orderId) {
        log.debug("查询订单明细: orderId={}", orderId);
        LambdaQueryWrapper<OrdOrderItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdOrderItem::getOrderId, orderId)
                .orderByAsc(OrdOrderItem::getCreateTime);
        return list(wrapper);
    }
}
