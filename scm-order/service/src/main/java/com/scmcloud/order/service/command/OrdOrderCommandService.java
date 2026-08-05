package com.scmcloud.order.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrdOrderItem;
import com.scmcloud.order.domain.entity.OrdStatusHistory;
import com.scmcloud.order.mapper.OrdOrderMapper;
import com.scmcloud.system.api.StatusMachineDubboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.scmcloud.common.domain.Money;
import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrdOrderCommandService {
    private final OrdOrderMapper ordOrderMapper;
    private final OrdOrderItemCommandService ordOrderItemCommandService;
    private final OrdStatusHistoryCommandService ordStatusHistoryCommandService;

    @DubboReference
    private StatusMachineDubboService statusMachine;

    @Master(reason = "创建订单")
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

        int saved = ordOrderMapper.insert(order);
        if (saved <= 0) {
            throw new RuntimeException("创建订单失败");
        }

        for (OrdOrderItem item : items) {
            item.setOrderId(order.getId());
            item.setOrderNo(order.getOrderNo());
            item.setCreateTime(LocalDateTime.now());
        }
        ordOrderItemCommandService.saveBatch(items);

        OrdStatusHistory history = new OrdStatusHistory();
        history.setOrderId(order.getId());
        history.setOrderNo(order.getOrderNo());
        history.setFromStatus(null);
        history.setToStatus(0);
        history.setEvent("ORDER_CREATED");
        history.setOperatorId(order.getCreateBy());
        history.setTransitionedAt(LocalDateTime.now());
        ordStatusHistoryCommandService.save(history);

        log.info("订单创建成功: id={}, orderNo={}", order.getId(), order.getOrderNo());
        return order;
    }

    @Master(reason = "保存订单")
    @Transactional(rollbackFor = Exception.class)
    public int save(OrdOrder order) {
        return ordOrderMapper.insert(order);
    }

    @Master(reason = "更新订单")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(OrdOrder order) {
        return ordOrderMapper.updateById(order);
    }

    @Master(reason = "更新订单状态")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrderStatus(Long orderId, Integer status) {
        log.info("更新订单状态: orderId={}, status={}", orderId, status);

        OrdOrder order = ordOrderMapper.selectById(orderId);
        if (order == null) {
            log.warn("订单不存在: orderId={}", orderId);
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

        int updated = ordOrderMapper.updateById(order);
        if (updated > 0) {
            OrdStatusHistory history = new OrdStatusHistory();
            history.setOrderId(order.getId());
            history.setOrderNo(order.getOrderNo());
            history.setFromStatus(fromStatus);
            history.setToStatus(status);
            history.setEvent("STATUS_CHANGED");
            history.setTransitionedAt(LocalDateTime.now());
            ordStatusHistoryCommandService.save(history);
        }

        return updated > 0;
    }

    @Master(reason = "删除订单")
    @Transactional(rollbackFor = Exception.class)
    public int removeById(Long id) {
        return ordOrderMapper.deleteById(id);
    }

    @Master(reason = "创建订单")
    @Transactional(rollbackFor = Exception.class)
    public int saveBatch(List<OrdOrder> list) {
        return list.stream().map(ordOrderMapper::insert).reduce(0, Integer::sum);
    }
}
