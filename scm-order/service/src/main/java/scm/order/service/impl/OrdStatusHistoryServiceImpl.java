package scm.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import scm.order.domain.entity.OrdStatusHistory;
import scm.order.mapper.OrdStatusHistoryMapper;
import scm.order.service.IOrdStatusHistoryService;

import java.util.List;

@Slf4j
@Service
public class OrdStatusHistoryServiceImpl extends ServiceImpl<OrdStatusHistoryMapper, OrdStatusHistory> implements IOrdStatusHistoryService {

    @Override
    public List<OrdStatusHistory> listByOrderId(Long orderId) {
        log.debug("查询订单状态历史: orderId={}", orderId);
        LambdaQueryWrapper<OrdStatusHistory> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdStatusHistory::getOrderId, orderId)
                .orderByDesc(OrdStatusHistory::getTransitionedAt);
        return list(wrapper);
    }
}
