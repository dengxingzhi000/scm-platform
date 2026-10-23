package com.scmcloud.analytics.job;

import com.scmcloud.analytics.domain.entity.DataQualityResult;
import com.scmcloud.analytics.domain.entity.DataQualityRule;
import com.scmcloud.analytics.mapper.DataQualityRuleMapper;
import com.scmcloud.analytics.service.DataQualityService;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs all enabled quality rules, iterating tenant by tenant so every rule is
 * evaluated inside its own tenant context.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataQualityJob {

    private final DataQualityRuleMapper ruleMapper;
    private final DataQualityService dataQualityService;

    @XxlJob("analyticsDataQualityJob")
    public void execute() {
        List<String> tenants = ruleMapper.selectDistinctTenantIds();
        AtomicInteger passed = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        for (String tenant : tenants) {
            UUID tenantId;
            try {
                tenantId = UUID.fromString(tenant);
            } catch (IllegalArgumentException ex) {
                log.warn("Skipping non-UUID analytics tenant: {}", tenant);
                continue;
            }
            TenantContextHolder.executeInTenantContext(tenantId, () -> {
                for (DataQualityRule rule : ruleMapper.selectEnabledByTenant(tenant)) {
                    try {
                        DataQualityResult result = dataQualityService.execute(rule);
                        if (Boolean.TRUE.equals(result.getPassed())) {
                            passed.incrementAndGet();
                        } else {
                            failed.incrementAndGet();
                        }
                    } catch (RuntimeException ex) {
                        log.warn("Quality rule failed: tenant={}, rule={}, error={}",
                                tenant, rule.getRuleCode(), ex.getMessage());
                        dataQualityService.recordFailure(rule, ex.getMessage());
                        failed.incrementAndGet();
                    }
                }
                return null;
            });
        }

        String message = "Quality job finished: tenants=" + tenants.size()
                + ", passed=" + passed.get() + ", failed=" + failed.get();
        XxlJobHelper.log(message);
        XxlJobHelper.handleSuccess(message);
    }
}
