package com.cognora.scm.finance.service.impl;

import com.cognora.platform.common.exception.ServiceException;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.scm.finance.domain.entity.AccountingPeriod;
import com.cognora.scm.finance.domain.enums.AccountingPeriodStatus;
import com.cognora.scm.finance.mapper.AccountingPeriodMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountingPeriodServiceImplTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    @Mock
    private AccountingPeriodMapper mapper;

    private AccountingPeriodServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountingPeriodServiceImpl(mapper);
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void openPeriodShouldDeriveMonthRange() {
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(AccountingPeriod.class))).thenReturn(1);

        AccountingPeriod period = service.openPeriod("2026-10");

        assertEquals("2026-10", period.getPeriodCode());
        assertEquals("2026-10-01", period.getStartDate().toString());
        assertEquals("2026-10-31", period.getEndDate().toString());
        assertEquals(AccountingPeriodStatus.OPEN.getCode(), period.getStatus());
        assertEquals(TENANT.toString(), period.getTenantId());
        verify(mapper).insert(period);
    }

    @Test
    void openPeriodShouldRejectDuplicate() {
        AccountingPeriod existing = new AccountingPeriod();
        existing.setPeriodCode("2026-10");
        when(mapper.selectOne(any())).thenReturn(existing);

        assertThrows(IllegalArgumentException.class, () -> service.openPeriod("2026-10"));
        verify(mapper, never()).insert(any(AccountingPeriod.class));
    }

    @Test
    void openPeriodShouldRejectInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> service.openPeriod("2026-13"));
    }

    @Test
    void closePeriodShouldTransitionOpenToClosed() {
        AccountingPeriod period = period(AccountingPeriodStatus.OPEN);
        when(mapper.selectById("ap-1")).thenReturn(period);
        when(mapper.updateById(any(AccountingPeriod.class))).thenReturn(1);

        AccountingPeriod closed = service.closePeriod("ap-1", "user-1");

        assertEquals(AccountingPeriodStatus.CLOSED.getCode(), closed.getStatus());
        assertEquals("user-1", closed.getClosedBy());
        assertEquals(true, closed.getClosedAt() != null);
        verify(mapper).updateById(period);
    }

    @Test
    void closePeriodShouldRejectAlreadyClosed() {
        when(mapper.selectById("ap-1")).thenReturn(period(AccountingPeriodStatus.CLOSED));

        assertThrows(ServiceException.class, () -> service.closePeriod("ap-1", "user-1"));
        verify(mapper, never()).updateById(any(AccountingPeriod.class));
    }

    @Test
    void closePeriodShouldRejectForeignTenant() {
        AccountingPeriod period = period(AccountingPeriodStatus.OPEN);
        period.setTenantId("019a0aee-0000-7000-8000-000000000099");
        when(mapper.selectById("ap-1")).thenReturn(period);

        assertThrows(AccessDeniedException.class, () -> service.closePeriod("ap-1", "user-1"));
        verify(mapper, never()).updateById(any(AccountingPeriod.class));
    }

    @Test
    void getCurrentPeriodShouldReturnCurrentMonthRecord() {
        AccountingPeriod current = period(AccountingPeriodStatus.OPEN);
        when(mapper.selectOne(any())).thenReturn(current);

        AccountingPeriod result = service.getCurrentPeriod();

        assertNotNull(result);
        assertEquals("2026-10", result.getPeriodCode());
    }

    @Test
    void getCurrentPeriodShouldReturnNullWhenMonthNotOpened() {
        when(mapper.selectOne(any())).thenReturn(null);

        assertNull(service.getCurrentPeriod());
    }

    @Test
    void listClosedPeriodsShouldReturnClosedRecords() {
        when(mapper.selectList(any())).thenReturn(List.of(period(AccountingPeriodStatus.CLOSED)));

        List<AccountingPeriod> result = service.listClosedPeriods();

        assertEquals(1, result.size());
        assertEquals(AccountingPeriodStatus.CLOSED.getCode(), result.get(0).getStatus());
    }

    @Test
    void closePeriodShouldTransitionReopenedBackToClosed() {
        AccountingPeriod period = period(AccountingPeriodStatus.REOPENED);
        when(mapper.selectById("ap-1")).thenReturn(period);
        when(mapper.updateById(any(AccountingPeriod.class))).thenReturn(1);

        AccountingPeriod closed = service.closePeriod("ap-1", "user-1");

        assertEquals(AccountingPeriodStatus.CLOSED.getCode(), closed.getStatus());
        assertEquals("user-1", closed.getClosedBy());
        assertTrue(closed.getClosedAt() != null);
        verify(mapper).updateById(period);
    }

    @Test
    void reopenPeriodShouldTransitionClosedToReopenedWithTrail() {
        AccountingPeriod period = period(AccountingPeriodStatus.CLOSED);
        when(mapper.selectById("ap-1")).thenReturn(period);
        when(mapper.updateById(any(AccountingPeriod.class))).thenReturn(1);

        AccountingPeriod reopened = service.reopenPeriod("ap-1", "  冲销漏记费用  ", "user-2");

        assertEquals(AccountingPeriodStatus.REOPENED.getCode(), reopened.getStatus());
        assertEquals("user-2", reopened.getReopenedBy());
        assertEquals("冲销漏记费用", reopened.getReopenReason());
        assertTrue(reopened.getReopenedAt() != null);
        verify(mapper).updateById(period);
    }

    @Test
    void reopenPeriodShouldRejectOpenPeriod() {
        when(mapper.selectById("ap-1")).thenReturn(period(AccountingPeriodStatus.OPEN));

        assertThrows(ServiceException.class, () -> service.reopenPeriod("ap-1", "reason", "user-2"));
        verify(mapper, never()).updateById(any(AccountingPeriod.class));
    }

    @Test
    void reopenPeriodShouldRejectAlreadyReopened() {
        when(mapper.selectById("ap-1")).thenReturn(period(AccountingPeriodStatus.REOPENED));

        assertThrows(ServiceException.class, () -> service.reopenPeriod("ap-1", "reason", "user-2"));
        verify(mapper, never()).updateById(any(AccountingPeriod.class));
    }

    @Test
    void reopenPeriodShouldRejectBlankReasonBeforeLoading() {
        assertThrows(IllegalArgumentException.class, () -> service.reopenPeriod("ap-1", "   ", "user-2"));

        verify(mapper, never()).selectById("ap-1");
    }

    @Test
    void reopenPeriodShouldRejectBlankOperator() {
        assertThrows(IllegalArgumentException.class, () -> service.reopenPeriod("ap-1", "reason", "  "));

        verify(mapper, never()).selectById("ap-1");
    }

    @Test
    void reopenPeriodShouldRejectUnknownPeriod() {
        when(mapper.selectById("ap-1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.reopenPeriod("ap-1", "reason", "user-2"));
    }

    @Test
    void reopenPeriodShouldRejectForeignTenant() {
        AccountingPeriod period = period(AccountingPeriodStatus.CLOSED);
        period.setTenantId("019a0aee-0000-7000-8000-000000000099");
        when(mapper.selectById("ap-1")).thenReturn(period);

        assertThrows(AccessDeniedException.class,
                () -> service.reopenPeriod("ap-1", "reason", "user-2"));
        verify(mapper, never()).updateById(any(AccountingPeriod.class));
    }

    private AccountingPeriod period(AccountingPeriodStatus status) {
        AccountingPeriod period = new AccountingPeriod();
        period.setId("ap-1");
        period.setTenantId(TENANT.toString());
        period.setPeriodCode("2026-10");
        period.setStatus(status.getCode());
        period.setDeleted(false);
        period.setCreateTime(LocalDateTime.now());
        return period;
    }
}
