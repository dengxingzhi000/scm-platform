package com.cognora.lms.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cognora.lms.logistics.domain.entity.TmsFreightReconciliation;
import com.cognora.lms.logistics.domain.entity.TmsWaybill;
import com.cognora.lms.logistics.domain.freight.FreightDiffClassifier;
import com.cognora.lms.logistics.domain.freight.FreightQuote;
import com.cognora.lms.logistics.mapper.TmsFreightReconciliationMapper;
import com.cognora.lms.logistics.service.ITmsFreightTemplateService;
import com.cognora.lms.logistics.service.ITmsWaybillService;
import com.cognora.platform.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 批次 2：运费对账用例（差异分类、幂等、跨租户拒绝）。
 */
class TmsFreightReconciliationTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private TmsFreightReconciliationMapper mapper;
    private ITmsWaybillService waybillService;
    private ITmsFreightTemplateService templateService;
    private TmsFreightReconciliationServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(TmsFreightReconciliationMapper.class);
        waybillService = mock(ITmsWaybillService.class);
        templateService = mock(ITmsFreightTemplateService.class);
        service = new TmsFreightReconciliationServiceImpl(waybillService, templateService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void overchargedBillShouldProducePositiveDiff() {
        when(waybillService.getById("w1")).thenReturn(waybill());
        when(templateService.quote(anyString(), anyString(), any(), any())).thenReturn(quote("44.00"));
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(mapper.insert(any(TmsFreightReconciliation.class))).thenReturn(1);

        TmsFreightReconciliation record = service.reconcile("w1", "SH", new BigDecimal("50.00"), "B202611", "tester");

        ArgumentCaptor<TmsFreightReconciliation> captor =
                ArgumentCaptor.forClass(TmsFreightReconciliation.class);
        verify(mapper).insert(captor.capture());
        TmsFreightReconciliation saved = captor.getValue();
        assertEquals(0, saved.getExpectedAmount().compareTo(new BigDecimal("44.00")));
        assertEquals(0, saved.getDiffAmount().compareTo(new BigDecimal("6.00")));
        assertEquals(FreightDiffClassifier.OVERCHARGE, saved.getDiffType());
        assertEquals(TmsFreightReconciliation.STATUS_OPEN, saved.getStatus());
        assertEquals("WB-9", saved.getWaybillNo());
        assertEquals("B202611", saved.getBatchNo());
        assertEquals("tester", saved.getCreateBy());
        assertSame(saved, record);
    }

    @Test
    void underchargedBillShouldProduceNegativeDiff() {
        when(waybillService.getById("w1")).thenReturn(waybill());
        when(templateService.quote(anyString(), anyString(), any(), any())).thenReturn(quote("44.00"));
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(mapper.insert(any(TmsFreightReconciliation.class))).thenReturn(1);

        TmsFreightReconciliation record = service.reconcile("w1", "SH", new BigDecimal("40.00"), null, null);

        assertEquals(0, record.getDiffAmount().compareTo(new BigDecimal("-4.00")));
        assertEquals(FreightDiffClassifier.UNDERCHARGE, record.getDiffType());
        assertEquals(TmsFreightReconciliationServiceImpl.DEFAULT_BATCH, record.getBatchNo());
    }

    @Test
    void matchedBillShouldBeClassifiedAsMatch() {
        when(waybillService.getById("w1")).thenReturn(waybill());
        when(templateService.quote(anyString(), anyString(), any(), any())).thenReturn(quote("44.00"));
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(mapper.insert(any(TmsFreightReconciliation.class))).thenReturn(1);

        TmsFreightReconciliation record = service.reconcile("w1", "SH", new BigDecimal("44.00"), "B202611", "tester");

        assertEquals(0, record.getDiffAmount().compareTo(BigDecimal.ZERO));
        assertEquals(FreightDiffClassifier.MATCH, record.getDiffType());
    }

    @Test
    void repeatedReconciliationForSameBatchShouldBeIdempotent() {
        when(waybillService.getById("w1")).thenReturn(waybill());
        TmsFreightReconciliation existing = new TmsFreightReconciliation();
        existing.setId("recon-1");
        existing.setBatchNo("B202611");
        existing.setWaybillNo("WB-9");
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));

        TmsFreightReconciliation record = service.reconcile(
                "w1", "SH", new BigDecimal("50.00"), "B202611", "tester");

        assertSame(existing, record);
        verify(mapper, never()).insert(any(TmsFreightReconciliation.class));
        verify(templateService, never()).quote(anyString(), anyString(), any(), any());
    }

    @Test
    void concurrentDuplicateInsertShouldReturnExistingRecord() {
        when(waybillService.getById("w1")).thenReturn(waybill());
        when(templateService.quote(anyString(), anyString(), any(), any())).thenReturn(quote("44.00"));
        TmsFreightReconciliation existing = new TmsFreightReconciliation();
        existing.setId("recon-1");
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(), List.of(existing));
        when(mapper.insert(any(TmsFreightReconciliation.class)))
                .thenThrow(new DuplicateKeyException("uk_tms_freight_recon_batch"));

        TmsFreightReconciliation record = service.reconcile(
                "w1", "SH", new BigDecimal("50.00"), "B202611", "tester");

        assertSame(existing, record);
    }

    @Test
    void waybillOutsideTenantShouldBeRejected() {
        when(waybillService.getById("w1")).thenReturn(null);

        assertThrows(AccessDeniedException.class,
                () -> service.reconcile("w1", "SH", new BigDecimal("50.00"), "B202611", "tester"));
        verify(mapper, never()).insert(any(TmsFreightReconciliation.class));
    }

    @Test
    void missingTenantContextShouldBeRejected() {
        TenantContextHolder.clear();

        assertThrows(AccessDeniedException.class,
                () -> service.reconcile("w1", "SH", new BigDecimal("50.00"), "B202611", "tester"));
        verify(mapper, never()).insert(any(TmsFreightReconciliation.class));
    }

    @Test
    void negativeBilledAmountShouldBeRejected() {
        when(waybillService.getById("w1")).thenReturn(waybill());

        assertThrows(IllegalArgumentException.class,
                () -> service.reconcile("w1", "SH", new BigDecimal("-1"), "B202611", "tester"));
    }

    private TmsWaybill waybill() {
        TmsWaybill waybill = new TmsWaybill();
        waybill.setId("w1");
        waybill.setWaybillNo("WB-9");
        waybill.setCarrierId("carrier-1");
        waybill.setGoodsWeight(new BigDecimal("10"));
        waybill.setGoodsVolume(new BigDecimal("0"));
        return waybill;
    }

    private FreightQuote quote(String total) {
        BigDecimal amount = new BigDecimal(total);
        return new FreightQuote(new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("10"),
                new BigDecimal("10"), new BigDecimal("8.00"), new BigDecimal("36.00"),
                BigDecimal.ZERO, amount);
    }
}
