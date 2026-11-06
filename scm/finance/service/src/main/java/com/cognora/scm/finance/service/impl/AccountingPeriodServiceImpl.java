package com.cognora.scm.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.exception.ServiceException;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.util.UUIDv7Util;
import com.cognora.scm.finance.domain.entity.AccountingPeriod;
import com.cognora.scm.finance.domain.enums.AccountingPeriodStatus;
import com.cognora.scm.finance.mapper.AccountingPeriodMapper;
import com.cognora.scm.finance.service.AccountingPeriodService;
import com.cognora.scm.finance.service.support.FinanceTenantGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountingPeriodServiceImpl implements AccountingPeriodService {

    private static final Pattern PERIOD_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final AccountingPeriodMapper accountingPeriodMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountingPeriod openPeriod(String periodCode) {
        if (!StringUtils.hasText(periodCode) || !PERIOD_PATTERN.matcher(periodCode.trim()).matches()) {
            throw new IllegalArgumentException("账期格式必须为 YYYY-MM: " + periodCode);
        }
        String normalized = periodCode.trim();
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(normalized);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("账期格式必须为 YYYY-MM: " + periodCode);
        }

        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        AccountingPeriod existing = accountingPeriodMapper.selectOne(Wrappers.<AccountingPeriod>lambdaQuery()
                .eq(AccountingPeriod::getTenantId, tenantId)
                .eq(AccountingPeriod::getPeriodCode, normalized)
                .eq(AccountingPeriod::getDeleted, false)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new IllegalArgumentException("账期已存在: " + normalized);
        }

        AccountingPeriod period = new AccountingPeriod();
        period.setId(UUIDv7Util.generateString());
        period.setTenantId(tenantId);
        period.setPeriodCode(normalized);
        period.setStartDate(yearMonth.atDay(1));
        period.setEndDate(yearMonth.atEndOfMonth());
        period.setStatus(AccountingPeriodStatus.OPEN.getCode());
        period.setDeleted(false);
        period.setCreateTime(LocalDateTime.now());
        period.setUpdateTime(LocalDateTime.now());

        if (accountingPeriodMapper.insert(period) <= 0) {
            throw new IllegalStateException("开启账期失败: " + normalized);
        }
        log.info("账期已开启: tenant={}, period={}", tenantId, normalized);
        return period;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountingPeriod closePeriod(String id, String closedBy) {
        AccountingPeriod period = accountingPeriodMapper.selectById(id);
        if (period == null || Boolean.TRUE.equals(period.getDeleted())) {
            throw new IllegalArgumentException("账期不存在: " + id);
        }
        FinanceTenantGuard.verifyOwnership(period.getTenantId());

        AccountingPeriodStatus current = AccountingPeriodStatus.fromCode(period.getStatus());
        if (!current.canTransitionTo(AccountingPeriodStatus.CLOSED)) {
            throw new ServiceException(ResultCode.CONFLICT.getCode(), HttpStatus.CONFLICT,
                    "非法账期流转: " + current + " -> CLOSED");
        }

        period.setStatus(AccountingPeriodStatus.CLOSED.getCode());
        period.setClosedBy(closedBy);
        period.setClosedAt(LocalDateTime.now());
        period.setUpdateTime(LocalDateTime.now());
        accountingPeriodMapper.updateById(period);
        log.info("账期已关账: id={}, period={}", id, period.getPeriodCode());
        return period;
    }

    @Override
    public AccountingPeriod getById(String id) {
        AccountingPeriod period = accountingPeriodMapper.selectById(id);
        if (period == null || Boolean.TRUE.equals(period.getDeleted())) {
            throw new IllegalArgumentException("账期不存在: " + id);
        }
        FinanceTenantGuard.verifyOwnership(period.getTenantId());
        return period;
    }

    @Override
    public AccountingPeriod getCurrentPeriod() {
        String periodCode = YearMonth.now().toString();
        return accountingPeriodMapper.selectOne(Wrappers.<AccountingPeriod>lambdaQuery()
                .eq(AccountingPeriod::getTenantId, TenantContextHolder.getRequiredTenantId().toString())
                .eq(AccountingPeriod::getPeriodCode, periodCode)
                .eq(AccountingPeriod::getDeleted, false)
                .last("LIMIT 1"));
    }

    @Override
    public List<AccountingPeriod> listClosedPeriods() {
        return accountingPeriodMapper.selectList(Wrappers.<AccountingPeriod>lambdaQuery()
                .eq(AccountingPeriod::getTenantId, TenantContextHolder.getRequiredTenantId().toString())
                .eq(AccountingPeriod::getDeleted, false)
                .eq(AccountingPeriod::getStatus, AccountingPeriodStatus.CLOSED.getCode())
                .orderByDesc(AccountingPeriod::getPeriodCode));
    }

    @Override
    public Page<AccountingPeriod> pageQuery(int page, int size, Integer status) {
        LambdaQueryWrapper<AccountingPeriod> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(AccountingPeriod::getTenantId, TenantContextHolder.getRequiredTenantId().toString())
                .eq(AccountingPeriod::getDeleted, false)
                .eq(status != null, AccountingPeriod::getStatus, status)
                .orderByDesc(AccountingPeriod::getPeriodCode);
        return accountingPeriodMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
