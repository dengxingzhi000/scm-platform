package com.cognora.lms.logistics.service.impl;

import com.cognora.lms.logistics.domain.entity.TmsClaimOrder;
import com.cognora.lms.logistics.domain.entity.TmsExceptionCase;
import com.cognora.lms.logistics.domain.exceptioncase.ClaimOrderStatus;
import com.cognora.lms.logistics.domain.exceptioncase.ExceptionCaseStatus;
import com.cognora.lms.logistics.mapper.TmsClaimOrderMapper;
import com.cognora.lms.logistics.service.ITmsExceptionCaseService;
import com.cognora.platform.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 批次 3：理赔单创建与状态机用例。
 * 覆盖金额校验、异常件状态门槛、审批/赔付流转与跨租户 403。
 */
class TmsClaimOrderServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private TmsClaimOrderMapper mapper;
    private ITmsExceptionCaseService exceptionCaseService;
    private TmsClaimOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(TmsClaimOrderMapper.class);
        exceptionCaseService = mock(ITmsExceptionCaseService.class);
        service = new TmsClaimOrderServiceImpl(exceptionCaseService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createClaimShouldPersistDraftForProcessingCase() {
        when(exceptionCaseService.getById("case-1"))
                .thenReturn(exceptionCase(ExceptionCaseStatus.PROCESSING));

        TmsClaimOrder claim = service.createClaim("case-1", new BigDecimal("12.345"), "货物破损", "ev-1", "tester");

        ArgumentCaptor<TmsClaimOrder> captor = ArgumentCaptor.forClass(TmsClaimOrder.class);
        verify(mapper).insert(captor.capture());
        TmsClaimOrder saved = captor.getValue();
        assertEquals("case-1", saved.getCaseId());
        assertEquals("WB-1", saved.getWaybillNo());
        assertEquals(0, saved.getClaimAmount().compareTo(new BigDecimal("12.35")));
        assertEquals("CNY", saved.getCurrency());
        assertEquals(ClaimOrderStatus.DRAFT, saved.getStatus());
        assertEquals("tester", saved.getCreateBy());
        assertEquals(saved.getId(), claim.getId());
    }

    @Test
    void createClaimForNewCaseShouldBeRejected() {
        when(exceptionCaseService.getById("case-1"))
                .thenReturn(exceptionCase(ExceptionCaseStatus.NEW));

        assertThrows(IllegalStateException.class,
                () -> service.createClaim("case-1", new BigDecimal("10"), "破损", null, "tester"));
        verify(mapper, never()).insert(any(TmsClaimOrder.class));
    }

    @Test
    void createClaimWithNonPositiveAmountShouldBeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createClaim("case-1", BigDecimal.ZERO, "破损", null, "tester"));
        verify(mapper, never()).insert(any(TmsClaimOrder.class));
    }

    @Test
    void createClaimOutsideTenantShouldReturn403() {
        when(exceptionCaseService.getById("case-1")).thenReturn(null);

        assertThrows(AccessDeniedException.class,
                () -> service.createClaim("case-1", new BigDecimal("10"), "破损", null, "tester"));
    }

    @Test
    void submitShouldRecordSubmitter() {
        when(mapper.selectById("claim-1")).thenReturn(claim(ClaimOrderStatus.DRAFT));

        TmsClaimOrder submitted = service.transition("claim-1", "submitted", "operator-a", "提交审批");

        ArgumentCaptor<TmsClaimOrder> captor = ArgumentCaptor.forClass(TmsClaimOrder.class);
        verify(mapper).updateById(captor.capture());
        TmsClaimOrder patch = captor.getValue();
        assertEquals(ClaimOrderStatus.SUBMITTED, patch.getStatus());
        assertEquals("operator-a", patch.getSubmittedBy());
        assertNotNull(patch.getSubmittedTime());
        assertEquals("提交审批", patch.getRemark());
        assertEquals(ClaimOrderStatus.SUBMITTED, submitted.getStatus());
    }

    @Test
    void draftShouldNotSkipApproval() {
        when(mapper.selectById("claim-1")).thenReturn(claim(ClaimOrderStatus.DRAFT));

        assertThrows(IllegalStateException.class,
                () -> service.transition("claim-1", "APPROVED", "operator-a", null));
        verify(mapper, never()).updateById(any(TmsClaimOrder.class));
    }

    @Test
    void submittedShouldBeApprovedByReviewer() {
        when(mapper.selectById("claim-1")).thenReturn(claim(ClaimOrderStatus.SUBMITTED));

        service.transition("claim-1", "APPROVED", "reviewer-a", "同意赔付");

        ArgumentCaptor<TmsClaimOrder> captor = ArgumentCaptor.forClass(TmsClaimOrder.class);
        verify(mapper).updateById(captor.capture());
        assertEquals("reviewer-a", captor.getValue().getReviewedBy());
        assertNotNull(captor.getValue().getReviewedTime());
    }

    @Test
    void approvedShouldBePaid() {
        when(mapper.selectById("claim-1")).thenReturn(claim(ClaimOrderStatus.APPROVED));

        service.transition("claim-1", "PAID", "finance-a", "已打款");

        ArgumentCaptor<TmsClaimOrder> captor = ArgumentCaptor.forClass(TmsClaimOrder.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(ClaimOrderStatus.PAID, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getPaidTime());
    }

    @Test
    void paidClaimShouldBeTerminal() {
        when(mapper.selectById("claim-1")).thenReturn(claim(ClaimOrderStatus.PAID));

        assertThrows(IllegalStateException.class,
                () -> service.transition("claim-1", "SUBMITTED", "operator-a", null));
    }

    @Test
    void unknownTargetStatusShouldBeRejected() {
        when(mapper.selectById("claim-1")).thenReturn(claim(ClaimOrderStatus.DRAFT));

        assertThrows(IllegalArgumentException.class,
                () -> service.transition("claim-1", "UNKNOWN", "operator-a", null));
    }

    @Test
    void transitionOutsideTenantShouldReturn403() {
        when(mapper.selectById("claim-1")).thenReturn(null);

        assertThrows(AccessDeniedException.class,
                () -> service.transition("claim-1", "SUBMITTED", "operator-a", null));
    }

    private TmsExceptionCase exceptionCase(String status) {
        TmsExceptionCase exceptionCase = new TmsExceptionCase();
        exceptionCase.setId("case-1");
        exceptionCase.setWaybillNo("WB-1");
        exceptionCase.setStatus(status);
        return exceptionCase;
    }

    private TmsClaimOrder claim(String status) {
        TmsClaimOrder claim = new TmsClaimOrder();
        claim.setId("claim-1");
        claim.setCaseId("case-1");
        claim.setWaybillNo("WB-1");
        claim.setStatus(status);
        return claim;
    }
}
