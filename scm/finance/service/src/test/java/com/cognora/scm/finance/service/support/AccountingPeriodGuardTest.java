package com.cognora.scm.finance.service.support;

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

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountingPeriodGuardTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    @Mock
    private AccountingPeriodMapper accountingPeriodMapper;

    private AccountingPeriodGuard guard;

    @BeforeEach
    void setUp() {
        guard = new AccountingPeriodGuard(accountingPeriodMapper);
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void closedPeriodShouldRejectWrite() {
        when(accountingPeriodMapper.selectOne(any())).thenReturn(period(AccountingPeriodStatus.CLOSED));

        assertThrows(ServiceException.class,
                () -> guard.assertWritable(LocalDate.of(2026, 10, 15), "结算单:STL-1"));
    }

    @Test
    void closedPeriodShouldRejectWriteViaPeriodCodeFallback() {
        when(accountingPeriodMapper.selectOne(any())).thenReturn(period(AccountingPeriodStatus.CLOSED));

        assertThrows(ServiceException.class,
                () -> guard.assertWritable("2026-10", null, "发票:INV-1"));
    }

    @Test
    void openPeriodShouldAllowWrite() {
        when(accountingPeriodMapper.selectOne(any())).thenReturn(period(AccountingPeriodStatus.OPEN));

        assertDoesNotThrow(() -> guard.assertWritable(LocalDate.of(2026, 10, 15), "结算单:STL-1"));
    }

    @Test
    void reopenedPeriodShouldAllowWrite() {
        when(accountingPeriodMapper.selectOne(any())).thenReturn(period(AccountingPeriodStatus.REOPENED));

        assertDoesNotThrow(() -> guard.assertWritable(LocalDate.of(2026, 10, 15), "结算单:STL-1"));
    }

    @Test
    void missingPeriodShouldAllowWrite() {
        when(accountingPeriodMapper.selectOne(any())).thenReturn(null);

        assertDoesNotThrow(() -> guard.assertWritable(LocalDate.of(2026, 9, 1), "结算单:STL-1"));
    }

    @Test
    void nullBusinessDateShouldSkipCheck() {
        assertDoesNotThrow(() -> guard.assertWritable((LocalDate) null, "结算单:STL-1"));
    }

    private AccountingPeriod period(AccountingPeriodStatus status) {
        AccountingPeriod period = new AccountingPeriod();
        period.setTenantId(TENANT.toString());
        period.setPeriodCode("2026-10");
        period.setStatus(status.getCode());
        period.setDeleted(false);
        return period;
    }
}
