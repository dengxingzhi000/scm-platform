package com.cognora.platform.common.xxl;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 共享 XXL-Job 执行器引导。
 *
 * <p>只有配置了 {@code xxl.job.admin.addresses} 的服务才会创建执行器；没有
 * 调度需求的服务（或本地未启动 XXL-Job 时）不会因为缺少配置而启动失败。
 * 各服务只保留 {@code xxl.job.*} 配置，不再各自复制一份 {@code XxlJobConfig}。</p>
 *
 * <p>XXL-Job 3.5.0 的 admin 地址是根路径（如 {@code http://host:8080/}），
 * 不再带旧版的 {@code /xxl-job-admin} 前缀。</p>
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "xxl.job", name = "admin.addresses")
public class XxlJobAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "xxlJobExecutor")
    public XxlJobSpringExecutor xxlJobExecutor(
            @Value("${xxl.job.admin.addresses}") String adminAddresses,
            @Value("${xxl.job.accessToken:}") String accessToken,
            @Value("${xxl.job.executor.appname}") String appname,
            @Value("${xxl.job.executor.address:}") String address,
            @Value("${xxl.job.executor.ip:}") String ip,
            @Value("${xxl.job.executor.port}") int port,
            @Value("${xxl.job.executor.logpath}") String logPath,
            @Value("${xxl.job.executor.logretentiondays}") int logRetentionDays) {

        log.info("[XXL-Job] initializing executor: appname={}, adminAddresses={}", appname, adminAddresses);

        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appname);
        executor.setAddress(address);
        executor.setIp(ip);
        executor.setPort(port);
        executor.setAccessToken(accessToken);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        return executor;
    }
}
