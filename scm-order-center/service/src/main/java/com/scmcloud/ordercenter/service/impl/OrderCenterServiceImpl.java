package com.scmcloud.ordercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.ordercenter.domain.entity.OcOrder;
import com.scmcloud.ordercenter.domain.entity.OcOrderItem;
import com.scmcloud.ordercenter.mapper.OcOrderMapper;
import com.scmcloud.ordercenter.service.IOrderCenterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderCenterServiceImpl extends ServiceImpl<OcOrderMapper, OcOrder> implements IOrderCenterService {

    private final com.scmcloud.ordercenter.mapper.OcOrderItemMapper orderItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OcOrder createOrder(OcOrder order, List<OcOrderItem> items) {
        log.info("Creating order: orderNo={}, userId={}", order.getOrderNo(), order.getUserId());

        order.setStatus(1);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        if (order.getTotalAmount() == null) {
            BigDecimal totalAmount = items.stream()
                    .map(OcOrderItem::getPayableAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setTotalAmount(totalAmount);
        }

        if (order.getPayableAmount() == null) {
            BigDecimal payable = order.getTotalAmount()
                    .subtract(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO)
                    .add(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO);
            order.setPayableAmount(payable);
        }

        save(order);

        for (OcOrderItem item : items) {
            item.setOrderNo(order.getOrderNo());
            item.setCreatedAt(LocalDateTime.now());
        }

        orderItemMapper.insert(items);

        log.info("Order created: orderNo={}", order.getOrderNo());
        return order;
    }

    @Override
    public OcOrder getOrder(String orderNo) {
        return getOne(new LambdaQueryWrapper<OcOrder>()
                .eq(OcOrder::getOrderNo, orderNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, String reason) {
        OcOrder order = getOrder(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderNo);
        }

        if (order.getStatus() != 1 && order.getStatus() != 2) {
            throw new IllegalStateException("Order cannot be cancelled, status: " + order.getStatus());
        }

        order.setStatus(6);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);

        log.info("Order cancelled: orderNo={}, reason={}", orderNo, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(String orderNo, String paymentNo) {
        OcOrder order = getOrder(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderNo);
        }

        if (order.getStatus() != 1) {
            throw new IllegalStateException("Order cannot be paid, status: " + order.getStatus());
        }

        order.setStatus(2);
        order.setPaidAmount(order.getPayableAmount());
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);

        log.info("Order paid: orderNo={}, paymentNo={}", orderNo, paymentNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(String orderNo, String logisticsNo) {
        OcOrder order = getOrder(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderNo);
        }

        if (order.getStatus() != 3) {
            throw new IllegalStateException("Order cannot be shipped, status: " + order.getStatus());
        }

        order.setStatus(4);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);

        log.info("Order shipped: orderNo={}, logisticsNo={}", orderNo, logisticsNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverOrder(String orderNo) {
        OcOrder order = getOrder(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderNo);
        }

        if (order.getStatus() != 4) {
            throw new IllegalStateException("Order cannot be delivered, status: " + order.getStatus());
        }

        order.setStatus(5);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);

        log.info("Order delivered: orderNo={}", orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(String orderNo) {
        OcOrder order = getOrder(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderNo);
        }

        if (order.getStatus() != 5) {
            throw new IllegalStateException("Order cannot be confirmed, status: " + order.getStatus());
        }

        order.setStatus(7);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);

        log.info("Order confirmed: orderNo={}", orderNo);
    }
}
