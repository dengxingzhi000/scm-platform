package com.scmcloud.analytics.job;

import com.scmcloud.analytics.materialization.MaterializationService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Builds the DWD sales detail layer from ODS order events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DwdJob {

    private final MaterializationService materializationService;

    @XxlJob("analyticsDwdJob")
    public void execute() {
        LocalDate date = MaterializationService.resolveDate(XxlJobHelper.getJobParam());
        try {
            int tenants = materializationService.materializeDwd(
                    date, XxlJobHelper.getShardIndex(), XxlJobHelper.getShardTotal());
            String message = "DWD materialized for " + date + ", tenants=" + tenants;
            XxlJobHelper.log(message);
            XxlJobHelper.handleSuccess(message);
        } catch (RuntimeException ex) {
            log.error("DWD materialization failed for {}", date, ex);
            XxlJobHelper.handleFail("DWD materialization failed: " + ex.getMessage());
        }
    }
}
