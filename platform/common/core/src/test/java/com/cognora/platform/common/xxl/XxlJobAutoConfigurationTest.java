package com.cognora.platform.common.xxl;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 共享 XXL-Job 执行器的条件装配：没有 admin 地址时不创建 Bean。
 */
class XxlJobAutoConfigurationTest {

    @Test
    void doesNotCreateExecutorWhenAdminAddressIsMissing() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setEnvironment(environment(Map.of()));
            context.register(XxlJobAutoConfiguration.class);
            context.refresh();

            assertThat(context.getBeanNamesForType(XxlJobSpringExecutor.class)).isEmpty();
        }
    }

    @Test
    void createsExecutorWhenAdminAddressIsConfigured() {
        Map<String, Object> properties = Map.of(
                "xxl.job.admin.addresses", "http://localhost:8088/",
                "xxl.job.accessToken", "test-token",
                "xxl.job.executor.appname", "scm-audit",
                "xxl.job.executor.port", "9988",
                "xxl.job.executor.logpath", "target/xxl-job",
                "xxl.job.executor.logretentiondays", "30");

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setEnvironment(environment(properties));
            context.register(XxlJobAutoConfiguration.class);
            context.refresh();

            XxlJobSpringExecutor executor = context.getBean(XxlJobSpringExecutor.class);
            assertThat(executor).isNotNull();
        }
    }

    private static StandardEnvironment environment(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        return environment;
    }
}
