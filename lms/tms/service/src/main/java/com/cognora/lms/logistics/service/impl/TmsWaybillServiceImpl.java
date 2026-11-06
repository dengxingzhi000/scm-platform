package com.cognora.lms.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.cognora.lms.logistics.domain.IllegalStatusTransitionException;
import com.cognora.lms.logistics.domain.WaybillStatus;
import com.cognora.lms.logistics.domain.entity.TmsWaybill;
import com.cognora.lms.logistics.mapper.TmsWaybillMapper;
import com.cognora.lms.logistics.service.ITmsWaybillService;
import com.cognora.lms.logistics.support.TenantGuard;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsWaybillServiceImpl extends ServiceImpl<TmsWaybillMapper, TmsWaybill> implements ITmsWaybillService {

    @Override
    public Page<TmsWaybill> pageList(int page, int size, String waybillNo, Integer status, String carrierId) {
        log.debug("鏌ヨ杩愬崟鍒楄〃: page={}, size={}, waybillNo={}, status={}, carrierId={}", (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (waybillNo + "").replace("\r", "_").replace("\n", "_"), (status + "").replace("\r", "_").replace("\n", "_"), (carrierId + "").replace("\r", "_").replace("\n", "_"));

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
        log.debug("Query by waybill number: waybillNo={}", (waybillNo + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .eq(TmsWaybill::getWaybillNo, waybillNo)
                .eq(TmsWaybill::getDeleted, false)
                .one();
    }

    @Override
    public List<TmsWaybill> listByOrderId(String orderId) {
        log.debug("Query waybills by order ID: orderId={}", (orderId + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .eq(TmsWaybill::getOrderId, orderId)
                .eq(TmsWaybill::getDeleted, false)
                .orderByDesc(TmsWaybill::getCreateTime)
                .list();
    }

    @Override
    public List<TmsWaybill> listByOrderNo(String orderNo) {
        log.debug("Query waybills by order number: orderNo={}", (orderNo + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .eq(TmsWaybill::getOrderNo, orderNo)
                .eq(TmsWaybill::getDeleted, false)
                .orderByDesc(TmsWaybill::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TmsWaybill createWaybill(TmsWaybill waybill) {
        log.info("鍒涘缓杩愬崟: orderNo={}, carrierId={}", (waybill.getOrderNo() + "").replace("\r", "_").replace("\n", "_"), (waybill.getCarrierId() + "").replace("\r", "_").replace("\n", "_"));
        TenantGuard.requireTenant();

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

        log.info("杩愬崟鍒涘缓鎴愬姛: id={}, waybillNo={}", (waybill.getId() + "").replace("\r", "_").replace("\n", "_"), (waybill.getWaybillNo() + "").replace("\r", "_").replace("\n", "_"));
        return waybill;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(String waybillId, Integer status, String operator) {
        log.info("鏇存柊杩愬崟鐘讹拷 waybillId={}, status={}, operator={}", (waybillId + "").replace("\r", "_").replace("\n", "_"), (status + "").replace("\r", "_").replace("\n", "_"), (operator + "").replace("\r", "_").replace("\n", "_"));

        TenantGuard.requireTenant();
        TmsWaybill waybill = getById(waybillId);
        if (waybill == null) {
            log.warn("Waybill not found or not visible in current tenant: waybillId={}", (waybillId + "").replace("\r", "_").replace("\n", "_"));
            throw new AccessDeniedException("Waybill is not accessible in the current tenant");
        }

        int current = waybill.getStatus() == null ? WaybillStatus.CREATED : waybill.getStatus();
        WaybillStatus.assertTransitionAllowed(current, status);

        if (current == status) {
            // 幂等重放：同状态重复提交不改写数据，也不刷新签收时间。
            log.info("Waybill status replay ignored: waybillNo={}, status={}", waybill.getWaybillNo(), status);
            return true;
        }

        waybill.setStatus(status);
        waybill.setUpdateBy(operator);
        waybill.setUpdateTime(LocalDateTime.now());

        if (status == WaybillStatus.DELIVERED) {
            // 签收时间只写一次，保证重复回调不会覆盖真实签收时刻。
            LocalDateTime now = LocalDateTime.now();
            if (waybill.getActualDelivery() == null) {
                waybill.setActualDelivery(now);
            }
            if (waybill.getSignTime() == null) {
                waybill.setSignTime(now);
            }
        }

        boolean success = updateById(waybill);
        if (success) {
            log.info("Waybill status updated: waybillNo={}, from={}, to={}",
                    waybill.getWaybillNo(), current, status);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelWaybill(String waybillId, String reason, String operator) {
        log.info("鍙栨秷杩愬崟: waybillId={}, reason={}, operator={}", (waybillId + "").replace("\r", "_").replace("\n", "_"), (reason + "").replace("\r", "_").replace("\n", "_"), (operator + "").replace("\r", "_").replace("\n", "_"));

        TenantGuard.requireTenant();
        TmsWaybill waybill = getById(waybillId);
        if (waybill == null) {
            log.warn("杩愬崟涓嶅瓨\u200b waybillId={}", (waybillId + "").replace("\r", "_").replace("\n", "_"));
            throw new AccessDeniedException("Waybill is not accessible in the current tenant");
        }

        int current = waybill.getStatus() == null ? WaybillStatus.CREATED : waybill.getStatus();
        if (!WaybillStatus.isCancellable(current)) {
            // 已签收/已退回不可再取消；派送中需走退回流程，避免运单凭空消失。
            throw new IllegalStatusTransitionException(
                    "运单当前状态「" + WaybillStatus.label(current) + "」不允许取消，请走退回流程");
        }

        waybill.setStatus(WaybillStatus.RETURNED);
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

    @Override
    public void deleteWaybill(String id) {
        log.info("Delete waybill: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        TenantGuard.requireTenant();
        TmsWaybill existing = getById(id);
        if (existing == null) {
            throw new AccessDeniedException("Waybill is not accessible in the current tenant");
        }
        if (WaybillStatus.isTerminal(existing.getStatus())) {
            // 已签收/已退回运单承载签收凭证与对账依据，禁止逻辑删除。
            throw new IllegalStatusTransitionException(
                    "运单已处于终态「" + WaybillStatus.label(existing.getStatus()) + "」，不可删除");
        }
        TmsWaybill waybill = new TmsWaybill();
        waybill.setId(id);
        waybill.setDeleted(true);
        updateById(waybill);
    }

    private String generateWaybillNo() {
        return "WB" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
    }
}
