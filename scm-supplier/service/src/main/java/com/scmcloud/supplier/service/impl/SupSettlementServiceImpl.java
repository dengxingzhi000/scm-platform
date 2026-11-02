package com.scmcloud.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.supplier.domain.entity.SupSettlement;
import com.scmcloud.supplier.mapper.SupSettlementMapper;
import com.scmcloud.supplier.service.ISupSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupSettlementServiceImpl extends ServiceImpl<SupSettlementMapper, SupSettlement>
        implements ISupSettlementService {

    private final StatusValidator statusValidator;

    @Override
    public Page<SupSettlement> pageList(int page, int size, String supplierId, Integer status,
                                        String settlementPeriod) {
        log.debug("分页查询对账单 page={}, size={}, supplierId={}, status={}", (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (supplierId + "").replace("\r", "_").replace("\n", "_"), (status + "").replace("\r", "_").replace("\n", "_"));

        LambdaQueryWrapper<SupSettlement> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(supplierId)) {
            wrapper.eq(SupSettlement::getSupplierId, supplierId);
        }
        if (status != null) {
            wrapper.eq(SupSettlement::getStatus, status);
        }
        if (StringUtils.hasText(settlementPeriod)) {
            wrapper.eq(SupSettlement::getSettlementPeriod, settlementPeriod);
        }

        wrapper.eq(SupSettlement::getDeleted, false);
        wrapper.orderByDesc(SupSettlement::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<SupSettlement> listBySupplierId(String supplierId) {
        if (!StringUtils.hasText(supplierId)) {
            return List.of();
        }
        log.debug("查询供应商的所有对账单: supplierId={}", (supplierId + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .eq(SupSettlement::getSupplierId, supplierId)
                .eq(SupSettlement::getDeleted, false)
                .orderByDesc(SupSettlement::getCreateTime)
                .list();
    }

    @Override
    public SupSettlement createSettlement(SupSettlement settlement) {
        log.info("[API] 创建对账单 supplierId={}", (settlement.getSupplierId() + "").replace("\r", "_").replace("\n", "_"));
        settlement.setId(UUID.randomUUID());
        settlement.setStatus(0);
        settlement.setDeleted(false);
        settlement.setCreateTime(LocalDateTime.now());
        settlement.setUpdateTime(LocalDateTime.now());
        save(settlement);
        log.info("[API] 对账单创建成功 id={}", settlement.getId());
        return settlement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupSettlement updateSettlement(String id, SupSettlement settlement) {
        log.info("[API] 更新对账单 id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        SupSettlement existing = getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return null;
        }
        if (settlement.getStatus() != null && !settlement.getStatus().equals(existing.getStatus())) {
            statusValidator.validateTransition("SUPPLIER_SETTLEMENT",
                    resolveStatusName(existing.getStatus()), resolveStatusName(settlement.getStatus()));
        }
        settlement.setId(UUID.fromString(id));
        settlement.setUpdateTime(LocalDateTime.now());
        updateById(settlement);
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSettlement(String id) {
        log.info("[API] 删除对账单 id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        SupSettlement existing = getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return false;
        }
        existing.setDeleted(true);
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
        return true;
    }

    private String resolveStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 0 -> "DRAFT";
            case 1 -> "CONFIRMED";
            case 2 -> "PARTIAL_PAID";
            case 3 -> "FULLY_PAID";
            case 4 -> "CLOSED";
            default -> throw new IllegalArgumentException("未知的对账单状态: " + status);
        };
    }

    private void validateTransitionTo(SupSettlement settlement, String targetStatus) {
        String currentStatus = resolveStatusName(settlement.getStatus());
        if (targetStatus.equals(currentStatus)) {
            return;
        }
        statusValidator.validateTransition("SUPPLIER_SETTLEMENT", currentStatus, targetStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirm(String id, String approverId, String approverName) {
        log.info("确认对账单 id={}, approverId={}", (id + "").replace("\r", "_").replace("\n", "_"), (approverId + "").replace("\r", "_").replace("\n", "_"));

        SupSettlement settlement = getById(id);
        if (settlement == null) {
            log.warn("对账单不存在: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
            return false;
        }
        validateTransitionTo(settlement, "CONFIRMED");

        settlement.setStatus(1);
        settlement.setApproverId(approverId);
        settlement.setApproverName(approverName);
        settlement.setApprovedAt(LocalDateTime.now());
        settlement.setUpdateTime(LocalDateTime.now());

        boolean success = updateById(settlement);
        if (success) {
            log.info("对账单确认成功 id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsPaid(String id, String updateBy) {
        log.info("标记对账单已付款: id={}", (id + "").replace("\r", "_").replace("\n", "_"));

        SupSettlement settlement = getById(id);
        if (settlement == null) {
            log.warn("对账单不存在: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
            return false;
        }
        validateTransitionTo(settlement, "FULLY_PAID");

        settlement.setStatus(3);
        settlement.setPaymentAmount(settlement.getActualAmount());
        settlement.setUpdateTime(LocalDateTime.now());
        settlement.setUpdateBy(updateBy);

        boolean success = updateById(settlement);
        if (success) {
            log.info("对账单标记付款成功 id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        }
        return success;
    }
}
