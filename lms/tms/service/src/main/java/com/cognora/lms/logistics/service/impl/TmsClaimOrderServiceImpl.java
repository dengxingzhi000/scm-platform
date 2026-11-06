package com.cognora.lms.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.lms.logistics.domain.entity.TmsClaimOrder;
import com.cognora.lms.logistics.domain.entity.TmsExceptionCase;
import com.cognora.lms.logistics.domain.exceptioncase.ClaimOrderStatus;
import com.cognora.lms.logistics.domain.exceptioncase.ExceptionCaseStatus;
import com.cognora.lms.logistics.mapper.TmsClaimOrderMapper;
import com.cognora.lms.logistics.service.ITmsClaimOrderService;
import com.cognora.lms.logistics.service.ITmsExceptionCaseService;
import com.cognora.lms.logistics.support.TenantGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsClaimOrderServiceImpl extends ServiceImpl<TmsClaimOrderMapper, TmsClaimOrder>
        implements ITmsClaimOrderService {

    private final ITmsExceptionCaseService exceptionCaseService;

    @Override
    public Page<TmsClaimOrder> pageList(int page, int size, String waybillNo, String status) {
        LambdaQueryWrapper<TmsClaimOrder> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(waybillNo)) {
            wrapper.eq(TmsClaimOrder::getWaybillNo, waybillNo);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(TmsClaimOrder::getStatus, status.trim().toUpperCase());
        }
        wrapper.orderByDesc(TmsClaimOrder::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TmsClaimOrder createClaim(String caseId, BigDecimal amount, String reason,
                                     String evidenceUrl, String operator) {
        TenantGuard.requireTenant();
        if (!StringUtils.hasText(caseId)) {
            throw new IllegalArgumentException("异常件 ID 不能为空");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("理赔金额必须大于 0");
        }
        TmsExceptionCase exceptionCase = exceptionCaseService.getById(caseId);
        if (exceptionCase == null) {
            throw new AccessDeniedException("Exception case is not accessible in the current tenant");
        }
        if (!ExceptionCaseStatus.PROCESSING.equals(exceptionCase.getStatus())
                && !ExceptionCaseStatus.RESOLVED.equals(exceptionCase.getStatus())) {
            throw new IllegalStateException("异常件需处于处理中或已解决状态才可发起理赔");
        }

        TmsClaimOrder claim = new TmsClaimOrder();
        claim.setId(UUID.randomUUID().toString());
        claim.setCaseId(exceptionCase.getId());
        claim.setWaybillNo(exceptionCase.getWaybillNo());
        claim.setClaimAmount(amount.setScale(2, java.math.RoundingMode.HALF_UP));
        claim.setCurrency("CNY");
        claim.setStatus(ClaimOrderStatus.DRAFT);
        claim.setReason(reason);
        claim.setEvidenceUrl(evidenceUrl);
        claim.setCreateBy(operator);
        claim.setCreateTime(LocalDateTime.now());
        claim.setUpdateTime(LocalDateTime.now());
        save(claim);
        log.info("理赔单已创建: id={}, caseId={}, amount={}", claim.getId(),
                (caseId + "").replace("\r", "_").replace("\n", "_"), claim.getClaimAmount());
        return claim;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TmsClaimOrder transition(String id, String targetStatus, String operator, String remark) {
        TenantGuard.requireTenant();
        TmsClaimOrder existing = getById(id);
        if (existing == null) {
            throw new AccessDeniedException("Claim order is not accessible in the current tenant");
        }
        String from = existing.getStatus();
        String target = targetStatus == null ? null : targetStatus.trim().toUpperCase();
        if (!ClaimOrderStatus.isKnown(target)) {
            throw new IllegalArgumentException("未知的理赔单目标状态: " + targetStatus);
        }
        ClaimOrderStatus.assertAllowed(from, target);

        LocalDateTime now = LocalDateTime.now();
        TmsClaimOrder patch = new TmsClaimOrder();
        patch.setId(id);
        patch.setStatus(target);
        patch.setUpdateTime(now);
        patch.setRemark(remark);
        if (ClaimOrderStatus.SUBMITTED.equals(target)) {
            patch.setSubmittedBy(operator);
            patch.setSubmittedTime(now);
        } else if (ClaimOrderStatus.APPROVED.equals(target) || ClaimOrderStatus.REJECTED.equals(target)) {
            patch.setReviewedBy(operator);
            patch.setReviewedTime(now);
        } else if (ClaimOrderStatus.PAID.equals(target)) {
            patch.setPaidTime(now);
        }
        updateById(patch);
        existing.setStatus(target);
        log.info("理赔单状态流转: id={}, {} -> {}", (id + "").replace("\r", "_").replace("\n", "_"), from, target);
        return existing;
    }
}
