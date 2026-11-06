package com.cognora.lms.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.lms.logistics.domain.entity.TmsFreightReconciliation;
import com.cognora.lms.logistics.domain.entity.TmsWaybill;
import com.cognora.lms.logistics.domain.freight.FreightDiffClassifier;
import com.cognora.lms.logistics.domain.freight.FreightQuote;
import com.cognora.lms.logistics.mapper.TmsFreightReconciliationMapper;
import com.cognora.lms.logistics.service.ITmsFreightReconciliationService;
import com.cognora.lms.logistics.service.ITmsFreightTemplateService;
import com.cognora.lms.logistics.service.ITmsWaybillService;
import com.cognora.lms.logistics.support.TenantGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsFreightReconciliationServiceImpl
        extends ServiceImpl<TmsFreightReconciliationMapper, TmsFreightReconciliation>
        implements ITmsFreightReconciliationService {

    /** 未指定批次时的默认批次号。 */
    public static final String DEFAULT_BATCH = "DEFAULT";

    private final ITmsWaybillService waybillService;
    private final ITmsFreightTemplateService freightTemplateService;

    @Override
    public Page<TmsFreightReconciliation> pageList(int page, int size, String batchNo, String diffType, String status) {
        LambdaQueryWrapper<TmsFreightReconciliation> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(batchNo)) {
            wrapper.eq(TmsFreightReconciliation::getBatchNo, batchNo);
        }
        if (StringUtils.hasText(diffType)) {
            wrapper.eq(TmsFreightReconciliation::getDiffType, diffType.trim().toUpperCase());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(TmsFreightReconciliation::getStatus, status.trim().toUpperCase());
        }
        wrapper.orderByDesc(TmsFreightReconciliation::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TmsFreightReconciliation reconcile(String waybillId, String regionCode, BigDecimal billedAmount,
                                              String batchNo, String operator) {
        TenantGuard.requireTenant();
        if (!StringUtils.hasText(waybillId)) {
            throw new IllegalArgumentException("waybillId 不能为空");
        }
        if (billedAmount == null || billedAmount.signum() < 0) {
            throw new IllegalArgumentException("承运商账单金额不能为空且不能为负数");
        }
        String batch = StringUtils.hasText(batchNo) ? batchNo.trim() : DEFAULT_BATCH;

        TmsWaybill waybill = waybillService.getById(waybillId);
        if (waybill == null) {
            throw new AccessDeniedException("Waybill is not accessible in the current tenant");
        }

        TmsFreightReconciliation existing = findByBatchAndWaybill(batch, waybill.getWaybillNo());
        if (existing != null) {
            log.info("运费对账已存在，幂等返回: batchNo={}, waybillNo={}", batch, waybill.getWaybillNo());
            return existing;
        }

        FreightQuote quote = freightTemplateService.quote(
                waybill.getCarrierId(), regionCode, waybill.getGoodsWeight(), waybill.getGoodsVolume());
        BigDecimal expected = quote.totalAmount();
        BigDecimal diff = FreightDiffClassifier.diff(billedAmount, expected);

        TmsFreightReconciliation record = new TmsFreightReconciliation();
        record.setId(UUID.randomUUID().toString());
        record.setBatchNo(batch);
        record.setWaybillId(waybill.getId());
        record.setWaybillNo(waybill.getWaybillNo());
        record.setCarrierId(waybill.getCarrierId());
        record.setRegionCode(StringUtils.hasText(regionCode) ? regionCode.trim().toUpperCase() : null);
        record.setActualWeight(quote.actualWeight());
        record.setVolume(waybill.getGoodsVolume());
        record.setChargeableWeight(quote.chargeableWeight());
        record.setExpectedAmount(expected);
        record.setBilledAmount(billedAmount.setScale(2, java.math.RoundingMode.HALF_UP));
        record.setDiffAmount(diff);
        record.setDiffType(FreightDiffClassifier.classify(diff));
        record.setStatus(TmsFreightReconciliation.STATUS_OPEN);
        record.setCreateBy(operator);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        try {
            if (!save(record)) {
                throw new IllegalStateException("运费对账落库失败");
            }
        } catch (DuplicateKeyException duplicate) {
            // 并发重复对账由唯一索引兜底，回读既有记录保持幂等。
            TmsFreightReconciliation concurrent = findByBatchAndWaybill(batch, waybill.getWaybillNo());
            if (concurrent != null) {
                return concurrent;
            }
            throw duplicate;
        }

        log.info("运费对账完成: batchNo={}, waybillNo={}, expected={}, billed={}, diffType={}",
                batch, waybill.getWaybillNo(), expected, record.getBilledAmount(), record.getDiffType());
        return record;
    }

    private TmsFreightReconciliation findByBatchAndWaybill(String batchNo, String waybillNo) {
        LambdaQueryWrapper<TmsFreightReconciliation> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmsFreightReconciliation::getBatchNo, batchNo);
        wrapper.eq(TmsFreightReconciliation::getWaybillNo, waybillNo);
        List<TmsFreightReconciliation> list = list(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TmsFreightReconciliation confirm(String id, String operator) {
        TenantGuard.requireTenant();
        TmsFreightReconciliation existing = getById(id);
        if (existing == null) {
            throw new AccessDeniedException("Freight reconciliation is not accessible in the current tenant");
        }
        if (TmsFreightReconciliation.STATUS_CONFIRMED.equals(existing.getStatus())) {
            // 重复确认幂等：不重复写库、不覆盖首次确认人。
            return existing;
        }
        if (!TmsFreightReconciliation.STATUS_OPEN.equals(existing.getStatus())) {
            throw new IllegalStateException("仅 OPEN 状态的差异单可确认: " + existing.getStatus());
        }
        TmsFreightReconciliation patch = new TmsFreightReconciliation();
        patch.setId(id);
        patch.setStatus(TmsFreightReconciliation.STATUS_CONFIRMED);
        patch.setRemark(operator);
        patch.setUpdateTime(LocalDateTime.now());
        updateById(patch);
        existing.setStatus(TmsFreightReconciliation.STATUS_CONFIRMED);
        log.info("运费差异单已确认: id={}, waybillNo={}, operator={}", id, existing.getWaybillNo(), operator);
        return existing;
    }
}
