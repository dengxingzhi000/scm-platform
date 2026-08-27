package com.scmcloud.order.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrdOrderItem;
import com.scmcloud.order.domain.entity.OrdStatusHistory;
import com.scmcloud.order.event.OrderCreatedEvent;
import com.scmcloud.order.event.OrderEventStore;
import com.scmcloud.order.event.OrderStatusChangedEvent;
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
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrdOrderCommandService {
    private final OrdOrderMapper ordOrderMapper;
    private final OrdOrderItemCommandService ordOrderItemCommandService;
    private final OrdStatusHistoryCommandService ordStatusHistoryCommandService;
    private final OrderEventStore eventStore;

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

        eventStore.append(new OrderCreatedEvent(
                order.getTenantId() != null ? order.getTenantId().toUUID() : null,
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                order.getTotalAmount() != null ? order.getTotalAmount().getAmount() : null,
                order.getPayableAmount() != null ? order.getPayableAmount().getAmount() : null));

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
    public boolean updateOrderStatus(UUID orderId, Integer status) {
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

            eventStore.append(new OrderStatusChangedEvent(
                    order.getTenantId() != null ? order.getTenantId().toUUID() : null,
                    order.getId(),
                    order.getOrderNo(),
                    OrderStatus.fromCode(fromStatus),
                    targetStatus));
        }

        return updated > 0;
    }

    @Master(reason = "删除订单")
    @Transactional(rollbackFor = Exception.class)
    public int removeById(UUID id) {
        return ordOrderMapper.deleteById(id);
    }

    /**
     * 取消订单（用户主动取消）。
     *
     * <p>复用领域取消逻辑（状态校验 + 状态流转 + 取消元数据），并通过状态机校验流转合法性，
     * 在同一事务内写入 {@code ord_status_history} 与 {@code ORDER_STATUS_CHANGED} 事件，
     * 供下游补偿库存、退款、通知等使用。</p>
     *
     * @param orderId 订单 ID
     * @param reason  取消原因
     * @return 取消成功返回 {@code true}；订单不存在返回 {@code false}
     */
    @Master(reason = "取消订单")
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(UUID orderId, String reason) {
        log.info("取消订单: orderId={}, reason={}", orderId, reason);

        OrdOrder existing = ordOrderMapper.selectById(orderId);
        if (existing == null) {
            log.warn("订单不存在: orderId={}", orderId);
            return false;
        }

        Integer fromStatus = existing.getStatus();
        String fromName = existing.getStatusEnum().name();
        String toName = OrderStatus.CANCELLED.name();

        StatusMachineDubboService.TransitionCheckDTO check =
                statusMachine.canTransition("ORDER", fromName, toName);
        if (!check.allowed()) {
            log.warn("非法状态流转: orderId={}, {} -> {}, reason={}", orderId, fromName, toName, check.reason());
            throw new IllegalStateException("非法状态流转: " + fromName + " -> " + toName + ": " + check.reason());
        }

        OrderStatus previousStatus = existing.getStatusEnum();
        existing.cancel(reason);
        ordOrderMapper.updateById(existing);

        OrdStatusHistory history = new OrdStatusHistory();
        history.setOrderId(existing.getId());
        history.setOrderNo(existing.getOrderNo());
        history.setFromStatus(fromStatus);
        history.setToStatus(OrderStatus.CANCELLED.getCode());
        history.setEvent("ORDER_CANCELLED");
        history.setOperatorId(existing.getUpdateBy());
        history.setTransitionedAt(LocalDateTime.now());
        ordStatusHistoryCommandService.save(history);

        eventStore.append(new OrderStatusChangedEvent(
                existing.getTenantId() != null ? existing.getTenantId().toUUID() : null,
                existing.getId(),
                existing.getOrderNo(),
                previousStatus,
                OrderStatus.CANCELLED));

        log.info("订单已取消: orderNo={}, id={}", existing.getOrderNo(), existing.getId());
        return true;
    }

    @Master(reason = "创建订单")
    @Transactional(rollbackFor = Exception.class)
    public int saveBatch(List<OrdOrder> list) {
        return list.stream().map(ordOrderMapper::insert).reduce(0, Integer::sum);
    }

    /**
     * 取消超时订单。
     *
     * <p>复用领域取消逻辑（状态校验 + 状态流转 + 取消元数据），并通过
     * {@code OrderEventStore} 在同一事务内写入 {@code ORDER_STATUS_CHANGED} 事件，
     * 供下游补偿库存等使用。</p>
     *
     * @param order 待取消的订单（须已持久化，含 id）
     */
    @Master(reason = "超时取消订单")
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutOrder(OrdOrder order) {
        if (order == null || order.getId() == null) {
            throw new IllegalArgumentException("待取消订单不能为空且必须包含 id");
        }

        OrdOrder existing = ordOrderMapper.selectById(order.getId());
        if (existing == null) {
            throw new IllegalArgumentException("订单不存在: id=" + order.getId());
        }

        OrderStatus previousStatus = existing.getStatusEnum();
        existing.cancel("订单超时未支付，系统自动取消");

        ordOrderMapper.updateById(existing);

        eventStore.append(new OrderStatusChangedEvent(
                existing.getTenantId() != null ? existing.getTenantId().toUUID() : null,
                existing.getId(),
                existing.getOrderNo(),
                previousStatus,
                OrderStatus.CANCELLED));

        log.info("超时订单已取消: orderNo={}, id={}", existing.getOrderNo(), existing.getId());
    }
}
