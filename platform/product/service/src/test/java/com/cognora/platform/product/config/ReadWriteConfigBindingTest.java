package com.cognora.platform.product.config;

import com.cognora.platform.common.data.rw.config.ReadWriteProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("scm-product 读写分离配置绑定测试")
class ReadWriteConfigBindingTest {

    @Test
    @DisplayName("application.yml 中的 spring.datasource.rw 必须能完整绑定")
    void shouldBindReadWritePropertiesFromApplicationYml() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load(
                "application", new ClassPathResource("application.yml"));

        StandardEnvironment environment = new StandardEnvironment();
        sources.forEach(source -> environment.getPropertySources().addFirst(source));

        assertEquals("false", environment.getProperty("spring.datasource.dynamic.enabled"),
                "rw 启用时必须关闭 baomidou 动态数据源，否则 primary 配置会冲突");

        ReadWriteProperties properties = Binder.get(environment)
                .bind("spring.datasource.rw", ReadWriteProperties.class)
                .orElseThrow(() -> new IllegalStateException(
                        "spring.datasource.rw is not configured in application.yml"));

        assertTrue(properties.isEnabled(), "读取分离必须在 product 服务上启用");
        assertEquals(ReadWriteProperties.RoutingTarget.MASTER, properties.getDefaultTarget(),
                "默认目标必须是主库，避免丢失上下文的线程读到从库");
        assertTrue(properties.isFailFastOnSlaveConnectionFailure());
        assertFalse(properties.isSlaveDownAffectsHealth());
        assertTrue(properties.getCircuitBreaker().isEnabled());
        assertEquals(500, properties.getCircuitBreaker().getSlowRtMs());
        assertEquals(30, properties.getCircuitBreaker().getTimeWindowSeconds());
        assertNotNull(properties.getReadMasterAfterWrite());
        assertEquals(2000, properties.getReadMasterAfterWrite().toMillis());

        ReadWriteProperties.DataSourceGroup group = properties.getGroups().get("product");
        assertNotNull(group, "group 名必须与配置一致");
        assertNotNull(group.getMaster());
        assertNotNull(group.getMaster().getUrl());
        assertNotNull(group.getMaster().getUsername());

        assertEquals(1, group.getSlaves().size());
        ReadWriteProperties.SlaveDataSourceConfig slave = group.getSlaves().getFirst();
        assertEquals("slave1", slave.getName());
        assertEquals(5000, slave.getConnectionTimeout().toMillis(),
                "从库连接探测必须快速失败");
        assertEquals(-1, slave.getInitializationFailTimeout(),
                "从库不可达时不能阻止应用启动");
    }
}