package com.scmcloud.fulfillment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.fulfillment.domain.entity.FulfillmentOrder;
import com.scmcloud.fulfillment.mapper.FulfillmentOrderMapper;
import com.scmcloud.fulfillment.service.IFulfillmentOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Slf4j
@Service
public class FulfillmentOrderServiceImpl extends ServiceImpl<FulfillmentOrderMapper, FulfillmentOrder> implements IFulfillmentOrderService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FulfillmentOrder createFulfillment(String orderNo, String userId, String fulfillmentType) {
        log.info("Creating fulfillment: orderNo={}, userId={}, type={}", orderNo, userId, fulfillmentType);

        FulfillmentOrder order = new FulfillmentOrder();
        order.setFulfillmentNo("FUL" + System.currentTimeMillis());
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setFulfillmentType(fulfillmentType);
        order.setStatus(1);
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
        FulfillmentOrder order = getFulfillment(fulfillmentNo);
        if (order == null) {
            throw new IllegalArgumentException("Fulfillment not found: " + fulfillmentNo);
        }

        order.setStatus(6);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
        log.info("Fulfillment cancelled: fulfillmentNo={}, reason={}", fulfillmentNo, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickItems(String fulfillmentNo) {
        FulfillmentOrder order = getFulfillment(fulfillmentNo);
        if (order == null) {
            throw new IllegalArgumentException("Fulfillment not found: " + fulfillmentNo);
        }

        order.setStatus(2);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
        log.info("Fulfillment picking: fulfillmentNo={}", fulfillmentNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void packItems(String fulfillmentNo) {
        FulfillmentOrder order = getFulfillment(fulfillmentNo);
        if (order == null) {
            throw new IllegalArgumentException("Fulfillment not found: " + fulfillmentNo);
        }

        order.setStatus(3);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
        log.info("Fulfillment packing: fulfillmentNo={}", fulfillmentNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipItems(String fulfillmentNo, String trackingNo, String carrier) {
        FulfillmentOrder order = getFulfillment(fulfillmentNo);
        if (order == null) {
            throw new IllegalArgumentException("Fulfillment not found: " + fulfillmentNo);
        }

        order.setStatus(4);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
        log.info("Fulfillment shipped: fulfillmentNo={}, trackingNo={}, carrier={}", fulfillmentNo, trackingNo, carrier);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDelivery(String fulfillmentNo) {
        FulfillmentOrder order = getFulfillment(fulfillmentNo);
        if (order == null) {
            throw new IllegalArgumentException("Fulfillment not found: " + fulfillmentNo);
        }

        order.setStatus(5);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
        log.info("Fulfillment delivered: fulfillmentNo={}", fulfillmentNo);
    }
}
