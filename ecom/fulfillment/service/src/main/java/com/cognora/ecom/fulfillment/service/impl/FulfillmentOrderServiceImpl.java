package com.cognora.ecom.fulfillment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.ecom.fulfillment.domain.entity.FulfillmentOrder;
import com.cognora.ecom.fulfillment.domain.entity.Tracking;
import com.cognora.ecom.fulfillment.mapper.FulfillmentOrderMapper;
import com.cognora.ecom.fulfillment.service.IFulfillmentOrderService;
import com.cognora.ecom.fulfillment.service.ITrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Slf4j
@Service
public class FulfillmentOrderServiceImpl extends ServiceImpl<FulfillmentOrderMapper, FulfillmentOrder> implements IFulfillmentOrderService {

    private static final int STATUS_CREATED = 1;
    private static final int STATUS_PICKED = 2;
    private static final int STATUS_PACKED = 3;
    private static final int STATUS_SHIPPED = 4;
    private static final int STATUS_DELIVERED = 5;
    private static final int STATUS_CANCELLED = 6;
    private static final int MAX_PAGE_SIZE = 100;

    private final ITrackingService trackingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FulfillmentOrder createFulfillment(String orderNo, String userId, String fulfillmentType) {
        if (!StringUtils.hasText(orderNo)) {
            throw new IllegalArgumentException("orderNo is required");
        }
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        if (!StringUtils.hasText(fulfillmentType)) {
            throw new IllegalArgumentException("fulfillmentType is required");
        }
        log.info("Creating fulfillment: orderNo={}, userId={}, type={}", safe(orderNo), safe(userId), safe(fulfillmentType));

        FulfillmentOrder order = new FulfillmentOrder();
        order.setFulfillmentNo("FUL" + System.currentTimeMillis());
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setFulfillmentType(fulfillmentType);
        order.setStatus(STATUS_CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        save(order);
        log.info("Fulfillment created: fulfillmentNo={}", order.getFulfillmentNo());
        return order;
    }

    @Override
    public FulfillmentOrder getFulfillment(String fulfillmentNo) {
        return getOne(new LambdaQueryWrapper<FulfillmentOrder>()
                .eq(FulfillmentOrder::getFulfillmentNo, fulfillmentNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelFulfillment(String fulfillmentNo, String reason) {
        FulfillmentOrder order = requireFulfillment(fulfillmentNo);
        int status = requireStatus(order);
        if (status < STATUS_CREATED || status > STATUS_PACKED) {
            throw new IllegalStateException("Fulfillment cannot be cancelled, status: " + status);
        }

        order.setStatus(STATUS_CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
        log.info("Fulfillment cancelled: fulfillmentNo={}, reason={}", (fulfillmentNo + "").replace("\r", "_").replace("\n", "_"), (reason + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickItems(String fulfillmentNo) {
        FulfillmentOrder order = requireFulfillment(fulfillmentNo);
        requireStatus(order, STATUS_CREATED, "picked");

        order.setStatus(STATUS_PICKED);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
        log.info("Fulfillment picking: fulfillmentNo={}", (fulfillmentNo + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void packItems(String fulfillmentNo) {
        FulfillmentOrder order = requireFulfillment(fulfillmentNo);
        requireStatus(order, STATUS_PICKED, "packed");

        order.setStatus(STATUS_PACKED);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
        log.info("Fulfillment packing: fulfillmentNo={}", (fulfillmentNo + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipItems(String fulfillmentNo, String trackingNo, String carrier) {
        if (!StringUtils.hasText(trackingNo)) {
            throw new IllegalArgumentException("trackingNo is required");
        }
        if (!StringUtils.hasText(carrier)) {
            throw new IllegalArgumentException("carrier is required");
        }
        FulfillmentOrder order = requireFulfillment(fulfillmentNo);
        requireStatus(order, STATUS_PACKED, "shipped");

        order.setStatus(STATUS_SHIPPED);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
        trackingService.recordEvent(new Tracking()
                .setFulfillmentNo(fulfillmentNo)
                .setTrackingNo(trackingNo)
                .setCarrier(carrier)
                .setStatus("SHIPPED")
                .setDescription("Shipment created")
                .setEventTime(LocalDateTime.now()));
        log.info("Fulfillment shipped: fulfillmentNo={}, trackingNo={}, carrier={}", (fulfillmentNo + "").replace("\r", "_").replace("\n", "_"), (trackingNo + "").replace("\r", "_").replace("\n", "_"), (carrier + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDelivery(String fulfillmentNo) {
        FulfillmentOrder order = requireFulfillment(fulfillmentNo);
        requireStatus(order, STATUS_SHIPPED, "delivered");

        order.setStatus(STATUS_DELIVERED);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
        log.info("Fulfillment delivered: fulfillmentNo={}", (fulfillmentNo + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    public Page<FulfillmentOrder> pageQuery(int page, int size, String orderNo, String userId, Integer status) {
        validatePage(page, size);
        LambdaQueryWrapper<FulfillmentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(orderNo), FulfillmentOrder::getOrderNo, orderNo)
                .eq(StringUtils.hasText(userId), FulfillmentOrder::getUserId, userId)
                .eq(status != null, FulfillmentOrder::getStatus, status)
                .orderByDesc(FulfillmentOrder::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    private FulfillmentOrder requireFulfillment(String fulfillmentNo) {
        FulfillmentOrder order = getFulfillment(fulfillmentNo);
        if (order == null) {
            throw new IllegalArgumentException("Fulfillment not found: " + fulfillmentNo);
        }
        return order;
    }

    private int requireStatus(FulfillmentOrder order) {
        if (order.getStatus() == null) {
            throw new IllegalStateException("Fulfillment has no status: " + order.getFulfillmentNo());
        }
        return order.getStatus();
    }

    private void requireStatus(FulfillmentOrder order, int expected, String action) {
        if (requireStatus(order) != expected) {
            throw new IllegalStateException("Fulfillment cannot be " + action
                    + ", status: " + order.getStatus());
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
