package com.cognora.scm.finance.service.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.scm.finance.domain.dto.TaxRateResolution;
import com.cognora.scm.finance.domain.entity.TaxRate;
import com.cognora.scm.finance.domain.enums.TaxRateSource;
import com.cognora.scm.finance.domain.support.FinanceAmount;
import com.cognora.scm.finance.mapper.TaxRateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 税率解析：结算与发票取税率都走这里，保证口径一致。
 *
 * <p>匹配顺序：</p>
 * <ol>
 *   <li>在业务日期落入 {@code [effective_from, effective_to]} 的启用规则中，按
 *       品类 / 地区匹配（规则该维度为空表示不限）；命中多条时取
 *       {@code effective_from} 最新的一条，同日再取创建时间最新的一条，返回
 *       {@code source=RULE}。</li>
 *   <li>未命中规则时取租户默认税率（{@code is_default=true}，同样要求业务日期
 *       在生效区间内，多条取最新生效），返回 {@code source=DEFAULT}。</li>
 *   <li>仍无命中时取配置 {@code finance.tax.default-rate}（默认 0.13），返回
 *       {@code source=DEFAULT} 且 {@code ruleId=null}。</li>
 * </ol>
 *
 * <p>停用（{@code enabled=false}）与已删除规则不参与匹配。</p>
 */
@Slf4j
@Component
public class TaxRateResolver {

    private final TaxRateMapper taxRateMapper;

    /** 租户未配置默认税率时的兜底值；与发票 tax_rate 一致用 0..1 小数。 */
    private final BigDecimal configDefaultRate;

    public TaxRateResolver(TaxRateMapper taxRateMapper,
                           @Value("${finance.tax.default-rate:0.13}") BigDecimal configDefaultRate) {
        this.taxRateMapper = taxRateMapper;
        this.configDefaultRate = FinanceAmount.rate(configDefaultRate, "配置默认税率");
    }

    /**
     * @param category     品类编码，可为空
     * @param region       地区编码，可为空
     * @param businessDate 业务日期（结算/开票日期）；为空按当天
     */
    public TaxRateResolution resolve(String category, String region, LocalDate businessDate) {
        LocalDate date = businessDate == null ? LocalDate.now() : businessDate;

        List<TaxRate> candidates = taxRateMapper.selectList(Wrappers.<TaxRate>lambdaQuery()
                .eq(TaxRate::getTenantId, TenantContextHolder.getRequiredTenantId().toString())
                .eq(TaxRate::getDeleted, false)
                .eq(TaxRate::getEnabled, true));
        if (candidates == null) {
            candidates = List.of();
        }
        List<TaxRate> inRange = candidates.stream()
                .filter(rate -> inEffectiveRange(rate, date))
                .toList();

        TaxRate rule = pickLatest(inRange.stream()
                .filter(rate -> !Boolean.TRUE.equals(rate.getIsDefault()))
                .filter(rate -> dimensionMatches(rate.getCategory(), category))
                .filter(rate -> dimensionMatches(rate.getRegion(), region))
                .toList());
        if (rule != null) {
            return new TaxRateResolution(
                    FinanceAmount.rate(rule.getRate(), "税率"),
                    TaxRateSource.RULE,
                    rule.getId(),
                    rule.getEffectiveFrom());
        }

        TaxRate tenantDefault = pickLatest(inRange.stream()
                .filter(rate -> Boolean.TRUE.equals(rate.getIsDefault()))
                .toList());
        if (tenantDefault != null) {
            return new TaxRateResolution(
                    FinanceAmount.rate(tenantDefault.getRate(), "税率"),
                    TaxRateSource.DEFAULT,
                    tenantDefault.getId(),
                    tenantDefault.getEffectiveFrom());
        }

        log.debug("未命中税率规则，使用配置默认税率 {}（category={}, region={}, date={}）",
                configDefaultRate.toPlainString(), category, region, date);
        return new TaxRateResolution(configDefaultRate, TaxRateSource.DEFAULT, null, null);
    }

    /**
     * 业务日期必须落在 {@code [effective_from, effective_to]} 内；结束为空表示长期有效。
     */
    private static boolean inEffectiveRange(TaxRate rate, LocalDate date) {
        if (rate.getEffectiveFrom() == null || date.isBefore(rate.getEffectiveFrom())) {
            return false;
        }
        return rate.getEffectiveTo() == null || !date.isAfter(rate.getEffectiveTo());
    }

    /**
     * 规则的维度为空表示不限；规则有值时必须与请求完全一致（忽略大小写）。
     */
    private static boolean dimensionMatches(String ruleValue, String requested) {
        if (ruleValue == null || ruleValue.isBlank()) {
            return true;
        }
        return requested != null && !requested.isBlank()
                && ruleValue.trim().equalsIgnoreCase(requested.trim());
    }

    /**
     * 取 {@code effective_from} 最新的一条；同日取 {@code create_time} 最新。
     */
    private static TaxRate pickLatest(List<TaxRate> rules) {
        TaxRate latest = null;
        for (TaxRate candidate : rules) {
            if (latest == null) {
                latest = candidate;
                continue;
            }
            int byEffective = candidate.getEffectiveFrom().compareTo(latest.getEffectiveFrom());
            if (byEffective > 0) {
                latest = candidate;
            } else if (byEffective == 0 && isCreatedLater(candidate, latest)) {
                latest = candidate;
            }
        }
        return latest;
    }

    private static boolean isCreatedLater(TaxRate candidate, TaxRate current) {
        LocalDateTime candidateCreated = candidate.getCreateTime();
        LocalDateTime currentCreated = current.getCreateTime();
        if (candidateCreated == null) {
            return false;
        }
        return currentCreated == null || candidateCreated.isAfter(currentCreated);
    }
}
