package com.cognora.lms.logistics.job;

import com.cognora.lms.logistics.service.ITmsSlaAlertService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * SLA 超时预警定时任务（批次 3）。
 *
 * <p>复用平台公共 XXL-Job 执行器（platform-common-core 自动装配），
 * 在调度中心注册 {@code tmsSlaOverdueScanHandler} 即可按分钟/小时级别扫描。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TmsSlaJob {

    public static final String HANDLER_NAME = "tmsSlaOverdueScanHandler";

    private final ITmsSlaAlertService slaAlertService;

    @XxlJob(HANDLER_NAME)
    public void scanOverdue() {
        int created = slaAlertService.scanOverdue(LocalDateTime.now());
        String message = "SLA 超时扫描完成，新增预警 " + created + " 条";
        log.info(message);
        XxlJobHelper.handleSuccess(message);
    }
}
