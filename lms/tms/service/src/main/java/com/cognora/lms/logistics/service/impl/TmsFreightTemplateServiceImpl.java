package com.cognora.lms.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.lms.logistics.domain.entity.TmsFreightTemplate;
import com.cognora.lms.logistics.domain.freight.FreightCalculator;
import com.cognora.lms.logistics.domain.freight.FreightQuote;
import com.cognora.lms.logistics.domain.freight.FreightRule;
import com.cognora.lms.logistics.mapper.TmsFreightTemplateMapper;
import com.cognora.lms.logistics.service.ITmsFreightTemplateService;
import com.cognora.lms.logistics.support.TenantGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TmsFreightTemplateServiceImpl extends ServiceImpl<TmsFreightTemplateMapper, TmsFreightTemplate>
        implements ITmsFreightTemplateService {

    /** 兜底区域编码：该省份无独立模板时使用。 */
    public static final String DEFAULT_REGION = "DEFAULT";

    @Override
    public Page<TmsFreightTemplate> pageList(int page, int size, String carrierId, String regionCode) {
        LambdaQueryWrapper<TmsFreightTemplate> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(carrierId)) {
            wrapper.eq(TmsFreightTemplate::getCarrierId, carrierId);
        }
        if (StringUtils.hasText(regionCode)) {
            wrapper.eq(TmsFreightTemplate::getRegionCode, regionCode.trim().toUpperCase());
        }
        wrapper.eq(TmsFreightTemplate::getDeleted, false);
        wrapper.orderByDesc(TmsFreightTemplate::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<TmsFreightTemplate> listByCarrier(String carrierId) {
        return lambdaQuery()
                .eq(TmsFreightTemplate::getCarrierId, carrierId)
                .eq(TmsFreightTemplate::getDeleted, false)
                .orderByAsc(TmsFreightTemplate::getRegionCode)
                .list();
    }

    @Override
    public TmsFreightTemplate createTemplate(TmsFreightTemplate template) {
        TenantGuard.requireTenant();
        if (template == null) {
            throw new IllegalArgumentException("运费模板不能为空");
        }
        template.setId(UUID.randomUUID().toString());
        template.setRegionCode(normalizeRegion(template.getRegionCode()));
        if (template.getEnabled() == null) {
            template.setEnabled(true);
        }
        template.setDeleted(false);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        save(template);
        log.info("运费模板已创建: id={}, carrierId={}, region={}",
                (template.getId() + "").replace("\r", "_").replace("\n", "_"),
                (template.getCarrierId() + "").replace("\r", "_").replace("\n", "_"),
                (template.getRegionCode() + "").replace("\r", "_").replace("\n", "_"));
        return template;
    }

    @Override
    public TmsFreightTemplate updateTemplate(String id, TmsFreightTemplate template) {
        TenantGuard.requireTenant();
        if (template == null) {
            throw new IllegalArgumentException("运费模板不能为空");
        }
        template.setId(id);
        if (StringUtils.hasText(template.getRegionCode())) {
            template.setRegionCode(normalizeRegion(template.getRegionCode()));
        }
        template.setUpdateTime(LocalDateTime.now());
        updateById(template);
        return template;
    }

    @Override
    public void deleteTemplate(String id) {
        TenantGuard.requireTenant();
        TmsFreightTemplate patch = new TmsFreightTemplate();
        patch.setId(id);
        patch.setDeleted(true);
        patch.setUpdateTime(LocalDateTime.now());
        updateById(patch);
    }

    @Override
    public TmsFreightTemplate resolveTemplate(String carrierId, String regionCode) {
        TenantGuard.requireTenant();
        String region = normalizeRegion(regionCode);

        TmsFreightTemplate hit = findFirst(carrierId, region);
        if (hit == null && !DEFAULT_REGION.equals(region)) {
            hit = findFirst(carrierId, DEFAULT_REGION);
        }
        if (hit == null) {
            hit = findFirst(null, region);
        }
        if (hit == null && !DEFAULT_REGION.equals(region)) {
            hit = findFirst(null, DEFAULT_REGION);
        }
        return hit;
    }

    @Override
    public FreightQuote quote(String carrierId, String regionCode, BigDecimal weight, BigDecimal volume) {
        TmsFreightTemplate template = resolveTemplate(carrierId, regionCode);
        if (template == null) {
            throw new IllegalArgumentException(
                    "未配置运费模板: carrierId=" + carrierId + ", region=" + normalizeRegion(regionCode));
        }
        return FreightCalculator.calculate(toRule(template), weight, volume);
    }

    private TmsFreightTemplate findFirst(String carrierId, String region) {
        LambdaQueryWrapper<TmsFreightTemplate> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmsFreightTemplate::getRegionCode, region);
        if (StringUtils.hasText(carrierId)) {
            wrapper.eq(TmsFreightTemplate::getCarrierId, carrierId);
        }
        wrapper.eq(TmsFreightTemplate::getEnabled, true);
        wrapper.eq(TmsFreightTemplate::getDeleted, false);
        wrapper.orderByAsc(TmsFreightTemplate::getCreateTime);
        List<TmsFreightTemplate> list = list(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }

    private FreightRule toRule(TmsFreightTemplate template) {
        // 必填项保持原值，缺失时由计算器抛出明确错误，避免静默回落到默认费率导致少收/多收。
        FreightRule rule = new FreightRule();
        rule.setFirstWeightKg(template.getFirstWeightKg());
        rule.setAdditionalUnitKg(template.getAdditionalUnitKg());
        rule.setVolumeDivisor(template.getVolumeDivisor());
        rule.setFirstWeightFee(template.getFirstWeightFee());
        rule.setAdditionalUnitFee(template.getAdditionalUnitFee());
        rule.setRegionSurcharge(template.getRegionSurcharge());
        rule.setMinCharge(template.getMinCharge());
        return rule;
    }

    private String normalizeRegion(String regionCode) {
        return StringUtils.hasText(regionCode) ? regionCode.trim().toUpperCase() : DEFAULT_REGION;
    }
}
