package com.cognora.lms.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.lms.logistics.domain.WaybillStatus;
import com.cognora.lms.logistics.domain.entity.TmsSlaAlert;
import com.cognora.lms.logistics.domain.entity.TmsWaybill;
import com.cognora.lms.logistics.domain.sla.SlaCalculator;
import com.cognora.lms.logistics.mapper.TmsSlaAlertMapper;
import com.cognora.lms.logistics.service.ITmsSlaAlertService;
import com.cognora.lms.logistics.service.ITmsWaybillService;
import com.cognora.lms.logistics.support.TenantGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsSlaAlertServiceImpl extends ServiceImpl<TmsSlaAlertMapper, TmsSlaAlert>
        implements ITmsSlaAlertService {

    private final ITmsWaybillService waybillService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanOverdue(LocalDateTime now) {
        LocalDateTime scanAt = now == null ? LocalDateTime.now() : now;
        LambdaQueryWrapper<TmsWaybill> wrapper = Wrappers.lambdaQuery();
        wrapper.isNotNull(TmsWaybill::getEstimatedDelivery);
        wrapper.lt(TmsWaybill::getEstimatedDelivery, scanAt);
        wrapper.notIn(TmsWaybill::getStatus, WaybillStatus.DELIVERED, WaybillStatus.RETURNED);
        wrapper.eq(TmsWaybill::getDeleted, false);
        List<TmsWaybill> waybills = waybillService.list(wrapper);

        int created = 0;
        for (TmsWaybill waybill : waybills) {
            if (exists(waybill.getWaybillNo(), waybill.getEstimatedDelivery())) {
                continue;
            }
            SlaCalculator.SlaEvaluation evaluation = SlaCalculator.evaluate(
                    waybill.getEstimatedDelivery(), null, scanAt);
            if (!SlaCalculator.OVERDUE.equals(evaluation.status())) {
                continue;
            }
            TmsSlaAlert alert = new TmsSlaAlert();
            alert.setId(UUID.randomUUID().toString());
            alert.setTenantId(waybill.getTenantId());
            alert.setWaybillId(waybill.getId());
            alert.setWaybillNo(waybill.getWaybillNo());
            alert.setCarrierId(waybill.getCarrierId());
            alert.setPromisedTime(waybill.getEstimatedDelivery());
            alert.setOverdueHours(evaluation.overdueHours());
            alert.setLevel(evaluation.level());
            alert.setStatus(TmsSlaAlert.STATUS_OPEN);
            alert.setCreateTime(scanAt);
            alert.setUpdateTime(scanAt);
            try {
                if (save(alert)) {
                    created++;
                }
            } catch (DuplicateKeyException duplicate) {
                // 并发扫描由唯一索引兜底，跳过即可。
                log.debug("SLA alert already exists: waybillNo={}", waybill.getWaybillNo());
            }
        }
        log.info("SLA 超时扫描完成: scanned={}, created={}, scanAt={}", waybills.size(), created, scanAt);
        return created;
    }

    @Override
    public Page<TmsSlaAlert> pageList(int page, int size, String waybillNo, String status, String level) {
        LambdaQueryWrapper<TmsSlaAlert> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(waybillNo)) {
            wrapper.eq(TmsSlaAlert::getWaybillNo, waybillNo);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(TmsSlaAlert::getStatus, status.trim().toUpperCase());
        }
        if (StringUtils.hasText(level)) {
            wrapper.eq(TmsSlaAlert::getLevel, level.trim().toUpperCase());
        }
        wrapper.orderByDesc(TmsSlaAlert::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public void acknowledge(String id, String operator) {
        TenantGuard.requireTenant();
        TmsSlaAlert alert = getById(id);
        if (alert == null) {
            throw new AccessDeniedException("SLA alert is not accessible in the current tenant");
        }
        if (TmsSlaAlert.STATUS_ACK.equals(alert.getStatus())) {
            return;
        }
        if (!TmsSlaAlert.STATUS_OPEN.equals(alert.getStatus())) {
            throw new IllegalArgumentException("仅 OPEN 状态的预警可确认");
        }
        TmsSlaAlert patch = new TmsSlaAlert();
        patch.setId(id);
        patch.setStatus(TmsSlaAlert.STATUS_ACK);
        patch.setAckBy(operator);
        patch.setAckTime(LocalDateTime.now());
        patch.setUpdateTime(LocalDateTime.now());
        updateById(patch);
    }

    private boolean exists(String waybillNo, LocalDateTime promisedTime) {
        LambdaQueryWrapper<TmsSlaAlert> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmsSlaAlert::getWaybillNo, waybillNo);
        wrapper.eq(TmsSlaAlert::getPromisedTime, promisedTime);
        List<TmsSlaAlert> list = list(wrapper);
        return !list.isEmpty();
    }
}
