package scm.purchase.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scm.purchase.domain.entity.PurOrderItem;
import scm.purchase.mapper.PurOrderItemMapper;
import scm.purchase.service.IPurOrderItemService;

import java.util.List;

@Slf4j
@Service
public class PurOrderItemServiceImpl extends ServiceImpl<PurOrderItemMapper, PurOrderItem> implements IPurOrderItemService {

    @Override
    public List<PurOrderItem> listByOrderId(String orderId) {
        return lambdaQuery()
                .eq(PurOrderItem::getOrderId, orderId)
                .orderByAsc(PurOrderItem::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByOrderId(String orderId) {
        return lambdaUpdate()
                .eq(PurOrderItem::getOrderId, orderId)
                .remove();
    }
}
