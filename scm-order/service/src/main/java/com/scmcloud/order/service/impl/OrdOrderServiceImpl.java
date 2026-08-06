package com.scmcloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.system.api.StatusMachineDubboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrdOrderItem;
import com.scmcloud.order.domain.entity.OrdStatusHistory;
import com.scmcloud.order.mapper.OrdOrderMapper;
import com.scmcloud.order.service.IOrdOrderItemService;
import com.scmcloud.order.service.IOrdOrderService;
import com.scmcloud.order.service.IOrdStatusHistoryService;

import com.scmcloud.common.domain.Money;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrdOrderServiceImpl extends ServiceImpl<OrdOrderMapper, OrdOrder> implements IOrdOrderService {

    private final IOrdOrderItemService orderItemService;
    private final IOrdStatusHistoryService statusHistoryService;

    @DubboReference
    private StatusMachineDubboService statusMachine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrdOrder createOrder(OrdOrder order, List<OrdOrderItem> items) {
        log.info("创建订单: orderNo={}, userId={}", order.getOrderNo(), order.getUserId());

        if (CollectionUtils.isEmpty(items)) {
            throw new IllegalArgumentException("订单明细不能为空");
        }

        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setDeleted(false);

        if (order.getTotalAmount() == null) {
            Money totalAmount = items.stream()
                    .map(OrdOrderItem::getSubtotal)
                    .reduce(Money.ZERO, Money::add);
            order.setTotalAmount(totalAmount);
        }

        if (order.getPayableAmount() == null) {
            Money discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : Money.ZERO;
            Money freight = order.getFreightAmount() != null ? order.getFreightAmount() : Money.ZERO;
            Money payable = order.getTotalAmount().subtract(discount).add(freight);
            order.setPayableAmount(payable);
        }

        boolean saved = save(order);
        if (!saved) {
            throw new RuntimeException("创建订单失败");
        }

        for (OrdOrderItem item : items) {
            item.setOrderId(order.getId());
            item.setOrderNo(order.getOrderNo());
            item.setCreateTime(LocalDateTime.now());
        }
        orderItemService.saveBatch(items);

        OrdStatusHistory history = new OrdStatusHistory();
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
        log.info("更新订单状态 orderId={}, status={}", orderId, status);

        OrdOrder order = getById(orderId);
        if (order == null) {
            log.warn("订单不存在 orderId={}", orderId);
            return false;
        }

        Integer fromStatus = order.getStatus();
        OrderStatus targetStatus = OrderStatus.fromCode(status);

        // 通过状态机验证流转合法性
        String fromName = OrderStatus.fromCode(fromStatus).name();
        String toName = targetStatus.name();
        StatusMachineDubboService.TransitionCheckDTO check =
                statusMachine.canTransition("ORDER", fromName, toName);
        if (!check.allowed()) {
            log.warn("非法状态流转: orderId={}, {} -> {}, reason={}", orderId, fromName, toName, check.reason());
            throw new IllegalStateException("非法状态流转: " + fromName + " -> " + toName + ": " + check.reason());
        }

        order.transitionTo(targetStatus);

        boolean updated = updateById(order);
        if (updated) {
            OrdStatusHistory history = new OrdStatusHistory();
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
    public List<OrdOrder> listByUserId(String userId) {
        log.debug("查询用户订单: userId={}", userId);
        LambdaQueryWrapper<OrdOrder> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdOrder::getUserId, userId)
                .eq(OrdOrder::getDeleted, false)
                .orderByDesc(OrdOrder::getCreateTime);
        return list(wrapper);
    }

    @Override
    public Page<OrdOrder> pageByUserId(String userId, Integer pageNum, Integer pageSize) {
        log.debug("分页查询用户订单: userId={}, pageNum={}, pageSize={}", userId, pageNum, pageSize);
        LambdaQueryWrapper<OrdOrder> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdOrder::getUserId, userId)
                .eq(OrdOrder::getDeleted, false)
                .orderByDesc(OrdOrder::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }
}
