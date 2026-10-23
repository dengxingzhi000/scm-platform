package com.scmcloud.analytics.job;

import com.scmcloud.analytics.materialization.MaterializationService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Aggregates DWD data into the daily DWS summary layer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DwsJob {

    private final MaterializationService materializationService;

    @XxlJob("analyticsDwsJob")
    public void execute() {
        LocalDate date = MaterializationService.resolveDate(XxlJobHelper.getJobParam());
        try {
            int tenants = materializationService.materializeDws(
                    date, XxlJobHelper.getShardIndex(), XxlJobHelper.getShardTotal());
            String message = "DWS materialized for " + date + ", tenants=" + tenants;
            XxlJobHelper.log(message);
            XxlJobHelper.handleSuccess(message);
        } catch (RuntimeException ex) {
            log.error("DWS materialization failed for {}", date, ex);
            XxlJobHelper.handleFail("DWS materialization failed: " + ex.getMessage());
        }
    }
}
