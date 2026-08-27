package com.scmcloud.order.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.order.domain.entity.OrdStatusHistory;
import com.scmcloud.order.mapper.OrdStatusHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdStatusHistoryQueryService {
    private final OrdStatusHistoryMapper ordStatusHistoryMapper;

    @Slave
    public OrdStatusHistory getById(String id) {
        return ordStatusHistoryMapper.selectById(id);
    }

    @Slave
    public List<OrdStatusHistory> listByOrderId(UUID orderId) {
        LambdaQueryWrapper<OrdStatusHistory> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdStatusHistory::getOrderId, orderId)
                .orderByDesc(OrdStatusHistory::getTransitionedAt);
        return ordStatusHistoryMapper.selectList(wrapper);
    }
}
