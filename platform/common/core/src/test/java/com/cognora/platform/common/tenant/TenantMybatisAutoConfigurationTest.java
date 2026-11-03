package com.cognora.platform.common.tenant;

import org.apache.ibatis.plugin.Interceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the tenant auto-configuration in a real Spring context. The property binding here is
 * constructor binding on a record with two constructors — a wiring mistake used to only show up
 * when a service started, because no other test in this module touches the container.
 */
class TenantMybatisAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AsyncTenantAutoConfiguration.class, TenantMybatisAutoConfiguration.class));

    @Test
    void defaults_keepThePreviousRuntimeBehaviour() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(TenantProperties.class);
            TenantProperties properties = context.getBean(TenantProperties.class);
            assertThat(properties.failOnParseError()).isTrue();
            assertThat(properties.required()).isTrue();
            assertThat(properties.sqlIsolation()).isTrue();
            assertThat(properties.excludePaths()).isEqualTo(TenantProperties.DEFAULT_EXCLUDE_PATHS);
            assertThat(properties.excludeTables()).isEmpty();
            assertThat(context).hasSingleBean(Interceptor.class);
            assertThat(context.getBean(Interceptor.class)).isInstanceOf(TenantInterceptor.class);
        });
    }

    @Test
    void scmTenantKeys_areBoundThroughTheRecordConstructor() {
        runner.withPropertyValues(
                "cognora.tenant.required=false",
                "cognora.tenant.fail-on-parse-error=false",
                "cognora.tenant.exclude-paths[0]=/open/**",
                "cognora.tenant.exclude-tables[0]=cfg_shared_view",
                "cognora.tenant.exclude-tables[1]=report_*")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    TenantProperties properties = context.getBean(TenantProperties.class);
                    assertThat(properties.required()).isFalse();
                    assertThat(properties.failOnParseError()).isFalse();
                    assertThat(properties.excludePaths()).containsExactly("/open/**");
                    assertThat(properties.excludeTables())
                            .containsExactly("cfg_shared_view", "report_*");
                });
    }

    @Test
    void sqlIsolationCanBeSwitchedOffWithoutDroppingTheFilter() {
        runner.withPropertyValues("cognora.tenant.sql-isolation=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(Interceptor.class);
            assertThat(context).hasSingleBean(TenantProperties.class);
        });
    }
}
