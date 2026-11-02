package com.scmcloud.tenant.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.tenant.domain.entity.Tenant;
import com.scmcloud.tenant.domain.entity.TenantPackage;
import com.scmcloud.tenant.domain.entity.TenantPlatformFee;
import com.scmcloud.tenant.domain.entity.TenantResourceQuota;
import com.scmcloud.tenant.domain.entity.TenantSubscription;
import com.scmcloud.tenant.mapper.TenantPlatformFeeMapper;
import com.scmcloud.tenant.service.IPlatformFeeService;
import com.scmcloud.tenant.service.ITenantPackageService;
import com.scmcloud.tenant.service.ITenantResourceQuotaService;
import com.scmcloud.tenant.service.ITenantService;
import com.scmcloud.tenant.service.ITenantSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 平台服务费计算。
 *
 * <p>按订阅套餐月费 + 资源超量费生成每个租户的月度账单：
 * <ul>
 *   <li>基础费：套餐月价</li>
 *   <li>存储费：实际存储超出套餐容量部分 × 单价</li>
 *   <li>API 费：当日调用量超出套餐上限部分 × 单价</li>
 *   <li>优惠：按套餐折扣率抵扣基础费</li>
 * </ul>
 * 账单写入 {@code tenant_platform_fee}，同一租户同一账期幂等更新。
 *
 * @author SCM Platform Team
 * @since 2026-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformFeeServiceImpl implements IPlatformFeeService {

    private static final String BILLING_PERIOD_FORMAT = "yyyy-MM";

    private final TenantPlatformFeeMapper feeMapper;
    private final ITenantService tenantService;
    private final ITenantSubscriptionService subscriptionService;
    private final ITenantPackageService packageService;
    private final ITenantResourceQuotaService quotaService;

    /**
     * 存储超量单价（元/GB）
     */
    @Value("${scm.platform-fee.storage-unit-price:0.50}")
    private BigDecimal storageUnitPrice;

    /**
     * API 超量单价（元/次）
     */
    @Value("${scm.platform-fee.api-unit-price:0.01}")
    private BigDecimal apiUnitPrice;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int calculateMonthlyFees(YearMonth targetMonth, UUID tenantId) {
        log.info("开始计算平台服务费: month={}, tenantId={}", targetMonth, tenantId);

        List<Tenant> tenants = resolveTenants(tenantId);
        int calculated = 0;
        for (Tenant tenant : tenants) {
            if (tenant == null || Boolean.TRUE.equals(tenant.getDeleted())) {
                continue;
            }
            if (calculateForTenant(tenant, targetMonth)) {
                calculated++;
            }
        }
        log.info("平台服务费计算完成: month={}, 处理租户数={}", targetMonth, calculated);
        return calculated;
    }

    private List<Tenant> resolveTenants(UUID tenantId) {
        if (tenantId != null) {
            Tenant tenant = tenantService.getById(tenantId.toString());
            return tenant == null ? List.of() : List.of(tenant);
        }
        return tenantService.lambdaQuery()
                .eq(Tenant::getDeleted, false)
                .list();
    }

    private boolean calculateForTenant(Tenant tenant, YearMonth targetMonth) {
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();

        TenantSubscription subscription = findActiveSubscription(tenant.getId(), monthStart, monthEnd);
        if (subscription == null) {
            log.debug("租户在当前账期没有生效订阅，跳过: tenantId={}, month={}", tenant.getId(), targetMonth);
            return false;
        }

        TenantPackage pkg = subscription.getPackageId() == null
                ? null
                : packageService.getById(subscription.getPackageId());
        TenantResourceQuota quota = quotaService.lambdaQuery()
                .eq(TenantResourceQuota::getTenantId, tenant.getId())
                .one();

        BigDecimal baseFee = resolveBaseFee(subscription, pkg);
        BigDecimal discount = resolveDiscount(baseFee, pkg);
        BigDecimal storageFee = calculateStorageFee(quota, pkg);
        BigDecimal apiFee = calculateApiFee(quota, pkg);
        BigDecimal totalFee = baseFee.add(storageFee).add(apiFee);
        BigDecimal finalFee = totalFee.subtract(discount).max(BigDecimal.ZERO);

        String billingPeriod = targetMonth.format(DateTimeFormatter.ofPattern(BILLING_PERIOD_FORMAT));
        TenantPlatformFee existing = feeMapper.selectOne(Wrappers.<TenantPlatformFee>lambdaQuery()
                .eq(TenantPlatformFee::getTenantId, tenant.getId())
                .eq(TenantPlatformFee::getBillingPeriod, billingPeriod));

        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            TenantPlatformFee fee = new TenantPlatformFee();
            fee.setId(UUID.randomUUID().toString());
            fee.setTenantId(tenant.getId());
            fee.setBillingPeriod(billingPeriod);
            fee.setStartDate(monthStart);
            fee.setEndDate(monthEnd);
            fee.setPackageId(subscription.getPackageId());
            fee.setPackageName(pkg != null ? pkg.getPackageName() : null);
            fee.setBaseFee(scale(baseFee));
            fee.setStorageFee(scale(storageFee));
            fee.setApiFee(scale(apiFee));
            fee.setTotalFee(scale(totalFee));
            fee.setDiscountAmount(scale(discount));
            fee.setFinalFee(scale(finalFee));
            fee.setStatus(0);
            fee.setCreateTime(now);
            fee.setUpdateTime(now);
            feeMapper.insert(fee);
            log.info("平台服务费账单生成: tenantId={}, period={}, finalFee={}", tenant.getId(), billingPeriod, finalFee);
            return true;
        }

        if (existing.getStatus() != null && existing.getStatus() != 0) {
            log.debug("账单已支付或作废，跳过重算: tenantId={}, period={}", tenant.getId(), billingPeriod);
            return false;
        }

        existing.setPackageId(subscription.getPackageId());
        existing.setPackageName(pkg != null ? pkg.getPackageName() : null);
        existing.setBaseFee(scale(baseFee));
        existing.setStorageFee(scale(storageFee));
        existing.setApiFee(scale(apiFee));
        existing.setTotalFee(scale(totalFee));
        existing.setDiscountAmount(scale(discount));
        existing.setFinalFee(scale(finalFee));
        existing.setUpdateTime(now);
        feeMapper.updateById(existing);
        log.info("平台服务费账单更新: tenantId={}, period={}, finalFee={}", tenant.getId(), billingPeriod, finalFee);
        return true;
    }

    private TenantSubscription findActiveSubscription(String tenantId, LocalDate monthStart, LocalDate monthEnd) {
        List<TenantSubscription> subscriptions = subscriptionService.lambdaQuery()
                .eq(TenantSubscription::getTenantId, tenantId)
                .eq(TenantSubscription::getStatus, 1)
                .le(TenantSubscription::getStartDate, monthEnd)
                .ge(TenantSubscription::getEndDate, monthStart)
                .orderByDesc(TenantSubscription::getStartDate)
                .list();
        return subscriptions.isEmpty() ? null : subscriptions.get(0);
    }

    private BigDecimal resolveBaseFee(TenantSubscription subscription, TenantPackage pkg) {
        if (pkg != null && pkg.getPriceMonthly() != null) {
            return pkg.getPriceMonthly();
        }
        return nullSafe(subscription.getActualPrice());
    }

    private BigDecimal resolveDiscount(BigDecimal baseFee, TenantPackage pkg) {
        if (pkg == null || pkg.getDiscountRate() == null || pkg.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return baseFee.multiply(pkg.getDiscountRate());
    }

    private BigDecimal calculateStorageFee(TenantResourceQuota quota, TenantPackage pkg) {
        if (quota == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal used = nullSafe(quota.getCurrentStorageGb());
        int capacity = resolveLimit(quota.getMaxStorageGb(), pkg != null ? pkg.getMaxStorageGb() : null);
        if (capacity <= 0 || used.compareTo(BigDecimal.valueOf(capacity)) <= 0) {
            return BigDecimal.ZERO;
        }
        return used.subtract(BigDecimal.valueOf(capacity)).multiply(storageUnitPrice);
    }

    private BigDecimal calculateApiFee(TenantResourceQuota quota, TenantPackage pkg) {
        if (quota == null) {
            return BigDecimal.ZERO;
        }
        int calls = quota.getCurrentApiCallsToday() == null ? 0 : quota.getCurrentApiCallsToday();
        int capacity = resolveLimit(quota.getMaxApiCallsPerDay(), pkg != null ? pkg.getMaxApiCallsPerDay() : null);
        if (capacity <= 0 || calls <= capacity) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(calls - capacity).multiply(apiUnitPrice);
    }

    private int resolveLimit(Integer quotaLimit, Integer packageLimit) {
        if (quotaLimit != null && quotaLimit > 0) {
            return quotaLimit;
        }
        return packageLimit == null ? 0 : packageLimit;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
