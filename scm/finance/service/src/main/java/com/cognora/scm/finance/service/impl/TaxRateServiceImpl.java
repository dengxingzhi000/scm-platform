package com.cognora.scm.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.util.UUIDv7Util;
import com.cognora.scm.finance.domain.dto.CreateTaxRateRequest;
import com.cognora.scm.finance.domain.dto.TaxRateResolution;
import com.cognora.scm.finance.domain.dto.UpdateTaxRateRequest;
import com.cognora.scm.finance.domain.entity.TaxRate;
import com.cognora.scm.finance.domain.support.FinanceAmount;
import com.cognora.scm.finance.mapper.TaxRateMapper;
import com.cognora.scm.finance.service.TaxRateService;
import com.cognora.scm.finance.service.support.FinanceTenantGuard;
import com.cognora.scm.finance.service.support.TaxRateResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxRateServiceImpl implements TaxRateService {

    private final TaxRateMapper taxRateMapper;

    private final TaxRateResolver taxRateResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaxRate create(CreateTaxRateRequest request) {
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        TaxRate rate = new TaxRate();
        rate.setId(UUIDv7Util.generateString());
        rate.setTenantId(tenantId);
        rate.setCategory(normalizeDimension(request.getCategory()));
        rate.setRegion(normalizeDimension(request.getRegion()));
        rate.setRate(FinanceAmount.rate(request.getRate(), "税率"));
        rate.setEffectiveFrom(requireDate(request.getEffectiveFrom(), "生效开始日期"));
        rate.setEffectiveTo(request.getEffectiveTo());
        assertEffectiveRange(rate.getEffectiveFrom(), rate.getEffectiveTo());
        rate.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        rate.setEnabled(true);
        rate.setRemark(request.getRemark());
        rate.setDeleted(false);
        rate.setCreateTime(LocalDateTime.now());
        rate.setUpdateTime(LocalDateTime.now());

        if (Boolean.TRUE.equals(rate.getIsDefault())) {
            clearOtherDefaults(tenantId, null);
        }
        taxRateMapper.insert(rate);
        log.info("税率规则已新增: id={}, category={}, region={}, rate={}",
                rate.getId(), rate.getCategory(), rate.getRegion(), rate.getRate());
        return rate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaxRate update(String id, UpdateTaxRateRequest request) {
        TaxRate rate = loadOwned(id);

        if (request.getRate() != null) {
            rate.setRate(FinanceAmount.rate(request.getRate(), "税率"));
        }
        if (request.getCategory() != null) {
            rate.setCategory(normalizeDimension(request.getCategory()));
        }
        if (request.getRegion() != null) {
            rate.setRegion(normalizeDimension(request.getRegion()));
        }
        if (request.getEffectiveFrom() != null) {
            rate.setEffectiveFrom(requireDate(request.getEffectiveFrom(), "生效开始日期"));
        }
        if (request.getEffectiveTo() != null) {
            rate.setEffectiveTo(request.getEffectiveTo());
        }
        assertEffectiveRange(rate.getEffectiveFrom(), rate.getEffectiveTo());
        if (request.getIsDefault() != null) {
            rate.setIsDefault(request.getIsDefault());
        }
        if (request.getEnabled() != null) {
            rate.setEnabled(request.getEnabled());
        }
        if (request.getRemark() != null) {
            rate.setRemark(request.getRemark());
        }

        if (Boolean.TRUE.equals(rate.getIsDefault())) {
            clearOtherDefaults(rate.getTenantId(), rate.getId());
        }
        rate.setUpdateTime(LocalDateTime.now());
        taxRateMapper.updateById(rate);
        log.info("税率规则已更新: id={}, enabled={}, rate={}", id, rate.getEnabled(), rate.getRate());
        return rate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaxRate disable(String id) {
        TaxRate rate = loadOwned(id);
        if (!Boolean.FALSE.equals(rate.getEnabled())) {
            rate.setEnabled(false);
            rate.setUpdateTime(LocalDateTime.now());
            taxRateMapper.updateById(rate);
            log.info("税率规则已停用: id={}", id);
        }
        return rate;
    }

    @Override
    public TaxRate getById(String id) {
        return loadOwned(id);
    }

    @Override
    public Page<TaxRate> pageQuery(int page, int size, String category, String region, Boolean enabled) {
        LambdaQueryWrapper<TaxRate> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TaxRate::getTenantId, TenantContextHolder.getRequiredTenantId().toString())
                .eq(TaxRate::getDeleted, false)
                .eq(StringUtils.hasText(category), TaxRate::getCategory, normalizeDimension(category))
                .eq(StringUtils.hasText(region), TaxRate::getRegion, normalizeDimension(region))
                .eq(enabled != null, TaxRate::getEnabled, enabled)
                .orderByDesc(TaxRate::getEffectiveFrom)
                .orderByDesc(TaxRate::getCreateTime);
        return taxRateMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public TaxRateResolution resolve(String category, String region, LocalDate businessDate) {
        return taxRateResolver.resolve(category, region, businessDate);
    }

    private TaxRate loadOwned(String id) {
        TaxRate rate = taxRateMapper.selectById(id);
        if (rate == null || Boolean.TRUE.equals(rate.getDeleted())) {
            throw new IllegalArgumentException("税率规则不存在: " + id);
        }
        FinanceTenantGuard.verifyOwnership(rate.getTenantId());
        return rate;
    }

    /**
     * 租户默认税率唯一：新增/改为默认前先把同租户其它默认规则降级。
     */
    private void clearOtherDefaults(String tenantId, String excludeId) {
        LambdaUpdateWrapper<TaxRate> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(TaxRate::getTenantId, tenantId)
                .eq(TaxRate::getDeleted, false)
                .eq(TaxRate::getIsDefault, true)
                .ne(StringUtils.hasText(excludeId), TaxRate::getId, excludeId)
                .set(TaxRate::getIsDefault, false)
                .set(TaxRate::getUpdateTime, LocalDateTime.now());
        taxRateMapper.update(null, wrapper);
    }

    private static LocalDate requireDate(LocalDate value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        return value;
    }

    private static void assertEffectiveRange(LocalDate from, LocalDate to) {
        if (to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("生效结束日期不能早于开始日期");
        }
    }

    private static String normalizeDimension(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
