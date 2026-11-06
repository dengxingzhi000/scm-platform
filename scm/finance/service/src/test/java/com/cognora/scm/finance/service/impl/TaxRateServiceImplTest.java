package com.cognora.scm.finance.service.impl;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.scm.finance.domain.dto.CreateTaxRateRequest;
import com.cognora.scm.finance.domain.dto.TaxRateResolution;
import com.cognora.scm.finance.domain.dto.UpdateTaxRateRequest;
import com.cognora.scm.finance.domain.entity.TaxRate;
import com.cognora.scm.finance.domain.enums.TaxRateSource;
import com.cognora.scm.finance.mapper.TaxRateMapper;
import com.cognora.scm.finance.service.support.TaxRateResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxRateServiceImplTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    @Mock
    private TaxRateMapper mapper;

    @Mock
    private TaxRateResolver resolver;

    private TaxRateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaxRateServiceImpl(mapper, resolver);
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createShouldNormalizeRateAndStickTenant() {
        when(mapper.insert(any(TaxRate.class))).thenReturn(1);

        TaxRate created = service.create(request("0.13", LocalDate.of(2026, 1, 1), null, false));

        assertEquals(TENANT.toString(), created.getTenantId());
        assertEquals(new BigDecimal("0.1300"), created.getRate());
        assertTrue(created.getEnabled());
        assertFalse(created.getIsDefault());
        verify(mapper).insert(created);
    }

    @Test
    void createShouldRejectRateAboveOne() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(request("1.5", LocalDate.of(2026, 1, 1), null, false)));
        verify(mapper, never()).insert(any(TaxRate.class));
    }

    @Test
    void createShouldRejectInvertedEffectiveRange() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(request("0.13", LocalDate.of(2026, 1, 1), LocalDate.of(2025, 12, 31), false)));
        verify(mapper, never()).insert(any(TaxRate.class));
    }

    @Test
    void createShouldRequireEffectiveFrom() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(request("0.13", null, null, false)));
        verify(mapper, never()).insert(any(TaxRate.class));
    }

    @Test
    void updateUnknownRuleShouldFail() {
        when(mapper.selectById("tr-1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.update("tr-1", update("0.09")));
        verify(mapper, never()).updateById(any(TaxRate.class));
    }

    @Test
    void updateForeignTenantRuleShouldBeDenied() {
        TaxRate rule = rule("tr-1", "019a0aee-0000-7000-8000-000000000099");
        when(mapper.selectById("tr-1")).thenReturn(rule);

        assertThrows(AccessDeniedException.class, () -> service.update("tr-1", update("0.09")));
        verify(mapper, never()).updateById(any(TaxRate.class));
    }

    @Test
    void updateShouldRejectInvalidRate() {
        TaxRate rule = rule("tr-1", TENANT.toString());
        when(mapper.selectById("tr-1")).thenReturn(rule);

        assertThrows(IllegalArgumentException.class, () -> service.update("tr-1", update("1.2")));
        verify(mapper, never()).updateById(any(TaxRate.class));
    }

    @Test
    void updateShouldApplyPartialChanges() {
        TaxRate rule = rule("tr-1", TENANT.toString());
        when(mapper.selectById("tr-1")).thenReturn(rule);

        UpdateTaxRateRequest request = update("0.09");
        request.setRemark("调整口径");
        TaxRate updated = service.update("tr-1", request);

        assertEquals(new BigDecimal("0.0900"), updated.getRate());
        assertEquals("调整口径", updated.getRemark());
        verify(mapper).updateById(rule);
    }

    @Test
    void disableShouldSetEnabledFalse() {
        TaxRate rule = rule("tr-1", TENANT.toString());
        rule.setEnabled(true);
        when(mapper.selectById("tr-1")).thenReturn(rule);

        TaxRate disabled = service.disable("tr-1");

        assertFalse(disabled.getEnabled());
        verify(mapper).updateById(rule);
    }

    @Test
    void disableAlreadyDisabledShouldNotWriteAgain() {
        TaxRate rule = rule("tr-1", TENANT.toString());
        rule.setEnabled(false);
        when(mapper.selectById("tr-1")).thenReturn(rule);

        service.disable("tr-1");

        verify(mapper, never()).updateById(any(TaxRate.class));
    }

    @Test
    void resolveShouldDelegateToResolver() {
        TaxRateResolution expected = new TaxRateResolution(
                new BigDecimal("0.0600"), TaxRateSource.RULE, "tr-1", LocalDate.of(2026, 1, 1));
        when(resolver.resolve("ELECTRONICS", "CN", LocalDate.of(2026, 11, 6))).thenReturn(expected);

        TaxRateResolution actual = service.resolve("ELECTRONICS", "CN", LocalDate.of(2026, 11, 6));

        assertEquals(expected, actual);
    }

    private CreateTaxRateRequest request(String rate, LocalDate from, LocalDate to, boolean isDefault) {
        CreateTaxRateRequest request = new CreateTaxRateRequest();
        request.setCategory(" ELECTRONICS ");
        request.setRegion("CN");
        request.setRate(new BigDecimal(rate));
        request.setEffectiveFrom(from);
        request.setEffectiveTo(to);
        request.setIsDefault(isDefault);
        return request;
    }

    private UpdateTaxRateRequest update(String rate) {
        UpdateTaxRateRequest request = new UpdateTaxRateRequest();
        request.setRate(new BigDecimal(rate));
        return request;
    }

    private TaxRate rule(String id, String tenantId) {
        TaxRate rule = new TaxRate();
        rule.setId(id);
        rule.setTenantId(tenantId);
        rule.setRate(new BigDecimal("0.1300"));
        rule.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        rule.setEnabled(true);
        rule.setDeleted(false);
        return rule;
    }
}
