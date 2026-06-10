package com.scmcloud.common.tenant;

import com.scmcloud.common.tenant.quota.QuotaService;
import com.scmcloud.common.tenant.quota.QuotaType;
import com.scmcloud.common.tenant.quota.RequireQuotaCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Quota check AOP interceptor

 * Usage:
 * <pre>
 * @RequireQuotaCheck(quotaType = QuotaType.ORDERS, increment = 1)
 * public Order createOrder(OrderCreateDTO dto) {
 *     // ...
 * }
 * </pre>
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class QuotaChecker {
    private final QuotaService quotaService;

    /**
     * Check quota before method execution
     */
    @Before("@annotation(com.scmcloud.common.tenant.quota.RequireQuotaCheck)")
    public void checkQuota(JoinPoint joinPoint) {
        // Get tenant ID
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.warn("Tenant ID is null, skipping quota check");
            return;
        }

        // Get annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireQuotaCheck annotation = method.getAnnotation(RequireQuotaCheck.class);

        if (annotation == null) {
            return;
        }

        QuotaType quotaType = annotation.quotaType();
        int increment = annotation.increment();

        log.debug("Checking quota for tenant={}, type={}, increment={}",
                tenantId, quotaType, increment);

        // Check quota
        boolean hasQuota = quotaService.checkAndConsumeQuota(tenantId, quotaType, increment);

        if (!hasQuota) {
            log.warn("Quota exceeded for tenant={}, type={}", tenantId, quotaType);
            throw new QuotaExceededException(
                    String.format("Quota exceeded for %s. Please upgrade or contact support.", quotaType.getDescription())
            );
        }

        log.debug("Quota check passed for tenant={}, type={}", tenantId, quotaType);
    }

    /**
     * Quota exceeded exception
     */
    public static class QuotaExceededException extends RuntimeException {
        public QuotaExceededException(String message) {
            super(message);
        }
    }
}