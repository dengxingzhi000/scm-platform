package scm.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import scm.order.domain.entity.OrdOrder;
import scm.order.domain.entity.OrdOrderItem;
import scm.order.domain.entity.OrdStatusHistory;
import scm.order.mapper.OrdOrderMapper;
import scm.order.service.IOrdOrderItemService;
import scm.order.service.IOrdOrderService;
import scm.order.service.IOrdStatusHistoryService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrdOrderServiceImpl extends ServiceImpl<OrdOrderMapper, OrdOrder> implements IOrdOrderService {

    private final IOrdOrderItemService orderItemService;

    private final IOrdStatusHistoryService statusHistoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrdOrder createOrder(OrdOrder order, List<OrdOrderItem> items) {
        log.info("创建订单: orderNo={}, userId={}", order.getOrderNo(), order.getUserId());

        if (CollectionUtils.isEmpty(items)) {
            throw new IllegalArgumentException("订单明细不能为空");
        }

        order.setId(UUID.randomUUID().toString());
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setDeleted(false);

        if (order.getTotalAmount() == null) {
            BigDecimal totalAmount = items.stream()
                    .map(OrdOrderItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setTotalAmount(totalAmount);
        }

        if (order.getPayableAmount() == null) {
            BigDecimal payable = order.getTotalAmount()
                    .subtract(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO)
                    .add(order.getFreightAmount() != null ? order.getFreightAmount() : BigDecimal.ZERO);
            order.setPayableAmount(payable);
        }

        boolean saved = save(order);
        if (!saved) {
            throw new RuntimeException("创建订单失败");
        }

        for (OrdOrderItem item : items) {
            item.setId(UUID.randomUUID().toString());
            item.setOrderId(order.getId());
            item.setOrderNo(order.getOrderNo());
            item.setCreateTime(LocalDateTime.now());
        }
        orderItemService.saveBatch(items);

        OrdStatusHistory history = new OrdStatusHistory();
        history.setId(UUID.randomUUID().toString());
        history.setOrderId(order.getId());
        history.setOrderNo(order.getOrderNo());
        history.setFromStatus(null);
        history.setToStatus(0);
        history.setEvent("ORDER_CREATED");
        history.setOperatorId(order.getCreateBy());
        history.setTransitionedAt(LocalDateTime.now());
        statusHistoryService.save(history);

        log.info("订单创建成功: id={}, orderNo={}", order.getId(), order.getOrderNo());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrderStatus(Long orderId, Integer status) {
        log.info("更新订单状态: orderId={}, status={}", orderId, status);

        OrdOrder order = getById(orderId);
        if (order == null) {
            log.warn("订单不存在: orderId={}", orderId);
            return false;
        }

        Integer fromStatus = order.getStatus();
        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());

        if (status == 6) {
            order.setCompletedAt(LocalDateTime.now());
        } else if (status == 7) {
            order.setCancelledAt(LocalDateTime.now());
        }

        boolean updated = updateById(order);
        if (updated) {
            OrdStatusHistory history = new OrdStatusHistory();
            history.setId(UUID.randomUUID().toString());
            history.setOrderId(order.getId());
            history.setOrderNo(order.getOrderNo());
            history.setFromStatus(fromStatus);
            history.setToStatus(status);
            history.setEvent("STATUS_CHANGED");
            history.setTransitionedAt(LocalDateTime.now());
            statusHistoryService.save(history);
        }

        return updated;
    }

    @Override
    public List<OrdOrder> listByUserId(Long userId) {
        log.debug("查询用户订单: userId={}", userId);
        LambdaQueryWrapper<OrdOrder> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdOrder::getUserId, userId)
                .eq(OrdOrder::getDeleted, false)
                .orderByDesc(OrdOrder::getCreateTime);
        return list(wrapper);
    }

    @Override
    public Page<OrdOrder> pageByUserId(Long userId, Integer pageNum, Integer pageSize) {
        log.debug("分页查询用户订单: userId={}, pageNum={}, pageSize={}", userId, pageNum, pageSize);
        LambdaQueryWrapper<OrdOrder> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdOrder::getUserId, userId)
                .eq(OrdOrder::getDeleted, false)
                .orderByDesc(OrdOrder::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }
}
