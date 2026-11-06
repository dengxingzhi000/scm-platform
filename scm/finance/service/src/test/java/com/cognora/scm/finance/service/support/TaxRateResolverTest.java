package com.cognora.scm.finance.service.support;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.scm.finance.domain.dto.TaxRateResolution;
import com.cognora.scm.finance.domain.entity.TaxRate;
import com.cognora.scm.finance.domain.enums.TaxRateSource;
import com.cognora.scm.finance.mapper.TaxRateMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 税率匹配优先级：区间内最新生效规则 > 租户默认 > 配置默认。
 */
@ExtendWith(MockitoExtension.class)
class TaxRateResolverTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 11, 6);

    @Mock
    private TaxRateMapper taxRateMapper;

    private TaxRateResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TaxRateResolver(taxRateMapper, new BigDecimal("0.13"));
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void latestEffectiveFromWinsAmongMatchingRules() {
        TaxRate oldRule = rule("r-old", "ELECTRONICS", null, "0.09", LocalDate.of(2026, 1, 1), null, false);
        TaxRate newRule = rule("r-new", "ELECTRONICS", null, "0.13", LocalDate.of(2026, 7, 1), null, false);
        TaxRate genericRule = rule("r-generic", null, null, "0.06", LocalDate.of(2025, 1, 1), null, false);
        when(taxRateMapper.selectList(any())).thenReturn(List.of(oldRule, newRule, genericRule));

        TaxRateResolution resolution = resolver.resolve("ELECTRONICS", "CN", BUSINESS_DATE);

        assertEquals(TaxRateSource.RULE, resolution.getSource());
        assertEquals("r-new", resolution.getRuleId());
        assertEquals(new BigDecimal("0.1300"), resolution.getRate());
        assertEquals(LocalDate.of(2026, 7, 1), resolution.getEffectiveFrom());
    }

    @Test
    void ruleOutsideEffectiveRangeShouldBeSkipped() {
        TaxRate futureRule = rule("r-future", "ELECTRONICS", null, "0.09",
                LocalDate.of(2026, 12, 1), null, false);
        when(taxRateMapper.selectList(any())).thenReturn(List.of(futureRule));

        TaxRateResolution resolution = resolver.resolve("ELECTRONICS", "CN", BUSINESS_DATE);

        assertEquals(TaxRateSource.DEFAULT, resolution.getSource());
        assertNull(resolution.getRuleId());
        assertEquals(new BigDecimal("0.1300"), resolution.getRate());
    }

    @Test
    void expiredRuleShouldBeSkipped() {
        TaxRate expired = rule("r-expired", "ELECTRONICS", null, "0.09",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), false);
        when(taxRateMapper.selectList(any())).thenReturn(List.of(expired));

        TaxRateResolution resolution = resolver.resolve("ELECTRONICS", "CN", BUSINESS_DATE);

        assertEquals(TaxRateSource.DEFAULT, resolution.getSource());
    }

    @Test
    void categoryMismatchShouldFallBackToTenantDefault() {
        TaxRate electronics = rule("r-elec", "ELECTRONICS", null, "0.13", LocalDate.of(2026, 1, 1), null, false);
        TaxRate tenantDefault = rule("r-default", null, null, "0.06", LocalDate.of(2026, 1, 1), null, true);
        when(taxRateMapper.selectList(any())).thenReturn(List.of(electronics, tenantDefault));

        TaxRateResolution resolution = resolver.resolve("FOOD", "CN", BUSINESS_DATE);

        assertEquals(TaxRateSource.DEFAULT, resolution.getSource());
        assertEquals("r-default", resolution.getRuleId());
        assertEquals(new BigDecimal("0.0600"), resolution.getRate());
    }

    @Test
    void tenantDefaultWinsOverConfigDefault() {
        TaxRate tenantDefault = rule("r-default", null, null, "0.06", LocalDate.of(2026, 1, 1), null, true);
        when(taxRateMapper.selectList(any())).thenReturn(List.of(tenantDefault));

        TaxRateResolution resolution = resolver.resolve(null, null, BUSINESS_DATE);

        assertEquals(TaxRateSource.DEFAULT, resolution.getSource());
        assertEquals("r-default", resolution.getRuleId());
    }

    @Test
    void configDefaultUsedWhenNothingMatches() {
        when(taxRateMapper.selectList(any())).thenReturn(List.of());

        TaxRateResolution resolution = resolver.resolve("ELECTRONICS", "CN", BUSINESS_DATE);

        assertEquals(TaxRateSource.DEFAULT, resolution.getSource());
        assertNull(resolution.getRuleId());
        assertNull(resolution.getEffectiveFrom());
        assertEquals(new BigDecimal("0.1300"), resolution.getRate());
    }

    @Test
    void regionSpecificRuleShouldBeatGenericRuleWithSameEffectiveDate() {
        TaxRate generic = rule("r-generic", "ELECTRONICS", null, "0.09",
                LocalDate.of(2026, 7, 1), null, false);
        generic.setCreateTime(LocalDateTime.of(2026, 7, 2, 10, 0));
        TaxRate regional = rule("r-region", "ELECTRONICS", "CN", "0.11",
                LocalDate.of(2026, 7, 1), null, false);
        regional.setCreateTime(LocalDateTime.of(2026, 7, 3, 10, 0));
        when(taxRateMapper.selectList(any())).thenReturn(List.of(generic, regional));

        TaxRateResolution resolution = resolver.resolve("ELECTRONICS", "CN", BUSINESS_DATE);

        assertEquals(TaxRateSource.RULE, resolution.getSource());
        assertEquals("r-region", resolution.getRuleId());
        assertEquals(new BigDecimal("0.1100"), resolution.getRate());
    }

    private TaxRate rule(String id, String category, String region, String rate,
                         LocalDate from, LocalDate to, boolean isDefault) {
        TaxRate rule = new TaxRate();
        rule.setId(id);
        rule.setTenantId(TENANT.toString());
        rule.setCategory(category);
        rule.setRegion(region);
        rule.setRate(new BigDecimal(rate));
        rule.setEffectiveFrom(from);
        rule.setEffectiveTo(to);
        rule.setIsDefault(isDefault);
        rule.setEnabled(true);
        rule.setDeleted(false);
        return rule;
    }
}
