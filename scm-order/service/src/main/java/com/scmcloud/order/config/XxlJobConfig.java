package com.scmcloud.order.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.UUID;

/**
 * XXL-Job 閰嶇疆
 *
 * <p>閰嶇疆 XXL-Job 鎵ц鍣紝鑷姩娉ㄥ唽锟絏XL-Job Admin
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.accessToken:}")
    private String accessToken;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.address:}")
    private String address;

    @Value("${xxl.job.executor.ip:}")
    private String ip;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;

    /**
     * XXL-Job 鎵ц锟?
     */
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info("馃殌 [XXL-Job] 鍒濆鍖栨墽琛屽櫒: appname={}, adminAddresses={}",
                appname, adminAddresses);

        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setAddress(address);
        xxlJobSpringExecutor.setIp(ip);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);

        log.info("[XXL-Job] Executor initialization completed");
        return xxlJobSpringExecutor;
    }
}