package com.scmcloud.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.common.status.StatusValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.scmcloud.logistics.domain.entity.TmsWaybill;
import com.scmcloud.logistics.mapper.TmsWaybillMapper;
import com.scmcloud.logistics.service.ITmsWaybillService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsWaybillServiceImpl extends ServiceImpl<TmsWaybillMapper, TmsWaybill> implements ITmsWaybillService {

    private final StatusValidator statusValidator;

    @Override
    public Page<TmsWaybill> pageList(int page, int size, String waybillNo, Integer status, String carrierId) {
        log.debug("鏌ヨ杩愬崟鍒楄〃: page={}, size={}, waybillNo={}, status={}, carrierId={}", page, size, waybillNo, status, carrierId);

        LambdaQueryWrapper<TmsWaybill> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(waybillNo)) {
            wrapper.like(TmsWaybill::getWaybillNo, waybillNo);
        }
        if (status != null) {
            wrapper.eq(TmsWaybill::getStatus, status);
        }
        if (StringUtils.hasText(carrierId)) {
            wrapper.eq(TmsWaybill::getCarrierId, carrierId);
        }
        wrapper.eq(TmsWaybill::getDeleted, false);
        wrapper.orderByDesc(TmsWaybill::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public TmsWaybill getByWaybillNo(String waybillNo) {
        log.debug("Query by waybill number: waybillNo={}", waybillNo);
        return lambdaQuery()
                .eq(TmsWaybill::getWaybillNo, waybillNo)
                .eq(TmsWaybill::getDeleted, false)
                .one();
    }

    @Override
    public List<TmsWaybill> listByOrderId(String orderId) {
        log.debug("Query waybills by order ID: orderId={}", orderId);
        return lambdaQuery()
                .eq(TmsWaybill::getOrderId, orderId)
                .eq(TmsWaybill::getDeleted, false)
                .orderByDesc(TmsWaybill::getCreateTime)
                .list();
    }

    @Override
    public List<TmsWaybill> listByOrderNo(String orderNo) {
        log.debug("Query waybills by order number: orderNo={}", orderNo);
        return lambdaQuery()
                .eq(TmsWaybill::getOrderNo, orderNo)
                .eq(TmsWaybill::getDeleted, false)
                .orderByDesc(TmsWaybill::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TmsWaybill createWaybill(TmsWaybill waybill) {
        log.info("鍒涘缓杩愬崟: orderNo={}, carrierId={}", waybill.getOrderNo(), waybill.getCarrierId());

        if (waybill.getId() == null) {
            waybill.setId(UUID.randomUUID().toString());
        }
        if (waybill.getWaybillNo() == null) {
            waybill.setWaybillNo(generateWaybillNo());
        }
        waybill.setStatus(0);
        waybill.setDeleted(false);
        waybill.setCreateTime(LocalDateTime.now());
        waybill.setUpdateTime(LocalDateTime.now());

        boolean success = save(waybill);
        if (!success) {
            throw new RuntimeException("鍒涘缓杩愬崟澶辫触");
        }

        log.info("杩愬崟鍒涘缓鎴愬姛: id={}, waybillNo={}", waybill.getId(), waybill.getWaybillNo());
        return waybill;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(String waybillId, Integer status, String operator) {
        log.info("鏇存柊杩愬崟鐘讹拷 waybillId={}, status={}, operator={}", waybillId, status, operator);

        TmsWaybill waybill = getById(waybillId);
        if (waybill == null) {
            log.warn("Waybill not found: waybillId={}", waybillId);
            return false;
        }

        waybill.setStatus(status);
        waybill.setUpdateBy(operator);
        waybill.setUpdateTime(LocalDateTime.now());

        if (status == 4) {
            waybill.setActualDelivery(LocalDateTime.now());
        }

        boolean success = updateById(waybill);
        if (success) {
            log.info("Waybill status updated successfully: waybillNo={}, status={}", waybill.getWaybillNo(), status);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelWaybill(String waybillId, String reason, String operator) {
        log.info("鍙栨秷杩愬崟: waybillId={}, reason={}, operator={}", waybillId, reason, operator);

        TmsWaybill waybill = getById(waybillId);
        if (waybill == null) {
            log.warn("杩愬崟涓嶅瓨\u200b waybillId={}", waybillId);
            return false;
        }

        String implCancelFromStatus;
        if (waybill.getStatus() == 0) {
            implCancelFromStatus = "CREATED";
        } else if (waybill.getStatus() == 1) {
            implCancelFromStatus = "PENDING";
        } else if (waybill.getStatus() == 2) {
            implCancelFromStatus = "IN_TRANSIT";
        } else {
            implCancelFromStatus = "DELIVERED";
        }
        statusValidator.validateTransition("LOGISTICS", implCancelFromStatus, "CANCELLED");

        waybill.setStatus(6);
        waybill.setExceptionType("CANCEL");
        waybill.setExceptionReason(reason);
        waybill.setUpdateBy(operator);
        waybill.setUpdateTime(LocalDateTime.now());

        boolean success = updateById(waybill);
        if (success) {
            log.info("杩愬崟鍙栨秷鎴愬姛: waybillNo={}", waybill.getWaybillNo());
        }
        return success;
    }

    private String generateWaybillNo() {
        return "WB" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
    }
}
