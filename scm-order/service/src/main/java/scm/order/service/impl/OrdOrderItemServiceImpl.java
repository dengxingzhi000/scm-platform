package scm.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import scm.order.domain.entity.OrdOrderItem;
import scm.order.mapper.OrdOrderItemMapper;
import scm.order.service.IOrdOrderItemService;

import java.util.List;

@Slf4j
@Service
public class OrdOrderItemServiceImpl extends ServiceImpl<OrdOrderItemMapper, OrdOrderItem> implements IOrdOrderItemService {

    @Override
    public List<OrdOrderItem> listByOrderId(Long orderId) {
        log.debug("查询订单明细: orderId={}", orderId);
        LambdaQueryWrapper<OrdOrderItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdOrderItem::getOrderId, orderId)
                .orderByAsc(OrdOrderItem::getCreateTime);
        return list(wrapper);
    }
}
