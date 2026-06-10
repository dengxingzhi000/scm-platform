package com.scmcloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.order.domain.entity.OrdStatusHistory;
import com.scmcloud.order.mapper.OrdStatusHistoryMapper;
import com.scmcloud.order.service.IOrdStatusHistoryService;

import java.util.List;

@Slf4j
@Service
public class OrdStatusHistoryServiceImpl extends ServiceImpl<OrdStatusHistoryMapper, OrdStatusHistory> implements IOrdStatusHistoryService {

    @Override
    public List<OrdStatusHistory> listByOrderId(Long orderId) {
        log.debug("Query order status history: orderId={}", orderId);
        LambdaQueryWrapper<OrdStatusHistory> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdStatusHistory::getOrderId, orderId)
                .orderByDesc(OrdStatusHistory::getTransitionedAt);
        return list(wrapper);
    }
}
