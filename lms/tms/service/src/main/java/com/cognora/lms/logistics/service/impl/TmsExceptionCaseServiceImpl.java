package com.cognora.lms.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.lms.logistics.domain.WaybillStatus;
import com.cognora.lms.logistics.domain.entity.TmsExceptionCase;
import com.cognora.lms.logistics.domain.entity.TmsWaybill;
import com.cognora.lms.logistics.domain.exceptioncase.ExceptionCaseStatus;
import com.cognora.lms.logistics.mapper.TmsExceptionCaseMapper;
import com.cognora.lms.logistics.service.ITmsExceptionCaseService;
import com.cognora.lms.logistics.service.ITmsWaybillService;
import com.cognora.lms.logistics.support.TenantGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsExceptionCaseServiceImpl extends ServiceImpl<TmsExceptionCaseMapper, TmsExceptionCase>
        implements ITmsExceptionCaseService {

    private final ITmsWaybillService waybillService;

    @Override
    public Page<TmsExceptionCase> pageList(int page, int size, String waybillNo, String status) {
        LambdaQueryWrapper<TmsExceptionCase> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(waybillNo)) {
            wrapper.eq(TmsExceptionCase::getWaybillNo, waybillNo);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(TmsExceptionCase::getStatus, status.trim().toUpperCase());
        }
        wrapper.orderByDesc(TmsExceptionCase::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TmsExceptionCase createCase(String waybillId, String exceptionType, String description, String operator) {
        TenantGuard.requireTenant();
        if (!StringUtils.hasText(exceptionType)) {
            throw new IllegalArgumentException("异常类型不能为空");
        }
        TmsWaybill waybill = waybillService.getById(waybillId);
        if (waybill == null) {
            throw new AccessDeniedException("Waybill is not accessible in the current tenant");
        }
        if (WaybillStatus.isTerminal(waybill.getStatus())) {
            throw new IllegalStateException("已签收/已退回运单不能登记异常件");
        }

        TmsExceptionCase exceptionCase = new TmsExceptionCase();
        exceptionCase.setId(UUID.randomUUID().toString());
        exceptionCase.setWaybillId(waybill.getId());
        exceptionCase.setWaybillNo(waybill.getWaybillNo());
        exceptionCase.setExceptionType(exceptionType.trim().toUpperCase());
        exceptionCase.setDescription(description);
        exceptionCase.setStatus(ExceptionCaseStatus.NEW);
        exceptionCase.setCreateBy(operator);
        exceptionCase.setCreateTime(LocalDateTime.now());
        exceptionCase.setUpdateTime(LocalDateTime.now());
        save(exceptionCase);
        log.info("异常件已登记: id={}, waybillNo={}, type={}",
                exceptionCase.getId(), waybill.getWaybillNo(), exceptionCase.getExceptionType());
        return exceptionCase;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TmsExceptionCase claim(String id, String responsibleParty, String operator) {
        TenantGuard.requireTenant();
        if (!StringUtils.hasText(responsibleParty)) {
            throw new IllegalArgumentException("责任方不能为空");
        }
        TmsExceptionCase existing = getById(id);
        if (existing == null) {
            // 跨租户不可见统一按 403，避免泄露其他租户单据是否存在。
            throw new AccessDeniedException("Exception case is not accessible in the current tenant");
        }
        ExceptionCaseStatus.assertAllowed(existing.getStatus(), ExceptionCaseStatus.CLAIMED);

        TmsExceptionCase patch = new TmsExceptionCase();
        patch.setId(id);
        patch.setStatus(ExceptionCaseStatus.CLAIMED);
        patch.setResponsibleParty(responsibleParty.trim().toUpperCase());
        patch.setClaimedBy(operator);
        patch.setClaimedTime(LocalDateTime.now());
        patch.setUpdateTime(LocalDateTime.now());
        updateById(patch);
        existing.setStatus(patch.getStatus());
        existing.setResponsibleParty(patch.getResponsibleParty());
        existing.setClaimedBy(patch.getClaimedBy());
        existing.setClaimedTime(patch.getClaimedTime());
        log.info("异常件已认领: id={}, responsibleParty={}, operator={}", id, patch.getResponsibleParty(), operator);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TmsExceptionCase transition(String id, String targetStatus, String resolution, String operator) {
        TenantGuard.requireTenant();
        if (!StringUtils.hasText(targetStatus)) {
            throw new IllegalArgumentException("目标状态不能为空");
        }
        TmsExceptionCase existing = getById(id);
        if (existing == null) {
            throw new AccessDeniedException("Exception case is not accessible in the current tenant");
        }
        String from = existing.getStatus();
        String target = targetStatus.trim().toUpperCase();
        ExceptionCaseStatus.assertAllowed(from, target);

        TmsExceptionCase patch = new TmsExceptionCase();
        patch.setId(id);
        patch.setStatus(target);
        patch.setUpdateTime(LocalDateTime.now());
        if (ExceptionCaseStatus.RESOLVED.equals(target)) {
            patch.setResolution(resolution);
            patch.setResolvedTime(LocalDateTime.now());
        }
        updateById(patch);
        existing.setStatus(target);
        if (patch.getResolution() != null) {
            existing.setResolution(patch.getResolution());
            existing.setResolvedTime(patch.getResolvedTime());
        }
        log.info("异常件状态流转: id={}, {} -> {}", id, from, target);
        return existing;
    }
}
