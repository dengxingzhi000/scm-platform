package com.cognora.ecom.ordercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.ecom.ordercenter.domain.entity.OcOrder;
import com.cognora.ecom.ordercenter.domain.entity.OcOrderItem;
import com.cognora.ecom.ordercenter.mapper.OcOrderMapper;
import com.cognora.ecom.ordercenter.service.IOrderCenterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderCenterServiceImpl extends ServiceImpl<OcOrderMapper, OcOrder> implements IOrderCenterService {

    private static final int MAX_PAGE_SIZE = 100;

    private final com.cognora.ecom.ordercenter.mapper.OcOrderItemMapper orderItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OcOrder createOrder(OcOrder order, List<OcOrderItem> items) {
        if (order == null || !StringUtils.hasText(order.getOrderNo())) {
            throw new IllegalArgumentException("orderNo is required");
        }
        if (!StringUtils.hasText(order.getUserId())) {
            throw new IllegalArgumentException("userId is required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("order items must not be empty");
        }
        log.info("Creating order: orderNo={}, userId={}", safe(order.getOrderNo()), safe(order.getUserId()));

        order.setStatus(1);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        for (OcOrderItem item : items) {
            if (item.getQuantity() == null || item.getQuantity() < 1) {
                throw new IllegalArgumentException("item quantity must be greater than zero");
            }
            if (item.getPayableAmount() == null) {
                if (item.getPrice() == null) {
                    throw new IllegalArgumentException("item needs a price or payable amount");
                }
                item.setPayableAmount(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            requireNonNegative(item.getPayableAmount(), "item payable amount");
        }
        if (order.getDiscountAmount() != null && order.getDiscountAmount().signum() < 0) {
            throw new IllegalArgumentException("discount amount must not be negative");
        }
        if (order.getShippingFee() != null && order.getShippingFee().signum() < 0) {
            throw new IllegalArgumentException("shipping fee must not be negative");
        }

        if (order.getTotalAmount() == null) {
            BigDecimal totalAmount = items.stream()
                    .map(OcOrderItem::getPayableAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setTotalAmount(totalAmount);
        } else {
            requireNonNegative(order.getTotalAmount(), "total amount");
        }

        if (order.getPayableAmount() == null) {
            BigDecimal payable = order.getTotalAmount()
                    .subtract(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO)
                    .add(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO);
            if (payable.signum() < 0) {
                throw new IllegalArgumentException("payable amount must not be negative");
            }
            order.setPayableAmount(payable);
        } else {
            requireNonNegative(order.getPayableAmount(), "payable amount");
        }

        save(order);

        for (OcOrderItem item : items) {
            item.setOrderNo(order.getOrderNo());
            item.setCreatedAt(LocalDateTime.now());
        }

        orderItemMapper.insert(items);

        log.info("Order created: orderNo={}", safe(order.getOrderNo()));
        return order;
    }

    @Override
    public Page<OcOrder> pageQuery(int page, int size, String orderNo, String userId, Integer status) {
        validatePage(page, size);
        LambdaQueryWrapper<OcOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(orderNo), OcOrder::getOrderNo, orderNo)
                .eq(StringUtils.hasText(userId), OcOrder::getUserId, userId)
                .eq(status != null, OcOrder::getStatus, status)
                .orderByDesc(OcOrder::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
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

        if (order.getStatus() == null || (order.getStatus() != 1 && order.getStatus() != 2)) {
            throw new IllegalStateException("Order cannot be cancelled, status: " + order.getStatus());
        }

        order.setStatus(6);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);

        log.info("Order cancelled: orderNo={}, reason={}", (orderNo + "").replace("\r", "_").replace("\n", "_"), (reason + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(String orderNo, String paymentNo) {
        OcOrder order = getOrder(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderNo);
        }

        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new IllegalStateException("Order cannot be paid, status: " + order.getStatus());
        }
        if (order.getPayableAmount() == null || order.getPayableAmount().signum() <= 0) {
            throw new IllegalStateException("Order has no payable amount: " + orderNo);
        }

        order.setStatus(2);
        order.setPaidAmount(order.getPayableAmount());
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);

        log.info("Order paid: orderNo={}, paymentNo={}", (orderNo + "").replace("\r", "_").replace("\n", "_"), (paymentNo + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(String orderNo, String logisticsNo) {
        OcOrder order = getOrder(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderNo);
        }

        if (order.getStatus() == null || (order.getStatus() != 2 && order.getStatus() != 3)) {
            throw new IllegalStateException("Order cannot be shipped, status: " + order.getStatus());
        }

        order.setStatus(4);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);

        log.info("Order shipped: orderNo={}, logisticsNo={}", (orderNo + "").replace("\r", "_").replace("\n", "_"), (logisticsNo + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverOrder(String orderNo) {
        OcOrder order = getOrder(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderNo);
        }

        if (order.getStatus() == null || order.getStatus() != 4) {
            throw new IllegalStateException("Order cannot be delivered, status: " + order.getStatus());
        }

        order.setStatus(5);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);

        log.info("Order delivered: orderNo={}", (orderNo + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(String orderNo) {
        OcOrder order = getOrder(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderNo);
        }

        if (order.getStatus() == null || order.getStatus() != 5) {
            throw new IllegalStateException("Order cannot be confirmed, status: " + order.getStatus());
        }

        order.setStatus(7);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);

        log.info("Order confirmed: orderNo={}", (orderNo + "").replace("\r", "_").replace("\n", "_"));
    }

    private void requireNonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than or equal to 1");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private String safe(String value) {
        return (value + "").replace("\r", "_").replace("\n", "_");
    }
}
