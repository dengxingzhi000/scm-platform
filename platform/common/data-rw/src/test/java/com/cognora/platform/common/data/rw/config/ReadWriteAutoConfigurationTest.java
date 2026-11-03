package com.cognora.platform.common.data.rw.config;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.cognora.platform.common.data.rw.aop.ReadWriteRoutingAspect;
import com.cognora.platform.common.data.rw.circuitbreaker.SlaveCircuitBreaker;
import com.cognora.platform.common.data.rw.loadbalance.SlaveWarmupManager;
import com.cognora.platform.common.data.rw.routing.ReadWriteRoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("读写分离自动装配测试")
class ReadWriteAutoConfigurationTest {

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        ReadWriteAutoConfiguration.class,
                        DataSourceAutoConfiguration.class))
                .withPropertyValues(
                        "spring.datasource.dynamic.enabled=false",
                        "spring.datasource.rw.enabled=true",
                        "spring.datasource.rw.health-check-enabled=false");
    }

    private ApplicationContextRunner runnerWithMaster() {
        return runner().withPropertyValues(
                "spring.datasource.rw.groups.product.master.url="
                        + "jdbc:postgresql://127.0.0.1:1/db_product",
                "spring.datasource.rw.groups.product.master.username=admin",
                "spring.datasource.rw.groups.product.master.password=secret");
    }

    @Test
    @DisplayName("enabled=true 且只有主库时应正常启动并回退主库")
    void shouldStartWithMasterOnly() {
        runnerWithMaster().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DataSource.class);
            assertThat(context.getBean(DataSource.class)).isInstanceOf(ReadWriteRoutingDataSource.class);

            ReadWriteRoutingDataSource routing = (ReadWriteRoutingDataSource) context.getBean(DataSource.class);
            assertThat(routing.getSlaveAvailability()).isEmpty();
            assertThat(routing.getRouteCounts().get("slave")).isZero();

            assertThat(context).hasSingleBean(SlaveCircuitBreaker.class);
            assertThat(context).hasSingleBean(SlaveWarmupManager.class);
            assertThat(context).hasBean("readWriteTracingAdvisor");
        });
    }

    @Test
    @DisplayName("从库不可达时启动不能失败（连接池后台初始化）")
    void shouldStartWhenSlaveIsUnreachable() {
        runnerWithMaster()
                .withPropertyValues(
                        "spring.datasource.rw.groups.product.slaves[0].name=slave1",
                        "spring.datasource.rw.groups.product.slaves[0].url="
                                + "jdbc:postgresql://127.0.0.1:1/db_product",
                        "spring.datasource.rw.groups.product.slaves[0].username=admin",
                        "spring.datasource.rw.groups.product.slaves[0].password=secret")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ReadWriteRoutingDataSource routing =
                            (ReadWriteRoutingDataSource) context.getBean(DataSource.class);
                    assertThat(routing.getSlaveAvailability()).containsEntry("slave1", true);
                });
    }

    @Test
    @DisplayName("enabled=true 但没有配置 group 时应保持原有单库，不影响启动")
    void shouldKeepSingleDatasourceWhenNoGroupConfigured() {
        runner()
                .withPropertyValues(
                        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/db_product",
                        "spring.datasource.username=admin",
                        "spring.datasource.password=secret")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ReadWriteRoutingDataSource.class);
                    assertThat(context).doesNotHaveBean(ReadWriteRoutingAspect.class);
                    assertThat(context.getBean(DataSource.class)).isInstanceOf(HikariDataSource.class);
                });
    }

    @Test
    @DisplayName("enabled=false 时行为不变：仍是普通单库 DataSource")
    void shouldKeepDefaultBehaviourWhenDisabled() {
        runnerWithMaster()
                .withPropertyValues(
                        "spring.datasource.rw.enabled=false",
                        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/db_product",
                        "spring.datasource.username=admin",
                        "spring.datasource.password=secret")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ReadWriteRoutingDataSource.class);
                    assertThat(context).doesNotHaveBean(ReadWriteRoutingAspect.class);
                    assertThat(context.getBean(DataSource.class)).isInstanceOf(HikariDataSource.class);
                });
    }

    @Test
    @DisplayName("主库配置非法时启动期给出清晰错误")
    void shouldFailFastWithClearMessageWhenMasterConfigIsInvalid() {
        runnerWithMaster()
                .withPropertyValues("spring.datasource.rw.groups.product.master.username=")
                .run(context -> {
                    assertThat(context).hasFailed();

                    StringBuilder messages = new StringBuilder();
                    Throwable current = context.getStartupFailure();
                    while (current != null) {
                        messages.append(current.getMessage()).append(' ');
                        current = current.getCause();
                    }
                    assertThat(messages.toString())
                            .containsIgnoringCase("username")
                            .containsIgnoringCase("master");
                });
    }

    @Test
    @DisplayName("rw 开启且存在 @DS 时必须启动失败并给出明确错误")
    void shouldFailWhenDsAnnotationIsPresent() {
        runnerWithMaster()
                .withUserConfiguration(DsAnnotatedConfiguration.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(failureMessages(context.getStartupFailure()))
                            .contains("@DS")
                            .contains("DsAnnotatedRepository");
                });
    }

    @Test
    @DisplayName("rw 开启且配置了额外 dynamic datasource 时必须启动失败")
    void shouldFailWhenExtraDynamicDataSourceIsConfigured() {
        runnerWithMaster()
                .withPropertyValues(
                        "spring.datasource.dynamic.enabled=true",
                        "spring.datasource.dynamic.primary=product",
                        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/db_product",
                        "spring.datasource.username=admin",
                        "spring.datasource.password=secret",
                        "spring.datasource.dynamic.datasource.org.url="
                                + "jdbc:postgresql://127.0.0.1:1/db_org",
                        "spring.datasource.dynamic.datasource.org.username=admin",
                        "spring.datasource.dynamic.datasource.org.password=secret")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(failureMessages(context.getStartupFailure()))
                            .contains("org")
                            .contains("not covered by rw groups");
                });
    }

    private String failureMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        Throwable current = failure;
        while (current != null) {
            messages.append(current.getMessage()).append(' ');
            current = current.getCause();
        }
        return messages.toString();
    }

    @Configuration
    static class DsAnnotatedConfiguration {
        @Bean
        DsAnnotatedRepository dsAnnotatedRepository() {
            return new DsAnnotatedRepository();
        }
    }

    @DS("org")
    static class DsAnnotatedRepository {
        public String findById(String id) {
            return id;
        }
    }
}
