package com.scmcloud.analytics.job;

import com.scmcloud.analytics.materialization.MaterializationService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Publishes the ADS serving tables consumed by dashboards.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdsJob {

    private final MaterializationService materializationService;

    @XxlJob("analyticsAdsJob")
    public void execute() {
        LocalDate date = MaterializationService.resolveDate(XxlJobHelper.getJobParam());
        try {
            int tenants = materializationService.materializeAds(
                    date, XxlJobHelper.getShardIndex(), XxlJobHelper.getShardTotal());
            String message = "ADS materialized for " + date + ", tenants=" + tenants;
            XxlJobHelper.log(message);
            XxlJobHelper.handleSuccess(message);
        } catch (RuntimeException ex) {
            log.error("ADS materialization failed for {}", date, ex);
            XxlJobHelper.handleFail("ADS materialization failed: " + ex.getMessage());
        }
    }
}
