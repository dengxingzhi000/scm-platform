package com.cognora.scm.finance.service.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.exception.ServiceException;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.scm.finance.domain.entity.AccountingPeriod;
import com.cognora.scm.finance.domain.enums.AccountingPeriodStatus;
import com.cognora.scm.finance.mapper.AccountingPeriodMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 关账守卫：某账期一旦置为已结账，该期间内的单据写入一律拒绝。
 *
 * <p>只有 {@link AccountingPeriodStatus#CLOSED} 会拦截写入；{@code OPEN} 与
 * {@code REOPENED} 均放行。重开（CLOSED → REOPENED）是已结账账期唯一的合法
 * 回退路径，重开后再关账重新拦截。</p>
 *
 * <p>没有账期记录时视为未关账（账期是可选的管理动作，启用前不影响既有业务）。
 * 拒绝返回 409，避免与「参数错误」混淆。</p>
 */
@Component
@RequiredArgsConstructor
public class AccountingPeriodGuard {

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AccountingPeriodMapper accountingPeriodMapper;

    /**
     * 按业务日期检查账期是否已关账。
     */
    public void assertWritable(LocalDate businessDate, String documentNo) {
        if (businessDate == null) {
            return;
        }
        assertPeriodWritable(PERIOD_FORMAT.format(businessDate), documentNo);
    }

    /**
     * 优先使用业务日期；日期为空时回退解析 {@code YYYY-MM} 账期字符串。
     */
    public void assertWritable(String periodCode, LocalDate fallbackDate, String documentNo) {
        if (fallbackDate != null) {
            assertWritable(fallbackDate, documentNo);
            return;
        }
        if (!StringUtils.hasText(periodCode)) {
            return;
        }
        assertPeriodWritable(normalizePeriodCode(periodCode), documentNo);
    }

    public void assertPeriodWritable(String periodCode, String documentNo) {
        if (!StringUtils.hasText(periodCode)) {
            return;
        }
        String normalized = normalizePeriodCode(periodCode);
        AccountingPeriod period = accountingPeriodMapper.selectOne(Wrappers.<AccountingPeriod>lambdaQuery()
                .eq(AccountingPeriod::getTenantId, TenantContextHolder.getRequiredTenantId().toString())
                .eq(AccountingPeriod::getPeriodCode, normalized)
                .eq(AccountingPeriod::getDeleted, false)
                .last("LIMIT 1"));
        if (period != null && period.getStatus() != null
                && AccountingPeriodStatus.fromCode(period.getStatus()) == AccountingPeriodStatus.CLOSED) {
            throw new ServiceException(ResultCode.CONFLICT.getCode(), HttpStatus.CONFLICT,
                    "账期 " + normalized + " 已关账，禁止写入单据: " + documentNo);
        }
    }

    /**
     * 容忍 {@code YYYY-MM} 与 {@code YYYY-MM-DD} 两种入参，统一取到月。
     */
    private String normalizePeriodCode(String periodCode) {
        String trimmed = periodCode.trim();
        if (trimmed.length() >= 7) {
            return trimmed.substring(0, 7);
        }
        try {
            return PERIOD_FORMAT.format(LocalDate.parse(trimmed + "-01"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("账期格式必须为 YYYY-MM: " + periodCode);
        }
    }
}
